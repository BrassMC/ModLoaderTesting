package dev.turtywurty.testgradleplugin.extensions;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.util.Locale;

public abstract class TestGradleExtension {
    public abstract Property<String> getMinecraftVersion();

    public abstract Property<String> getVineflowerVersion();

    public abstract Property<String> getSide();

    public abstract Property<String> getUsername();

    public abstract Property<String> getUuid();

    public abstract Property<String> getAccessToken();

    public Provider<Side> getSideEnum() {
        return getSide().map(str -> Side.valueOf(str.toUpperCase(Locale.ROOT))).orElse(Side.CLIENT);
    }

    public enum Side {
        CLIENT,
        SERVER,
        BOTH
    }
}
