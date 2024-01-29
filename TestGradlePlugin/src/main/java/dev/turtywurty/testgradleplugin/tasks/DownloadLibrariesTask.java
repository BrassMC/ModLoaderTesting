package dev.turtywurty.testgradleplugin.tasks;

import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.HashingFunction;
import dev.turtywurty.testgradleplugin.OperatingSystem;
import dev.turtywurty.testgradleplugin.piston.version.Download;
import dev.turtywurty.testgradleplugin.piston.version.Library;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// TODO: Cache this task by comparing the hashes of the libraries
@CacheableTask
public class DownloadLibrariesTask extends DefaultTestGradleTask {
    @InputFile
    @Classpath
    private final Path versionJsonPath;

    @OutputDirectory
    private final Path librariesPath;

    @OutputFile
    private final Path librariesJsonPath;

    public DownloadLibrariesTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.versionJsonPath = versionPath.resolve("version.json");
        this.librariesPath = versionPath.resolve("libraries");
        this.librariesJsonPath = versionPath.resolve("libraries.json");
    }

    private static @NotNull StringBuilder getNormalizedPath(String[] split) {
        var pathBuilder = new StringBuilder();
        for (int index = 0; index < split.length - 1; index++) {
            String string = split[index];

            // check if the string is the version
            String[] slashSplit = string.split("/");
            if (slashSplit.length == 1) {
                pathBuilder.append(string);
            } else {
                // replace all . with /
                pathBuilder.append(string.replace(".", "/"));
            }

            pathBuilder.append("/");
        }

        return pathBuilder;
    }

    @TaskAction
    public void downloadLibraries() {
        System.out.println("Downloading libraries for version " + getMinecraftVersion() + "...");

        VersionPackage versionPackage = VersionPackage.fromPath(versionJsonPath);
        System.out.println("Version package path: " + versionJsonPath);

        List<Library> libraries = versionPackage.libraries();
        System.out.println("Libraries: " + libraries.size());

        Path minecraftLibrariesPath = OperatingSystem.getMinecraftDir().resolve("libraries");
        if (Files.notExists(minecraftLibrariesPath)) {
            throw new RuntimeException("You need a version of Minecraft installed to download libraries!");
        }

        System.out.println("Minecraft libraries path: " + minecraftLibrariesPath);

        Map<String, Path> libraryJars = new HashMap<>();
        for (Library library : libraries) {
            java.util.Optional<List<Library.DownloadRule>> rules = library.rules();
            if (rules.isPresent()) {
                List<Library.DownloadRule> downloadRules = rules.get();
                for (Library.DownloadRule rule : downloadRules) {
                    Library.DownloadRule.Action action = rule.action();
                    String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
                    String ruleOsName = rule.os().name().toLowerCase(Locale.ROOT);
                    if (ruleOsName.equals(osName)) {
                        if (action == Library.DownloadRule.Action.ALLOW) {
                            System.out.println("Allowed to download library " + library.name());
                        } else if (action == Library.DownloadRule.Action.DISALLOW) {
                            System.out.println("Disallowed to download library " + library.name());
                        }
                    }
                }
            }

            System.out.println("Library: " + library.name());
            Download artifact = library.artifact();

            // from: org.slf4j:slf4j-api:2.0.7
            // to: org/slf4j/slf4j-api/2.0.7/slf4j-api-2.0.7.jar
            String[] split = artifact.url()
                    .replace("https://libraries.minecraft.net/", "")
                    .split("/");
            String fileName = split[split.length - 1];

            StringBuilder pathBuilder = getNormalizedPath(split);

            Path libraryPath = librariesPath.resolve(pathBuilder.toString());
            Path libraryFile = libraryPath.resolve(fileName);
            System.out.println("Library path: " + libraryFile);

            if (Files.exists(libraryFile) && HashingFunction.SHA1.hash(libraryFile).equals(artifact.sha1())) {
                System.out.println("Skipping library: " + library.name());
                libraryJars.put(library.name(), libraryFile);
                continue;
            }

            Path minecraftLibraryPath = minecraftLibrariesPath.resolve(pathBuilder.toString()).resolve(fileName);
            if (Files.exists(minecraftLibraryPath) && HashingFunction.SHA1.hash(minecraftLibraryPath).equals(artifact.sha1())) {
                try {
                    Files.createDirectories(libraryPath);
                    Files.copy(minecraftLibraryPath, libraryFile);
                } catch (IOException exception) {
                    throw new RuntimeException("Failed to copy library " + library.name() + "!", exception);
                }

                libraryJars.put(library.name(), libraryFile);
                continue;
            }

            Path downloadPath = artifact.downloadToPath(libraryPath, fileName);
            System.out.println("Downloaded to: " + downloadPath);

            libraryJars.put(library.name(), downloadPath);
        }

        System.out.println("Downloaded " + libraryJars.size() + " libraries!");

        JsonObject librariesObject = new JsonObject();
        libraryJars.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(entry ->
                        librariesObject.addProperty(entry.getKey(), entry.getValue().toString()));

        try {
            Files.createDirectories(librariesJsonPath.getParent());
            Files.writeString(
                    librariesJsonPath,
                    librariesObject.toString(),
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to write libraries.json!", exception);
        }
    }

    public Path getVersionJsonPath() {
        return versionJsonPath;
    }

    public Path getLibrariesPath() {
        return librariesPath;
    }

    public Path getLibrariesJsonPath() {
        return librariesJsonPath;
    }
}
