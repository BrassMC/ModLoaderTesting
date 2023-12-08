package dev.turtywurty.testgradleplugin.extensions;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public abstract class TestGradleExtension {
    public abstract Property<String> getMinecraftVersion();

    public abstract Property<String> getVineflowerVersion();

    public abstract Property<String> getSide();

    public Provider<Side> getSideEnum() {
        return getSide().map(str -> switch (str.toLowerCase()) {
            case "client" -> Side.CLIENT;
            case "server" -> Side.SERVER;
            default -> Side.BOTH;
        });
    }

    public enum Side {
        CLIENT,
        SERVER,
        BOTH
    }
}
