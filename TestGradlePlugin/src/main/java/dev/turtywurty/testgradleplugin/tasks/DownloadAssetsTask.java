package dev.turtywurty.testgradleplugin.tasks;

import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.OperatingSystem;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import dev.turtywurty.testgradleplugin.piston.version.Download;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import dev.turtywurty.testgradleplugin.util.Downloads;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.google.gson.JsonElement;
import dev.turtywurty.testgradleplugin.piston.version.AssetIndex;
import java.util.List;
import java.util.Map;
import java.util.Set;

@DisableCachingByDefault(because = "Prepares locally cached Minecraft artifacts")
public abstract class DownloadAssetsTask extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getVersionJson();
    @OutputDirectory
    public abstract DirectoryProperty getAssetsDirectory();
    @Input
    public abstract Property<String> getAssetsUrl();

    @TaskAction
    public void download() throws Exception {
        var version = VersionPackage.fromPath(getVersionJson().get().getAsFile().toPath());
        AssetIndex index = version.assetIndex();
        Path root = getAssetsDirectory().get().getAsFile().toPath();
        Path indexFile = root.resolve("indexes/" + index.id() + ".json");
        Downloads.fetch(new Download(index.sha1(), index.size(), index.url()), indexFile);
        JsonObject objects = TestGradlePlugin.GSON.fromJson(Files.readString(indexFile), JsonObject.class)
            .getAsJsonObject("objects");
        Set<String> hashes = new HashSet<>();
        List<Future<?>> futures = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
                JsonObject object = entry.getValue().getAsJsonObject();
                String hash = object.get("hash").getAsString();
                if (!hashes.add(hash))
                    continue;

                String relative = hash.substring(0, 2) + "/" + hash;
                var download = new Download(hash, object.get("size").getAsLong(), getAssetsUrl().get() + relative);
                futures.add(executor.submit(() -> {
                    Path output = root.resolve("objects").resolve(relative);
                    Path installed = OperatingSystem.getMinecraftDir().resolve("assets/objects").resolve(relative);
                    try {
                        if (!Downloads.valid(output, download) && Downloads.valid(installed, download)) {
                            Files.createDirectories(output.getParent());
                            Files.copy(installed, output, StandardCopyOption.REPLACE_EXISTING);
                        }

                        Downloads.fetch(download, output);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to prepare asset " + hash, exception);
                    }
                }));
            }

            for (Future<?> future : futures) {
                future.get();
            }
        }
    }
}
