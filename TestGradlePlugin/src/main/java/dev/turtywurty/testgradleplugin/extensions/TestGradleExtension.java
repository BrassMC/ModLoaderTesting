package dev.turtywurty.testgradleplugin.extensions;

import org.gradle.api.provider.Property;

public abstract class TestGradleExtension {
    public abstract Property<String> getMinecraftVersion();
}
