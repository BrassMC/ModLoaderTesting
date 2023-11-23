package dev.turtywurty.testgradleplugin.tasks;

import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import dev.turtywurty.testgradleplugin.asset.AssetIndexHash;
import dev.turtywurty.testgradleplugin.asset.AssetObject;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
public abstract class DownloadAssetsTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void downloadAssets() {
        System.out.println("Downloading assets!");

        Path versionPath = getOutputDir()
                .orElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .get()
                .getAsFile()
                .toPath()
                .resolve(getVersion().get());

        Path versionJsonPath = versionPath.resolve("version.json");

        VersionPackage versionPackage = VersionPackage.fromPath(versionJsonPath);
        System.out.println("Version package path: " + versionJsonPath);

        String assetsUrl = versionPackage.assetIndex().url();
        System.out.println("Assets url: " + assetsUrl);

        AssetIndexHash assetIndexHash;
        try(InputStream stream = new URL(assetsUrl).openStream()) {
            var jsonStr = new String(stream.readAllBytes());
            JsonObject json = TestGradlePlugin.GSON.fromJson(jsonStr, JsonObject.class);
            assetIndexHash = AssetIndexHash.fromJson(json);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to download asset index!", exception);
        }

        System.out.println("Asset index hash: " + assetIndexHash);

        Path assetsPath = versionPath.resolve("assets");
        try {
            Files.createDirectories(assetsPath);

            for (AssetObject asset : assetIndexHash.getAssets()) {
                Path assetPath = assetsPath.resolve(asset.hash().substring(0, 2)).resolve(asset.hash());
                if (Files.notExists(assetPath)) {
                    System.out.println("Downloading asset: " + asset.name());

                    String url = "https://resources.download.minecraft.net/%s/%s".formatted(
                            asset.hash().substring(0, 2), asset.hash());
                    try(InputStream stream = new URL(url).openStream()) {
                        Files.createDirectories(assetPath.getParent());
                        Files.write(assetPath, stream.readAllBytes());
                    } catch (IOException exception) {
                        throw new RuntimeException("Failed to download asset!", exception);
                    }

                    Thread.sleep(100);
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create assets directory!", exception);
        } catch (InterruptedException exception) {
            throw new RuntimeException("Failed to sleep!", exception);
        }
    }
}
