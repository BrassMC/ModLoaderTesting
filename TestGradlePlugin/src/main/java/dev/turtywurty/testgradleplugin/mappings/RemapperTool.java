package dev.turtywurty.testgradleplugin.mappings;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RemapperTool {

    /**
     * Remap one .class file, write it in its correct package path, and return the Path to the new file.
     *
     * @param filePath  the input .class file to remap
     * @param outputDir the directory where the remapped class should be written
     * @param mappings  your ClassMapping list
     * @return the Path of the newly‐written remapped .class
     */
    public static Path remapClass(@NotNull Path filePath,
                                  @NotNull Path outputDir,
                                  List<ClassMapping> mappings) {
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException("Mappings list cannot be empty");
        }
        if (!Files.isRegularFile(filePath) || !filePath.toString().endsWith(".class")) {
            throw new IllegalArgumentException("File must be a valid .class file: " + filePath);
        }

        byte[] classBytes;
        try {
            classBytes = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read class file: " + filePath, e);
        }

        var cr = new ClassReader(classBytes);
        var cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        var remapper = new MappingsRemapper(mappings);
        var cv = new ClassRemapper(cw, remapper);
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        byte[] remappedBytes = cw.toByteArray();

        String internalObfName = cr.getClassName();
        ClassMapping mapping = mappings.stream()
                .filter(m -> m.getObfuscatedName().equals(internalObfName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No ClassMapping for " + internalObfName));

        String originalDotName = mapping.getOriginalName();
        String originalInternal = originalDotName.replace('.', '/');
        Path target = outputDir.resolve(originalInternal + ".class");

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, remappedBytes);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write remapped class to " + target, e);
        }

        return target;
    }

    public static class MappingsRemapper extends Remapper {
        private final List<ClassMapping> mappings;

        public MappingsRemapper(List<ClassMapping> mappings) {
            this.mappings = mappings;
        }

        @Override
        public String map(String internalName) {
            return mappings.stream()
                    .filter(m -> m.getObfuscatedName().equals(internalName))
                    .map(ClassMapping::getOriginalName)
                    .map(dotName -> dotName.replace('.', '/'))
                    .findFirst()
                    .orElse(super.map(internalName));
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            System.out.println("Mapping method: " + owner + "." + name + descriptor);

            return mappings.stream()
                    .filter(cm -> cm.getObfuscatedName().equals(owner))
                    .flatMap(cm -> cm.getMethods().stream())
                    .filter(mm -> mm.getObfuscatedName().equals(name)
                            && mm.getObfuscatedDescriptor().equals(descriptor))
                    .map(MethodMapping::getOriginalName)
                    .findFirst()
                    .orElse(super.mapMethodName(owner, name, descriptor));
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            return mappings.stream()
                    .filter(cm -> cm.getObfuscatedName().equals(owner))
                    .flatMap(cm -> cm.getFields().stream())
                    .filter(fm -> fm.getObfuscatedName().equals(name)
                            && fm.getObfuscatedDescriptor().equals(descriptor))
                    .map(FieldMapping::getOriginalName)
                    .findFirst()
                    .orElse(super.mapFieldName(owner, name, descriptor));
        }
    }
}
