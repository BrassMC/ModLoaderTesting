package dev.turtywurty.testgradleplugin.mappings;

import java.nio.file.Path;
import java.util.function.Predicate;

public interface MappingFile {
    MappingTree getCachedTree();

    MappingTree parseMappings(Path path);

    String findPath(String name, NodeType type);

    enum NodeType {
        PACKAGE(node -> !(node instanceof MappingTree.ObfuscatedNode)),
        CLASS(node -> node instanceof MappingTree.ClassNode),
        METHOD(node -> node instanceof MappingTree.MethodNode),
        FIELD(node -> node instanceof MappingTree.FieldNode),
        ANY(node -> true);

        private final Predicate<MappingTree.MappingNode> predicate;

        NodeType(Predicate<MappingTree.MappingNode> predicate) {
            this.predicate = predicate;
        }

        public Predicate<MappingTree.MappingNode> getPredicate() {
            return this.predicate;
        }
    }
}
