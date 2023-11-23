package dev.turtywurty.testgradleplugin.piston.version;

import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;

public record AssetIndex(String id, String sha1, int size, int totalSize, String url) {
    public static AssetIndex fromJson(JsonObject json) {
        return TestGradlePlugin.GSON.fromJson(json, AssetIndex.class);
    }
}
