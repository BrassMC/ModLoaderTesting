package dev.turtywurty.testgradleplugin.asset;

import com.google.gson.JsonObject;

public record AssetObject(String name, String hash, long size) {
    public static AssetObject fromJson(String name, JsonObject json) {
        String hash = json.get("hash").getAsString();
        long size = json.get("size").getAsLong();

        return new AssetObject(name, hash, size);
    }
}
