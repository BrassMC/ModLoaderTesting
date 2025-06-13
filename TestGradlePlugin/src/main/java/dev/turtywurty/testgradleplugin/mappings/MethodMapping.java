package dev.turtywurty.testgradleplugin.mappings;

public class MethodMapping {
    private final int startLine;
    private final int endLine;
    private final String signature;
    private final String obfuscatedName;
    private String obfuscatedDescriptor;

    public MethodMapping(int startLine, int endLine, String signature, String obfuscatedName) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.signature = signature;
        this.obfuscatedName = obfuscatedName;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String getSignature() {
        return signature;
    }

    public String getObfuscatedName() {
        return obfuscatedName;
    }

    public String getObfuscatedDescriptor() {
        return obfuscatedDescriptor;
    }

    public String getOriginalName() {
        int idx = signature.indexOf('(');
        if (idx == -1)
            return signature;

        return signature.substring(0, idx);
    }

    public String getDescriptor() {
        int idx = signature.indexOf('(');
        return (idx == -1) ? "" : signature.substring(idx);
    }

    public String getReturnType() {
        int idx = signature.indexOf(')');
        if (idx == -1 || idx + 1 >= signature.length())
            return "V"; // Default to void if no return type is specified

        return signature.substring(idx + 1);
    }

    public void setObfuscatedDescriptor(String obfuscatedDesc) {
        this.obfuscatedDescriptor = obfuscatedDesc;
    }
}
