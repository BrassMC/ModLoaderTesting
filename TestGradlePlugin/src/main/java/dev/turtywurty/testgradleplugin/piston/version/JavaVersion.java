package dev.turtywurty.testgradleplugin.piston.version;

import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;

public record JavaVersion(String component, int majorVersion) {
    public static JavaVersion fromJson(JsonObject json) {
        return TestGradlePlugin.GSON.fromJson(json, JavaVersion.class);
    }
}
