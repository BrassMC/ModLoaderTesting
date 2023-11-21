package dev.turtywurty.testgradleplugin.piston;

import dev.turtywurty.testgradleplugin.TestGradlePlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public record PistonMeta(PistonMetaLatestVersion latest,
                         PistonMetaVersion[] versions) {
    public static final PistonMeta SELF = loadMeta();

    private static PistonMeta loadMeta() {
        String url = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

        try(InputStream stream = new URL(url).openStream()) {
            String json = new String(stream.readAllBytes());
            return TestGradlePlugin.GSON.fromJson(json, PistonMeta.class);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load piston meta!", exception);
        }
    }

    public PistonMetaVersion findVersion(String version) {
        for (PistonMetaVersion metaVersion : this.versions) {
            if (metaVersion.id().equals(version)) {
                return metaVersion;
            }
        }

        return null;
    }
}
