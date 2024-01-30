package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import org.gradle.api.Task;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

import java.nio.file.Path;

public interface TestGradleTask extends Task {
    @Internal
    default TestGradleExtension getExtension() {
        return getProject().getExtensions().getByType(TestGradleExtension.class);
    }

    @Internal
    default String getMinecraftVersion() {
        Property<String> minecraftVersion = getExtension().getMinecraftVersion();
        if (!minecraftVersion.isPresent())
            throw new IllegalStateException("Minecraft version is not present!");

        return minecraftVersion.get();
    }

    @Internal
    default TestGradleExtension.Side getSide() {
        return getExtension().getSideEnum().getOrElse(TestGradleExtension.Side.BOTH);
    }

    @Internal
    default String getVineflowerVersion() {
        Property<String> vineflowerVersion = getExtension().getVineflowerVersion();
        if (!vineflowerVersion.isPresent())
            throw new IllegalStateException("Vineflower version is not present!");

        return vineflowerVersion.get();
    }

    @Internal
    default boolean isClient() {
        return getSide() == TestGradleExtension.Side.CLIENT || getSide() == TestGradleExtension.Side.BOTH;
    }

    @Internal
    default boolean isServer() {
        return getSide() == TestGradleExtension.Side.SERVER || getSide() == TestGradleExtension.Side.BOTH;
    }

    @Internal
    default Path getCacheDir() {
        return getProject().getGradle().getGradleUserHomeDir().toPath().resolve("caches/testGradle");
    }
}
