package dev.turtywurty.testgradleplugin.mappings;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class MappingTree {
    private final List<MappingNode> rootNodes = new ArrayList<>();

    public List<MappingNode> getRootNodes() {
        return List.copyOf(this.rootNodes);
    }

    public void addRootNode(MappingNode node) {
        this.rootNodes.add(node);
    }

    public @Nullable MappingNode findNode(String previousPackage, int atDepth, Collection<MappingNode> nodes, Predicate<MappingNode> predicate) {
        for (MappingNode node : nodes) {
            if (node.getName().equals(previousPackage)) {
                if (atDepth == 0 && predicate.test(node)) {
                    return node;
                }

                return findNode(previousPackage, atDepth - 1, node.getChildren().values(), predicate);
            }

            MappingNode foundNode;
            while ((foundNode = findNode(previousPackage, atDepth, node.getChildren().values(), predicate)) != null) {
                return foundNode;
            }

            if (atDepth == 0 && predicate.test(node)) {
                return node;
            }

            return findNode(previousPackage, getMaxDepth(), node.getChildren().values(), predicate);
        }

        return null;
    }

    public MappingNode findNode(String previousPackage, int depth, Predicate<MappingNode> predicate) {
        return findNode(previousPackage, depth, this.rootNodes, predicate);
    }

    public MappingNode findNode(String previousPackage, int depth) {
        return findNode(previousPackage, depth, this.rootNodes, mappingNode -> true);
    }

    public @Nullable MappingNode findNode(String name, Predicate<MappingNode> predicate) {
        for (MappingNode rootNode : this.rootNodes) {
            MappingNode node = findNode(name, getMaxDepth(), rootNode.getChildren().values(), predicate);
            if (node != null) {
                return node;
            }
        }

        return null;
    }

    public @Nullable MappingNode findNode(String name) {
        for (MappingNode rootNode : this.rootNodes) {
            MappingNode node = findNode(name, getMaxDepth(), rootNode.getChildren().values(), mappingNode -> true);
            if (node != null) {
                return node;
            }
        }

        return null;
    }

    // TODO: Fix StackOverflowError
    public int getMaxDepth(int depth, Collection<MappingNode> nodes) {
        int maxDepth = depth;
        for (MappingNode node : nodes) {
            int nextDepth = getMaxDepth(depth + 1, node.getChildren().values());
            if (nextDepth > maxDepth) {
                maxDepth = nextDepth;
            }
        }

        return maxDepth;
    }

    public int getMaxDepth() {
        return getMaxDepth(0, this.rootNodes);
    }

    public void print() {
        for (MappingNode rootNode : this.rootNodes) {
            rootNode.print(0);
        }
    }

    public static class MappingNode {
        protected final String name;
        protected final MappingNode parent;
        protected final Map<String, MappingNode> children = new HashMap<>();

        public MappingNode(String name, MappingNode parent) {
            this.name = name;
            this.parent = parent;
        }

        public String getName() {
            return this.name;
        }

        public MappingNode getParent() {
            return this.parent;
        }

        public Map<String, MappingNode> getChildren() {
            return Map.copyOf(this.children);
        }

        public void addChild(MappingNode node) {
            this.children.put(node.getName(), node);
        }

        @Override
        public String toString() {
            return getPrintText();
        }

        public String getPrintText() {
            return this.name;
        }

        public void print(int depth) {
            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }

            System.out.println(getPrintText());
            for (MappingNode child : this.children.values()) {
                child.print(depth + 1);
            }
        }
    }

    public static class ObfuscatedNode extends MappingNode {
        protected final String obfuscatedName;

        public ObfuscatedNode(String name, String obfuscatedName, MappingNode parent) {
            super(name, parent);
            this.obfuscatedName = obfuscatedName;
        }

        public String getObfuscatedName() {
            return this.obfuscatedName;
        }

        @Override
        public String getPrintText() {
            return this.obfuscatedName + " -> " + super.getPrintText();
        }
    }

    public static class MethodNode extends ObfuscatedNode {
        protected final int fromLine, toLine;
        protected final String returnType;
        protected final List<String> parameters;

        public MethodNode(String name, String obfuscatedName, int fromLine, int toLine, String returnType, List<String> parameters, MappingNode parent) {
            super(name, obfuscatedName, parent);
            this.fromLine = fromLine;
            this.toLine = toLine;
            this.returnType = returnType;
            this.parameters = parameters;
        }

        public int getFromLine() {
            return this.fromLine;
        }

        public int getToLine() {
            return this.toLine;
        }

        public String getReturnType() {
            return this.returnType;
        }

        public List<String> getParameters() {
            return List.copyOf(this.parameters);
        }

        @Override
        public String getPrintText() {
            return super.getPrintText() + " (" + this.returnType + " " + String.join(", ", this.parameters) + ")";
        }
    }

    public static class FieldNode extends ObfuscatedNode {
        protected final String type;

        public FieldNode(String name, String obfuscatedName, String type, MappingNode parent) {
            super(name, obfuscatedName, parent);
            this.type = type;
        }

        public String getType() {
            return this.type;
        }

        @Override
        public String getPrintText() {
            return super.getPrintText() + " (" + this.type + ")";
        }
    }
}
