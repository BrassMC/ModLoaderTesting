package dev.turtywurty.testgradleplugin.mappings;

import java.util.ArrayList;
import java.util.List;

public class ClassMapping {
    private final String originalName;
    private final String obfuscatedName;
    private String fileName;
    private String id;
    private final List<FieldMapping> fields = new ArrayList<>();
    private final List<MethodMapping> methods = new ArrayList<>();

    public ClassMapping(String originalName, String obfuscatedName) {
        this.originalName = originalName;
        this.obfuscatedName = obfuscatedName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void addFieldMapping(FieldMapping field) {
        fields.add(field);
    }

    public void addMethodMapping(MethodMapping method) {
        methods.add(method);
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getObfuscatedName() {
        return obfuscatedName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getId() {
        return id;
    }

    public List<FieldMapping> getFields() {
        return fields;
    }

    public List<MethodMapping> getMethods() {
        return methods;
    }
}