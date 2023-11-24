package dev.turtywurty.testgradleplugin.asset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

public class AssetIndexHash {
    private final Map<String, AssetObject> assets = new HashMap<>();

    public AssetIndexHash(Map<String, AssetObject> assets) {
        this.assets.putAll(assets);
    }

    public AssetIndexHash() {}

    public static AssetIndexHash fromJson(JsonObject json) {
        Map<String, AssetObject> assets = new HashMap<>();

        JsonObject objects = json.getAsJsonObject("objects");
        for (Map.Entry<String, JsonElement> assetEntry : objects.entrySet()) {
            String assetPath = assetEntry.getKey();
            JsonElement assetElement = assetEntry.getValue();
            if(!assetElement.isJsonObject()) {
                throw new RuntimeException("Asset element is not a json object! " + assetElement);
            }

            JsonObject assetObject = assetElement.getAsJsonObject();
            assets.put(assetPath, AssetObject.fromJson(assetObject));
        }

        return new AssetIndexHash(assets);
    }

    public Map<String, AssetObject> getAssets() {
        return Collections.unmodifiableMap(assets);
    }
}
