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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

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

//        Path serverMappingsPath = versionPath.resolve("server_mappings.txt");
//        if (Files.notExists(serverMappingsPath))
//            throw new RuntimeException("server_mappings.txt is missing, please run the downloadServerMappings task!");

        Path clientDir = versionPath.resolve("client");
        if (Files.notExists(clientDir))
            throw new RuntimeException("client is missing, please run the extractClient task!");
//
//        Path serverDir = versionPath.resolve("server");
//        if (Files.notExists(serverDir))
//            throw new RuntimeException("server is missing, please run the extractServer task!");

        // parse mappings
        //var serverMappings = new OfficialMappingsFile(serverMappingsPath);
        //serverMappings.getCachedTree().print();

        var clientMappings = new OfficialMappingsFile(clientMappingsPath);
        clientMappings.getCachedTree().print();

        // remap classes
        //remap(serverDir, serverMappings);
        remap(clientDir, clientMappings);
    }

    private static void remap(Path dir, OfficialMappingsFile mappings) {
        try (var forkJoinPool = new ForkJoinPool(); Stream<Path> paths = Files.list(dir)
                .filter(path -> path.toString().endsWith(".class"))) {
            for (Path file : paths.toArray(Path[]::new)) {
                forkJoinPool.submit(() -> remapClass(file, mappings));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to list files!", exception);
        }
    }

    private static void remapClass(Path path, MappingFile mappings) {
        String className = path.getFileName().toString().replace(".class", "");
        String mappedName = mappings.findPath(className, MappingFile.NodeType.CLASS);
        if (mappedName == null) {
            System.out.printf("Failed to find mapping for %s%n", className);
            return;
        }

        // move to correct location
        Path mappedPackagePath = path.getParent()
                .resolve(mappedName.replace(".", "/") + ".class");

        //System.out.printf("Remapping %s to %s%n", className, mappedName);
        try {
            Files.move(path, mappedPackagePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to move file!", exception);
        }

        // read the contents of the file
        try (var fileInputStream = new BufferedInputStream(Files.newInputStream(mappedPackagePath))) {
            var newBytes = writeReferences(mappings, fileInputStream);

            // write the new bytes to the file
            try (var fileOutputStream = new BufferedOutputStream(Files.newOutputStream(mappedPackagePath))) {
                fileOutputStream.write(newBytes);
            }

            System.out.printf("Remapped %s to %s%n", className, mappedName);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read file!", exception);
        }
    }

    private static byte[] writeReferences(MappingFile mappings, BufferedInputStream fileInputStream) throws IOException {
        var classReader = new ClassReader(fileInputStream);
        var classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        // create the remapper
        var remapper = new ClassReferenceRemapper(mappings);
        var classRemapper = new ClassRemapper(classWriter, remapper);

        // remap the class
        classReader.accept(classRemapper, ClassReader.EXPAND_FRAMES);

        return classWriter.toByteArray();
    }

    private static class ClassReferenceRemapper extends Remapper {
        private final MappingFile mappings;

        public ClassReferenceRemapper(MappingFile mappings) {
            this.mappings = mappings;
        }

        @Override
        public String map(String internalName) {
            String mappedName = this.mappings.findPath(internalName, MappingFile.NodeType.CLASS);
            return mappedName == null ? internalName : mappedName;
        }
    }
}
