package dev.turtywurty.testgradleplugin.mappings;

import java.util.Objects;

public final class FieldMapping {
    private final String descriptor;
    private final String originalName;
    private final String obfuscatedName;
    private String obfuscatedDescriptor;

    public FieldMapping(String descriptor, String originalName, String obfuscatedName) {
        this.descriptor = descriptor;
        this.originalName = originalName;
        this.obfuscatedName = obfuscatedName;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getObfuscatedName() {
        return obfuscatedName;
    }

    public String getObfuscatedDescriptor() {
        return obfuscatedDescriptor;
    }

    public void setObfuscatedDescriptor(String obfuscatedDesc) {
        this.obfuscatedDescriptor = obfuscatedDesc;
    }

    public String obfuscatedDescriptor() {
        return obfuscatedDescriptor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FieldMapping) obj;
        return Objects.equals(this.descriptor, that.descriptor) &&
                Objects.equals(this.originalName, that.originalName) &&
                Objects.equals(this.obfuscatedName, that.obfuscatedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptor, originalName, obfuscatedName);
    }

    @Override
    public String toString() {
        return "FieldMapping[" +
                "descriptor=" + descriptor + ", " +
                "originalName=" + originalName + ", " +
                "obfuscatedName=" + obfuscatedName + ']';
    }
}
