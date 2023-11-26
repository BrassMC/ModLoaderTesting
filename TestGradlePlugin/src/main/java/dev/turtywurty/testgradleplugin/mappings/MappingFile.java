package dev.turtywurty.testgradleplugin.mappings;

import java.nio.file.Path;

public interface MappingFile {
    MappingTree getCachedTree();
    MappingTree parseMappings(Path path);
    String remapClass(String className);
    String findPackage(String name);
}
