package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.mappings.MappingFile;
import dev.turtywurty.testgradleplugin.mappings.OfficialMappingsFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class RemapClassesTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void remapClasses() {
        System.out.println("Remapping classes!");

        Path versionPath = getOutputDir()
                .getOrElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .getAsFile()
                .toPath()
                .resolve(getVersion().get());

        Path clientMappingsPath = versionPath.resolve("client_mappings.txt");
        if (Files.notExists(clientMappingsPath))
            throw new RuntimeException("client_mappings.txt is missing, please run the downloadClientMappings task!");

        Path serverMappingsPath = versionPath.resolve("server_mappings.txt");
        if (Files.notExists(serverMappingsPath))
            throw new RuntimeException("server_mappings.txt is missing, please run the downloadServerMappings task!");

        Path clientDir = versionPath.resolve("client");
        if (Files.notExists(clientDir))
            throw new RuntimeException("client is missing, please run the extractClient task!");

        Path serverDir = versionPath.resolve("server");
        if (Files.notExists(serverDir))
            throw new RuntimeException("server is missing, please run the extractServer task!");

        // parse mappings
        var serverMappings = new OfficialMappingsFile(serverMappingsPath);
        serverMappings.getCachedTree().print();

        var clientMappings = new OfficialMappingsFile(clientMappingsPath);
        clientMappings.getCachedTree().print();

        // remap classes
        remap(clientDir, clientMappings);
        remap(serverDir, serverMappings);
    }

    private static void remap(Path dir, OfficialMappingsFile mappings) {
        try (var stream = Files.list(dir)) {
            for (Path path : stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .toList()) {
                String className = path.getFileName().toString().replace(".class", "");
                if (className.equals("zl")) {
                    System.out.println("Found zl!");
                }

                String mappedName = mappings.findPath(className, MappingFile.NodeType.CLASS);
                if (mappedName == null) {
                    System.out.printf("Failed to find mapping for %s%n", className);
                    continue;
                }

                // move to correct location
                Path mappedPackagePath = dir.resolve(mappedName.replace(".", "/") + ".class");

                System.out.printf("Remapping %s to %s%n", className, mappedName);
                Files.createDirectories(mappedPackagePath.getParent());
                Files.move(path, mappedPackagePath);

                // TODO: Figure out
                // read the contents of the file
                try (var fileInputStream = new FileInputStream(mappedPackagePath.toFile())) {
                    var classReader = new ClassReader(fileInputStream);

                    // remap the class
                    var classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);

                    var remapper = new ClassReferenceRemapper(mappings);
                    var classRemapper = new ClassRemapper(classWriter, remapper);
                    classReader.accept(classRemapper, 0);

                    // write the new class
                    Files.write(mappedPackagePath, classWriter.toByteArray());

                    // System.out.printf("Remapped %s to %s%n", className, mappedName);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to walk directory!", exception);
        }
    }

    private static class ClassReferenceRemapper extends Remapper {
        private final MappingFile mappings;

        public ClassReferenceRemapper(MappingFile mappings) {
            this.mappings = mappings;
        }

        @Override
        public String map(String internalName) {
            String mappedName = this.mappings.findPath(internalName, MappingFile.NodeType.CLASS);
            if (mappedName == null) {
                System.out.printf("Failed to find mapping for %s%n", internalName);
                return internalName;
            }

            return mappedName;
        }
    }
}
