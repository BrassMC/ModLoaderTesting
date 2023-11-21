package dev.turtywurty.testgradleplugin.piston.version;

import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import org.gradle.internal.impldep.com.google.gson.JsonObject;

public record AssetIndex(String id, String sha1, int size, int totalSize, String url) {
    public static AssetIndex fromJson(JsonObject json) {
        return TestGradlePlugin.GSON.fromJson(json, AssetIndex.class);
    }
}
