package dev.turtywurty.testgradleplugin.piston.version;

import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import org.gradle.internal.impldep.com.google.gson.JsonObject;

public record MinecraftArguments(GameArgument[] game, JvmArgument[] jvm) {
    public static MinecraftArguments fromJson(JsonObject json) {
        return TestGradlePlugin.GSON.fromJson(json, MinecraftArguments.class);
    }

    // Method to get a specific argument value by name
    public String getArgumentValue(String argumentName, boolean isJvmArgs) {
        return isJvmArgs ? getJvmArgumentValue(argumentName) : getGameArgumentValue(argumentName);
    }

    private String getGameArgumentValue(String argumentName) {
        if (game != null) {
            for (GameArgument gameArg : game) {
                String[] values = gameArg.value();
                if (values != null) {
                    for (int i = 0; i < values.length - 1; i += 2) {
                        if (values[i].equals(argumentName)) {
                            return values[i + 1];
                        }
                    }
                }
            }
        }

        return null;
    }

    private String getJvmArgumentValue(String argumentName) {
        if (jvm != null) {
            for (JvmArgument jvmArg : jvm) {
                Object value = jvmArg.value();
                if (value instanceof String str && argumentName.equals(str)) {
                    return str;
                }
            }
        }

        return null;
    }
}

record GameArgument(String[] value, Rule[] rules) {}

record Rule(String action, Features features, String value) {}

class Features {
    private boolean is_demo_user;
    private boolean has_custom_resolution;
    private boolean has_quick_plays_support;
    private boolean is_quick_play_singleplayer;
    private boolean is_quick_play_multiplayer;
    private boolean is_quick_play_realms;

    public boolean isIs_demo_user() {
        return is_demo_user;
    }

    public boolean isHas_custom_resolution() {
        return has_custom_resolution;
    }

    public boolean isHas_quick_plays_support() {
        return has_quick_plays_support;
    }

    public boolean isIs_quick_play_singleplayer() {
        return is_quick_play_singleplayer;
    }

    public boolean isIs_quick_play_multiplayer() {
        return is_quick_play_multiplayer;
    }

    public boolean isIs_quick_play_realms() {
        return is_quick_play_realms;
    }
}

record JvmArgument(Object value, Rule[] rules) {

}