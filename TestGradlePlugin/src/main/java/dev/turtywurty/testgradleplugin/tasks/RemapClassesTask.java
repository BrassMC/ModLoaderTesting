package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import dev.turtywurty.testgradleplugin.mappings.*;
import dev.turtywurty.testgradleplugin.util.FileUtil;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RemapClassesTask extends DefaultTestGradleTask {
    @InputFiles
    private final Path clientMappingsPath;
    @InputDirectory
    private final Path clientDir;

    private final Path remappedClientDir;

    public RemapClassesTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.clientMappingsPath = versionPath.resolve("client_mappings.txt");
        this.clientDir = versionPath.resolve("client");
        this.remappedClientDir = versionPath.resolve("remapped_client");
    }

    private static void remap(Path inputDir, Path outputDir, Path mappingsFile) {
        if (Files.notExists(inputDir) || !Files.isDirectory(inputDir))
            throw new RuntimeException("Input directory does not exist or is not a directory: " + inputDir);

        if (Files.notExists(outputDir)) {
            try {
                Files.createDirectories(outputDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create output directory: " + outputDir, e);
            }
        }

        if (!Files.isDirectory(outputDir))
            throw new RuntimeException("Output directory is not a directory: " + outputDir);

        try {
            List<ClassMapping> mappings = OfficialMappingsFile.parse(mappingsFile);
            if (mappings.isEmpty()) {
                throw new RuntimeException("No mappings found in the mappings file: " + mappingsFile);
            }

            Map<String, String> originalToObfuscated = mappings.stream()
                    .collect(Collectors.toMap(
                            classMapping -> classMapping.getOriginalName().replace(".", "/"),
                            ClassMapping::getObfuscatedName));
            var nameRemapper = new SimpleRemapper(originalToObfuscated);
            for (ClassMapping classMapping : mappings) {
                for (MethodMapping methodMapping : classMapping.getMethods()) {
                    String asmDesc = toAsmMethodDescriptor(
                            methodMapping.getDescriptor(), methodMapping.getReturnType());

                    String obfuscatedDesc = nameRemapper.mapMethodDesc(asmDesc);
                    methodMapping.setObfuscatedDescriptor(obfuscatedDesc);
                }

                for (FieldMapping fieldMapping : classMapping.getFields()) {
                    String obfuscatedDesc = nameRemapper.map(fieldMapping.getDescriptor());
                    fieldMapping.setObfuscatedDescriptor(obfuscatedDesc);
                }
            }

            try (var pool = new ForkJoinPool()) {
                pool.submit(new DirectoryRemapTask(inputDir, outputDir, mappings)).join();
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to parse mappings file: " + mappingsFile, exception);
        }
    }

    private static String toAsmMethodDescriptor(String humanParams, String humanReturn) {
        String params = humanParams.substring(1, humanParams.length() - 1).trim();
        StringBuilder sb = new StringBuilder("(");
        if (!params.isEmpty()) {
            for (String p : params.split(",")) {
                sb.append(toAsmTypeDescriptor(p.trim()));
            }
        }
        sb.append(")");
        sb.append(toAsmTypeDescriptor(humanReturn));
        return sb.toString();
    }

    private static String toAsmTypeDescriptor(String human) {
        switch (human) {
            case "byte":
                return "B";
            case "char":
                return "C";
            case "double":
                return "D";
            case "float":
                return "F";
            case "int":
                return "I";
            case "long":
                return "J";
            case "short":
                return "S";
            case "boolean":
                return "Z";
            case "void":
                return "V";
        }

        // handle arrays
        if (human.endsWith("[]")) {
            return "[" + toAsmTypeDescriptor(human.substring(0, human.length() - 2));
        }
        // object
        return "L" + human.replace('.', '/') + ";";
    }

    private static class DirectoryRemapTask extends RecursiveAction {
        private final Path inputDir;
        private final Path outputDir;
        private final List<ClassMapping> mappings;

        public DirectoryRemapTask(Path inputDir, Path outputDir, List<ClassMapping> mappings) {
            this.inputDir = inputDir;
            this.outputDir = outputDir;
            this.mappings = mappings;
        }

        @Override
        protected void compute() {
            try (Stream<Path> paths = Files.walk(inputDir)) {
                paths.forEach(filePath -> {
                    if (Files.isRegularFile(filePath) && filePath.toString().endsWith(".class")) {
                        try {
                            RemapperTool.remapClass(filePath, outputDir, mappings);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to remap class: " + filePath, e);
                        }
                    }
                });
            } catch (IOException exception) {
                throw new RuntimeException("Failed to walk input directory: " + inputDir, exception);
            }
        }
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

            if (Files.exists(remappedClientDir))
                FileUtil.deleteDirectory(remappedClientDir);

            remap(clientDir, remappedClientDir, clientMappingsPath);
        }

        // TODO: Figure out how to have optional directories then we can implement server remapping
    }

    public Path getClientMappingsPath() {
        return clientMappingsPath;
    }

    public Path getClientDir() {
        return clientDir;
    }
}
