package dev.turtywurty.testgradleplugin.piston.version;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.OperatingSystem;

import java.util.Map;
import java.util.regex.Pattern;

public final class LaunchRules {
    private LaunchRules() {
    }

    public static boolean allows(JsonArray rules) {
        return allows(rules, OperatingSystem.determineCurrentOperatingSystem().getName(),
            System.getProperty("os.arch"), System.getProperty("os.version"), Map.of());
    }

    public static boolean allows(
        JsonArray rules,
        String os,
        String arch,
        String version,
        Map<String, Boolean> features
    ) {
        if (rules == null)
            return true;

        boolean allowed = false;
        for (JsonElement element : rules) {
            JsonObject rule = element.getAsJsonObject();
            if (rule.has("os")) {
                JsonObject platform = rule.getAsJsonObject("os");
                if (platform.has("name") && !platform.get("name").getAsString().equals(os))
                    continue;

                if (platform.has("arch") && !Pattern.matches(platform.get("arch").getAsString(), arch))
                    continue;

                if (platform.has("version") && !Pattern.matches(platform.get("version").getAsString(), version))
                    continue;
            }

            boolean matches = true;
            if (rule.has("features")) {
                for (Map.Entry<String, JsonElement> feature : rule.getAsJsonObject("features").entrySet()) {
                    if (features.getOrDefault(feature.getKey(), false) != feature.getValue().getAsBoolean()) {
                        matches = false;
                        break;
                    }
                }
            }

            if (matches) {
                allowed = rule.get("action").getAsString().equals("allow");
            }
        }

        return allowed;
    }
}
