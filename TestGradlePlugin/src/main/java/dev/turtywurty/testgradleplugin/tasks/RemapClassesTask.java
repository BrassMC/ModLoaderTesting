package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import dev.turtywurty.testgradleplugin.mappings.OfficialMappingsFile;
import org.gradle.api.tasks.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

public class RemapClassesTask extends DefaultTestGradleTask {
    @InputFile
    @Classpath
    private final Path clientMappingsPath, serverMappingsPath;

    @InputDirectory
    @Classpath
    private final Path clientDir, serverDir;

    private final Path remappedClientDir, remappedServerDir;

    public RemapClassesTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.clientMappingsPath = versionPath.resolve("client_mappings.txt");
        this.serverMappingsPath = versionPath.resolve("server_mappings.txt");
        this.clientDir = versionPath.resolve("client");
        this.serverDir = versionPath.resolve("server");
        this.remappedClientDir = versionPath.resolve("remapped_client");
        this.remappedServerDir = versionPath.resolve("remapped_server");
    }

    private static void remap(Path dir, Path remappedDir, OfficialMappingsFile mappings) {
        try (var forkJoinPool = new ForkJoinPool(); Stream<Path> paths = Files.list(dir).parallel()
                .filter(path -> path.toString().endsWith(".class"))) {
            Path[] pathArray = paths.toArray(Path[]::new);
            Map<String, String> classMappings = new HashMap<>();

            long loadStart = System.currentTimeMillis();
            int loaded = 0;
            for (Path file : pathArray) {
                String className = file.getFileName().toString().replace(".class", "");
                String mappedName = mappings.getClassMappings().get(className);
                if (mappedName == null) {
                    System.out.printf("Failed to find mapping for %s%n", className);
                    continue;
                }

                classMappings.put(className, mappedName);
                loaded++;
            }

            System.out.printf("Loaded %d classes in %ds!%n", loaded, (System.currentTimeMillis() - loadStart) / 1000);

            var remapper = new ClassReferenceRemapper(classMappings);

            long remapStart = System.currentTimeMillis();
            int remapped = 0;
            for (Path path : pathArray) {
                // changing E:\gradle\cache\testGradle\client\a.class to E:\gradle\cache\testGradle\remapped_client\com\mojang\math\Axis.class
                Path newPath = remappedDir.resolve(dir.relativize(path));
                forkJoinPool.submit(() -> remapClass(path, newPath, classMappings, remapper)).get();
                remapped++;
            }

            System.out.printf("Remapped %d classes in %ds!%n", remapped, (System.currentTimeMillis() - remapStart) / 1000);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to list files!", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Failed to remap class!", exception.getCause());
        } catch (InterruptedException exception) {
            throw new IllegalStateException("Got interrupted!", exception);
        }
    }

    private static void remapClass(Path path, Path newPath, Map<String, String> classMappings, Remapper remapper) {
        String className = path.getFileName().toString().replace(".class", "");
        String mappedName = classMappings.get(className);
        Path mappedPackagePath = newPath.getParent()
                .resolve(mappedName.replace(".", "/") + ".class");

        classMappings.put(className, mappedName);

        System.out.printf("Remapping %s to %s%n", className, mappedName);
        try {
            Files.createDirectories(mappedPackagePath.getParent());
            Files.move(path, mappedPackagePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to move file!", exception);
        }

        try (var fileInputStream = new BufferedInputStream(Files.newInputStream(mappedPackagePath))) {
            var newBytes = writeReferences(fileInputStream, remapper);

            try (var fileOutputStream = new BufferedOutputStream(Files.newOutputStream(mappedPackagePath))) {
                fileOutputStream.write(newBytes);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read file!", exception);
        }
    }

    private static byte[] writeReferences(BufferedInputStream fileInputStream, Remapper remapper) throws IOException {
        var classReader = new ClassReader(fileInputStream);
        var classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        var classRemapper = new ClassRemapper(classWriter, remapper);
        classReader.accept(classRemapper, 0);

        return classWriter.toByteArray();
    }

    @TaskAction
    public void remapClasses() {
        System.out.println("Remapping classes!");

        TestGradleExtension.Side side = getSide();
        if (side == TestGradleExtension.Side.CLIENT || side == TestGradleExtension.Side.BOTH) {
            if (Files.notExists(clientMappingsPath))
                throw new RuntimeException("client_mappings.txt is missing, please run the downloadClientMappings task!");

            if (Files.notExists(clientDir))
                throw new RuntimeException("client is missing, please run the extractClient task!");

            var clientMappings = new OfficialMappingsFile(clientMappingsPath);
            remap(clientDir, remappedClientDir, clientMappings);
        }

        if (side == TestGradleExtension.Side.SERVER || side == TestGradleExtension.Side.BOTH) {
            if (Files.notExists(serverMappingsPath))
                throw new RuntimeException("server_mappings.txt is missing, please run the downloadServerMappings task!");

            if (Files.notExists(serverDir))
                throw new RuntimeException("server is missing, please run the extractServer task!");

            var serverMappings = new OfficialMappingsFile(serverMappingsPath);
            remap(serverDir, remappedServerDir, serverMappings);
        }
    }

    public Path getClientMappingsPath() {
        return clientMappingsPath;
    }

    public Path getServerMappingsPath() {
        return serverMappingsPath;
    }

    public Path getClientDir() {
        return clientDir;
    }

    public Path getServerDir() {
        return serverDir;
    }

    private static class ClassReferenceRemapper extends Remapper {
        private final Map<String, String> classMappings;

        public ClassReferenceRemapper(Map<String, String> classMappings) {
            this.classMappings = classMappings;
        }

        @Override
        public String map(String internalName) {
            String updatedName = classMappings.getOrDefault(internalName, internalName);
            return updatedName.equals(internalName) ? super.map(internalName) : updatedName.replace(".", "/");
        }
    }
}
