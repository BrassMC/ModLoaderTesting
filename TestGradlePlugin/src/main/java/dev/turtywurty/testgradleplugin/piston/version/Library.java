package dev.turtywurty.testgradleplugin.piston.version;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record Library(Download artifact, String path, String name, JsonArray rules) {
    public boolean allowed() {
        return artifact != null && LaunchRules.allows(rules);
    }

    public static List<Library> fromJsonArray(JsonArray json) {
        List<Library> libraries = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject library = element.getAsJsonObject();
            JsonObject downloads = library.getAsJsonObject("downloads");
            JsonObject artifact = downloads == null ? null : downloads.getAsJsonObject("artifact");
            libraries.add(new Library(Download.fromJson(artifact),
                artifact == null ? null : artifact.get("path").getAsString(),
                library.get("name").getAsString(), library.getAsJsonArray("rules")));
        }

        return List.copyOf(libraries);
    }
}
