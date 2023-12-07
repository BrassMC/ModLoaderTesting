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

    public @Nullable MappingNode findNode(Collection<MappingNode> nodes, Predicate<MappingNode> predicate) {
        Deque<MappingNode> stack = new ArrayDeque<>(nodes);
        while (!stack.isEmpty()) {
            MappingNode node = stack.pop();
            if (predicate.test(node)) {
                return node;
            }

            // the following line is commented out because it doesn't respect the package hierarchy
            // stack.addAll(node.getChildren().values());
        }
        return null;
    }

    public @Nullable MappingNode findNodeFull(Collection<MappingNode> nodes, Predicate<MappingNode> predicate) {
        Deque<MappingNode> stack = new ArrayDeque<>(nodes);
        while (!stack.isEmpty()) {
            MappingNode node = stack.pop();
            if (predicate.test(node)) {
                return node;
            }

            stack.addAll(node.getChildren().values());
        }
        return null;
    }

    public MappingNode findNodeFull(Predicate<MappingNode> predicate) {
        return findNodeFull(this.rootNodes, predicate);
    }

    public MappingNode findNode(Predicate<MappingNode> predicate) {
        return findNode(this.rootNodes, predicate);
    }

    public MappingNode findNode() {
        return findNode(this.rootNodes, mappingNode -> true);
    }

    public @Nullable MappingNode findNode(String name, Predicate<MappingNode> predicate) {
        return findNode(this.rootNodes, predicate.and(mappingNode -> mappingNode.getName().equals(name)));
    }

    public @Nullable MappingNode findNode(String name) {
        return findNode(name, mappingNode -> mappingNode.getName().equals(name));
    }

    public List<MappingNode> findNodesAtDepth(int depth) {
        if (depth <= 0) {
            return this.rootNodes;
        }

        List<MappingNode> nodes = new ArrayList<>();
        Deque<MappingNode> stack = new ArrayDeque<>(this.rootNodes);
        int currentDepth = 1;

        while (!stack.isEmpty() && currentDepth <= depth) {
            int size = stack.size();
            for (int i = 0; i < size; i++) {
                MappingNode node = stack.pop();
                stack.addAll(node.getChildren().values());
                if (currentDepth == depth) {
                    nodes.add(node);
                }
            }
            currentDepth++;
        }

        return nodes;
    }

    public int getMaxDepth(int depth, Collection<MappingNode> nodes) {
        int maxDepth = depth;
        for (MappingNode node : nodes) {
            int nodeDepth = getMaxDepth(depth + 1, node.getChildren().values());
            if (nodeDepth > maxDepth) {
                maxDepth = nodeDepth;
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

    public static class ClassNode extends ObfuscatedNode {
        public ClassNode(String name, String obfuscatedName, MappingNode parent) {
            super(name, obfuscatedName, parent);
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
