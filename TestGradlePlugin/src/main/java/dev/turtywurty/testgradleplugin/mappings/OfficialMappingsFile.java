package dev.turtywurty.testgradleplugin.mappings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OfficialMappingsFile implements MappingFile {
    private final MappingTree mappingTree;

    public OfficialMappingsFile(Path path) {
        this.mappingTree = parseMappings(path);
    }

    @Override
    public String findPath(String name, MappingFile.NodeType nodeType) {
        MappingTree.MappingNode node = this.mappingTree.findNodeFull(mappingNode -> switch (nodeType) {
            case PACKAGE -> nodeType.getPredicate().test(mappingNode) && mappingNode.getName().equals(name);
            case CLASS, METHOD, FIELD -> nodeType.getPredicate().test(mappingNode) &&
                    ((MappingTree.ObfuscatedNode) mappingNode).getObfuscatedName().equals(name);
            default -> false;
        });

        if (node == null)
            return null;

        List<String> packages = new ArrayList<>();
        while (node != null) {
            packages.add(node.getName());
            node = node.getParent();
        }

        var builder = new StringBuilder();
        for (int i = packages.size() - 1; i >= 0; i--) {
            builder.append(packages.get(i));
            if (i != 0)
                builder.append(".");
        }

        return builder.toString();
    }

    @Override
    public MappingTree parseMappings(Path path) {
        if (Files.notExists(path)) {
            throw new IllegalArgumentException("File does not exist!");
        }

        var mappingTree = new MappingTree();

        try (var linesStream = Files.lines(path)) {
            List<String> lines = linesStream.toList();
            MappingTree.MappingNode currentParent = null;

            long startRead = System.nanoTime();
            final int totalLines = lines.size();
            for (String line : lines) {
                // Empty lines and comments
                if (line.isBlank() || line.startsWith("#"))
                    continue;

                if (line.endsWith(":")) {
                    line = line.replace(":", "").trim();
                    currentParent = parseClass(line, mappingTree);
                } else if (currentParent instanceof MappingTree.ClassNode classParent && line.contains("->")) {
                    currentParent.addChild(parseMethodOrField(classParent, line));
                }
            }

            System.out.printf("Parsed %d lines in %dms%n",
                    totalLines,
                    (System.nanoTime() - startRead) / 1_000_000);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read file!", exception);
        }

        return mappingTree;
    }

    private static MappingTree.MappingNode parseClass(String line, MappingTree mappingTree) {
        String[] parts = line.split(" -> ");
        String classPath = parts[0];
        String obfuscatedName = parts[1];

        String[] packageSplit = classPath.split("\\.");
        List<String> packages = new ArrayList<>(Arrays.asList(packageSplit).subList(0, packageSplit.length - 1));

        MappingTree.MappingNode previousPackage = null;
        for (String aPackage : packages) {
            MappingTree.MappingNode node = mappingTree.findNode(
                    previousPackage != null ? previousPackage.getChildren().values() : mappingTree.getRootNodes(),
                    mappingNode -> mappingNode.getName().equals(aPackage) && !(mappingNode instanceof MappingTree.ObfuscatedNode));

            if (node == null) {
                node = new MappingTree.MappingNode(aPackage, previousPackage);
                if (previousPackage == null) {
                    mappingTree.addRootNode(node);
                } else {
                    previousPackage.addChild(node);
                }
            }

            previousPackage = node;
        }

        String className = packageSplit[packageSplit.length - 1];
        var classNode = new MappingTree.ClassNode(className, obfuscatedName, previousPackage);
        if (previousPackage == null) {
            mappingTree.addRootNode(classNode);
        } else {
            previousPackage.addChild(classNode);
        }

        return classNode;
    }

    private static MappingTree.ObfuscatedNode parseMethodOrField(MappingTree.ClassNode parent, String line) {
        String[] split = line.split(" -> ");
        String signature = split[0].trim();
        String obfuscatedName = split[1].trim();

        // determine if method or field
        String[] parts = signature.split(" ");
        if (!signature.contains("(")) {
            // field
            String returnType = parts[0];
            String name = parts[1];

            return new MappingTree.FieldNode(name, obfuscatedName, returnType, parent);
        } else {
            // method
            String[] infoSplit = parts[0].split(":");

            String returnType = infoSplit[0];
            int fromLine = -1;
            int toLine = -1;
            if (infoSplit.length >= 3) {
                try {
                    fromLine = Integer.parseInt(infoSplit[0]);
                    toLine = Integer.parseInt(infoSplit[1]);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Failed to parse line numbers!", exception);
                }

                returnType = infoSplit[2];
            }

            int startIndex = parts[1].indexOf("(");
            int endIndex = parts[1].lastIndexOf(")");
            String name = parts[1].substring(0, startIndex);
            List<String> params = Arrays.stream(parts[1].substring(startIndex + 1, endIndex).split(",")) // split params
                    .map(String::trim) // trim params
                    .toList();

            return new MappingTree.MethodNode(name, obfuscatedName, fromLine, toLine, returnType, params, parent);
        }
    }

    @Override
    public MappingTree getCachedTree() {
        return this.mappingTree;
    }
}
