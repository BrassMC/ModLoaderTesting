package dev.turtywurty.testgradleplugin.asset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AssetIndexHash {
    private final List<AssetObject> assets = new ArrayList<>();

    public AssetIndexHash(Collection<AssetObject> assets) {
        this.assets.addAll(assets);
    }

    public AssetIndexHash() {}

    public static AssetIndexHash fromJson(JsonObject json) {
        List<AssetObject> assets = new ArrayList<>();

        JsonObject objects = json.getAsJsonObject("objects");
        for (Map.Entry<String, JsonElement> assetEntry : objects.entrySet()) {
            String assetPath = assetEntry.getKey();
            JsonElement assetElement = assetEntry.getValue();
            if(!assetElement.isJsonObject()) {
                throw new RuntimeException("Asset element is not a json object! " + assetElement);
            }

            JsonObject assetObject = assetElement.getAsJsonObject();
            assets.add(AssetObject.fromJson(assetPath, assetObject));
        }

        return new AssetIndexHash(assets);
    }

    public List<AssetObject> getAssets() {
        return this.assets;
    }
}
