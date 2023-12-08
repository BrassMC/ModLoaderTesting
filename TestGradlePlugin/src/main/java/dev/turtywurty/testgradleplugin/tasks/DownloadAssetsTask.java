package dev.turtywurty.testgradleplugin.tasks;

import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.HashingFunction;
import dev.turtywurty.testgradleplugin.OperatingSystem;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import dev.turtywurty.testgradleplugin.asset.AssetIndexHash;
import dev.turtywurty.testgradleplugin.asset.AssetObject;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@CacheableTask
public abstract class DownloadAssetsTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @Input
    @Optional
    public abstract Property<String> getAssetsUrl();

    @Input
    @Optional
    public abstract Property<Integer> getConcurrentConnections();

    public DownloadAssetsTask() {
        getAssetsUrl().convention("https://resources.download.minecraft.net/");
        getConcurrentConnections().convention(8);
    }

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
        try (InputStream stream = new URL(assetsUrl).openStream()) {
            var jsonStr = new String(stream.readAllBytes());
            JsonObject json = TestGradlePlugin.GSON.fromJson(jsonStr, JsonObject.class);
            assetIndexHash = AssetIndexHash.fromJson(json);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to download asset index!", exception);
        }

        System.out.println("Asset index hash: " + assetIndexHash);

        Path assetsPath = versionPath.resolve("assets");
        Path objectsPath = assetsPath.resolve("objects");
        Path indexesPath = assetsPath.resolve("indexes/%s.json".formatted(versionPackage.assetIndex().id()));
        Path minecraftAssets = OperatingSystem.getMinecraftDir().resolve("assets/objects");
        try {
            Files.createDirectories(objectsPath);
            Files.createDirectories(indexesPath.getParent());

            Map<String, AssetObject> assets = assetIndexHash.getAssets();
            List<String> assetKeys = new ArrayList<>(assets.keySet());

            // sort the assets by their key
            Collections.sort(assetKeys);

            // remove any duplicates
            HashSet<String> assetSet = new HashSet<>(assets.size());
            assetKeys.removeIf(key -> key == null || !assetSet.add(assets.get(key).getPath()));

            ExecutorService executor = Executors.newFixedThreadPool(getConcurrentConnections().get());
            final CopyOnWriteArrayList<AssetObject> failedAssets = new CopyOnWriteArrayList<>();

            var indexJson = new JsonObject();
            var objectsJson = new JsonObject();
            for (String key : assetKeys) {
                AssetObject asset = assets.get(key);
                long size = asset.size();
                String hash = asset.hash();
                String path = asset.getPath();
                String assetUrl = getAssetsUrl().get() + asset.getPath();

                var assetJson = new JsonObject();
                assetJson.addProperty("hash", hash);
                assetJson.addProperty("size", size);
                objectsJson.add(key, assetJson);

                Path assetPath = objectsPath.resolve(path);
                Path minecraftAssetPath = minecraftAssets.resolve(asset.getPath());

                if (Files.exists(assetPath) && HashingFunction.SHA1.hash(assetPath).equals(hash)) {
                    // System.out.println("Skipping asset " + path + " as it already exists!");
                    continue;
                }

                Runnable copyHandler = () -> {
                    System.out.println("Copying asset " + path + " from " + minecraftAssetPath + " to " + assetPath + "!");
                    if (Files.exists(minecraftAssetPath) && HashingFunction.SHA1.hash(minecraftAssetPath).equals(hash)) {
                        try {
                            Files.createDirectories(assetPath.getParent());
                            Files.copy(minecraftAssetPath, assetPath);
                            return;
                        } catch (IOException ignored) {
                        }
                    }

                    System.out.println("Downloading asset " + path + " from " + assetUrl + " to " + assetPath + "!");
                    try (InputStream stream = new URL(assetUrl).openStream()) {
                        Files.createDirectories(assetPath.getParent());
                        Files.write(assetPath, stream.readAllBytes());
                    } catch (IOException exception) {
                        System.out.println("Failed to download asset " + path + " from " + assetUrl + "!");
                        failedAssets.add(asset);
                    }
                };

                executor.execute(copyHandler);
            }

            try {
                indexJson.add("objects", objectsJson);
                Files.writeString(indexesPath, TestGradlePlugin.GSON.toJson(indexJson));
            } catch (IOException exception) {
                throw new RuntimeException("Failed to write index json!", exception);
            }

            try {
                executor.shutdown();

                if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    throw new RuntimeException("Timed out while waiting for assets to download!");
                }
            } catch (InterruptedException exception) {
                throw new RuntimeException("Failed to wait for assets to download!", exception);
            }

            if (!failedAssets.isEmpty()) {
                StringBuilder errorMessage = new StringBuilder("Failed to download the following assets (Total: " + failedAssets.size() + "):\n");
                for (AssetObject asset : failedAssets) {
                    for (Map.Entry<String, AssetObject> entry : assets.entrySet()) {
                        if (entry.getValue().hash().equals(asset.hash())) {
                            errorMessage.append("Asset: ").append(entry.getKey()).append("\n");
                            break;
                        }
                    }
                }

                errorMessage.append("\nSome assets failed to download! See above for more details! Try running the task again!");
                throw new RuntimeException(errorMessage.toString());
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create assets directory!", exception);
        }
    }
}
