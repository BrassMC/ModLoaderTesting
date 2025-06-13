package dev.turtywurty.testgradleplugin.mappings;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OfficialMappingsFile {
    private static final Map<Path, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private record CacheEntry(List<ClassMapping> classes, long lastModified) {
    }

    /**
     * Parses the given mappings file and returns a list of ClassMapping instances.
     *
     * @param file the client mappings file
     * @return list of class mappings
     * @throws IOException if reading the file fails
     */
    public static List<ClassMapping> parse(Path file) throws IOException {
        CacheEntry cached = CACHE.get(file);
        if (cached != null && Files.exists(file) && Files.getLastModifiedTime(file).toMillis() == cached.lastModified) {
            return cached.classes;
        }

        List<ClassMapping> classes = new ArrayList<>();
        ClassMapping current = null;
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String rawLine;
            while ((rawLine = reader.readLine()) != null) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Class header: originalName -> obfuscatedName:
                if (line.endsWith(":") && line.contains(" -> ")) {
                    String header = line.substring(0, line.length() - 1);
                    String[] parts = header.split(" -> ", 2);
                    current = new ClassMapping(parts[0], parts[1]);
                    classes.add(current);
                }
                // Metadata JSON line starting with # {
                else if (rawLine.trim().startsWith("# {") && current != null) {
                    String json = rawLine.trim();
                    try {
                        // Strip leading '# ' and braces
                        int start = json.indexOf('{') + 1;
                        int end = json.lastIndexOf('}');
                        String body = json.substring(start, end);
                        String[] entries = body.split(",");
                        for (String entry : entries) {
                            String[] kv = entry.split(":", 2);
                            String key = kv[0].trim().replace("\"", "");
                            String value = kv[1].trim().replace("\"", "");
                            if ("fileName".equals(key)) {
                                current.setFileName(value);
                            } else if ("id".equals(key)) {
                                current.setId(value);
                            }
                        }
                    } catch (Exception e) {
                        // ignore malformed metadata
                    }
                }
                // Member mappings
                else if (current != null && rawLine.startsWith("    ")) {
                    String mapping = rawLine.trim();
                    // Method mapping: start:end:signature -> name
                    if (mapping.matches("\\d+:\\d+:.+ -> .+")) {
                        String[] parts = mapping.split(" -> ", 2);
                        String obfName = parts[1].trim();
                        String[] left = parts[0].split(":", 3);
                        int startLine = Integer.parseInt(left[0]);
                        int endLine = Integer.parseInt(left[1]);
                        String signature = left[2];
                        current.addMethodMapping(new MethodMapping(startLine, endLine, signature, obfName));
                    }
                    // Field mapping: descriptor name -> obf
                    else if (mapping.contains(" -> ")) {
                        String[] parts = mapping.split(" -> ", 2);
                        String obfName = parts[1].trim();
                        String[] decl = parts[0].trim().split(" ", 2);
                        String descriptor = decl[0];
                        String originalName = decl[1];
                        current.addFieldMapping(new FieldMapping(descriptor, originalName, obfName));
                    }
                }
            }
        }

        CACHE.put(file, new CacheEntry(Collections.unmodifiableList(classes), Files.getLastModifiedTime(file).toMillis()));
        return classes;
    }
}
