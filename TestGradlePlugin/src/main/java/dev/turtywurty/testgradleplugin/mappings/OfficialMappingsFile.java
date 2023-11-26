package dev.turtywurty.testgradleplugin.mappings;

import org.gradle.api.tasks.Internal;

import java.io.BufferedReader;
import java.io.FileReader;
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
    public String remapClass(String className) {
        MappingTree.ObfuscatedNode node = (MappingTree.ObfuscatedNode) this.mappingTree.findNode(
                className,
                mappingNode -> mappingNode instanceof MappingTree.ObfuscatedNode obfNode &&
                        obfNode.getObfuscatedName().equals(className));

        return node == null ? className : node.getName();
    }

    @Override
    public String findPackage(String name) {
        MappingTree.MappingNode node = this.mappingTree.findNode(
                name,
                mappingNode -> mappingNode instanceof MappingTree.ObfuscatedNode obfNode &&
                        obfNode.getName().equals(name));
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

    @Internal
    @Override
    public MappingTree parseMappings(Path path) {
        if (Files.notExists(path)) {
            throw new IllegalArgumentException("File does not exist!");
        }

        var mappingTree = new MappingTree();
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            MappingTree.MappingNode currentPackageOrClass = null;

            while ((line = bufferedReader.readLine()) != null) {
                if(line.startsWith("#")) {
                    // Comment
                    continue;
                }

                if(line.endsWith(":")) {
                    currentPackageOrClass = parseNode(currentPackageOrClass, line, mappingTree);
                } else if(currentPackageOrClass != null && line.contains("->")) {
                    currentPackageOrClass.addChild(parseMethodOrField(currentPackageOrClass, line));
                }
            }
        } catch(IOException exception) {
            throw new RuntimeException("Failed to read file!", exception);
        }

        return mappingTree;
    }

    // TODO: Figure out why it only adds the first package
    private static MappingTree.MappingNode parseNode(MappingTree.MappingNode parent, String line, MappingTree mappingTree) {
        String[] split = line.split(" -> ");
        String classPath = split[0].trim();
        String obfuscatedName = split[1].replace(":", "").trim();

        String[] packageSplit = classPath.split("\\.");
        List<String> packages = new ArrayList<>(Arrays.asList(packageSplit).subList(0, packageSplit.length - 1));

        MappingTree.MappingNode previousPackage = null;
        for (String aPackage : packages) {
            MappingTree.MappingNode node = new MappingTree.MappingNode(aPackage, parent);
            if (previousPackage == null && parent == null) {
                mappingTree.addRootNode(node);
            } else if(previousPackage != null) {
                previousPackage.addChild(node);
            } else {
                parent.addChild(node);
            }

            previousPackage = node;
        }

        String className = packageSplit[packageSplit.length - 1];
        MappingTree.ObfuscatedNode classNode = new MappingTree.ObfuscatedNode(className, obfuscatedName, parent);

        if (previousPackage == null) {
            mappingTree.addRootNode(classNode);
        } else {
            previousPackage.addChild(classNode);
        }

        return classNode;
    }

    private static MappingTree.ObfuscatedNode parseMethodOrField(MappingTree.MappingNode parent, String line) {
        String[] split = line.split(" -> ");
        String signature = split[0].trim();
        String obfuscatedName = split[1].trim();

        // determine if method or field
        String[] parts = signature.split(" ");
        if(!signature.contains("("))  {
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
            if (infoSplit.length == 3) {
                String fromLineStr = infoSplit[0];
                String toLineStr = infoSplit[1];

                try {
                    fromLine = Integer.parseInt(fromLineStr);
                    toLine = Integer.parseInt(toLineStr);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Failed to parse line numbers!", exception);
                }

                returnType = infoSplit[2];
            }

            String[] nameParamsSplit = parts[1].split("\\(");
            String name = nameParamsSplit[0];
            String[] params = nameParamsSplit[1].replace(")", "").split(",");

            return new MappingTree.MethodNode(name, obfuscatedName, fromLine, toLine, returnType, List.of(params), parent);
        }
    }

    @Override
    public MappingTree getCachedTree() {
        return this.mappingTree;
    }
}
