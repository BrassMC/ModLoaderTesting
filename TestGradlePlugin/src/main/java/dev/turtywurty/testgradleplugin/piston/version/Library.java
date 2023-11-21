package dev.turtywurty.testgradleplugin.piston.version;

import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import org.gradle.internal.impldep.com.google.gson.JsonArray;
import org.gradle.internal.impldep.com.google.gson.JsonElement;
import org.gradle.internal.impldep.com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Library(Download artifact, String name, Optional<DownloadRules> rules) {
    public static List<Library> fromJsonArray(JsonArray json) {
        List<Library> libraries = new ArrayList<>();
        for (JsonElement jsonElement : json) {
            if (!jsonElement.isJsonObject())
                continue;

            libraries.add(fromJson(jsonElement.getAsJsonObject()));
        }

        return libraries;
    }

    public static Library fromJson(JsonObject json) {
        Download artifact;
        if (json.has("downloads")) {
            JsonObject downloadsJson = json.getAsJsonObject("downloads");
            if (downloadsJson.has("artifact")) {
                JsonObject artifactJson = downloadsJson.getAsJsonObject("artifact");
                artifact = Download.fromJson(artifactJson);
            } else {
                artifact = null;
            }
        } else {
            artifact = null;
        }

        String name = json.get("name").getAsString();

        Optional<DownloadRules> rules;
        if (json.has("rules")) {
            JsonObject rulesJson = json.getAsJsonObject("rules");
            rules = Optional.of(DownloadRules.fromJson(rulesJson));
        } else {
            rules = Optional.empty();
        }

        return new Library(artifact, name, rules);
    }

    public record DownloadRules(Action action, OperatingSystem os) {
        public static DownloadRules fromJson(JsonObject json) {
            Action action;
            if (json.has("action")) {
                action = Action.valueOf(json.get("action").getAsString());
            } else {
                action = null;
            }

            OperatingSystem os;
            if (json.has("os")) {
                JsonObject osJson = json.getAsJsonObject("os");
                os = OperatingSystem.fromJson(osJson);
            } else {
                os = null;
            }

            return new DownloadRules(action, os);
        }

        public enum Action {
            ALLOW, DISALLOW
        }

        public record OperatingSystem(String name) {
            public static OperatingSystem fromJson(JsonObject json) {
                return TestGradlePlugin.GSON.fromJson(json, OperatingSystem.class);
            }
        }
    }
}
