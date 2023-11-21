package dev.turtywurty.testgradleplugin.piston.version;

import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import org.gradle.internal.impldep.com.google.gson.JsonObject;

public record JavaVersion(String component, int majorVersion) {
    public static JavaVersion fromJson(JsonObject json) {
        return TestGradlePlugin.GSON.fromJson(json, JavaVersion.class);
    }
}
