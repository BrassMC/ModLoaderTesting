package dev.turtywurty.testgradleplugin.asset;

import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;

public record AssetObject(String hash, long size) {
    public static AssetObject fromJson(JsonObject json) {
        String hash = json.get("hash").getAsString();
        long size = json.get("size").getAsLong();

        return new AssetObject(hash, size);
    }

    public String getPath() {
        return hash.substring(0, 2) + "/" + hash;
    }
}
