package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.HashingFunction;
import dev.turtywurty.testgradleplugin.OperatingSystem;
import dev.turtywurty.testgradleplugin.piston.version.Download;
import dev.turtywurty.testgradleplugin.piston.version.Library;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@CacheableTask
public abstract class DownloadLibrariesTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void downloadLibraries() {
        System.out.println("Downloading libraries for version " + getVersion().get());

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

        List<Library> libraries = versionPackage.libraries();
        System.out.println("Libraries: " + libraries.size());

        Path minecraftLibrariesPath = OperatingSystem.getMinecraftDir().resolve("libraries");
        if(Files.notExists(minecraftLibrariesPath)) {
            throw new RuntimeException("You need a version of Minecraft installed to download libraries!");
        }

        System.out.println("Minecraft libraries path: " + minecraftLibrariesPath);

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

            Path libraryPath = versionPath.resolve("libraries").resolve(pathBuilder.toString());
            Path libraryFile = libraryPath.resolve(fileName);
            System.out.println("Library path: " + libraryFile);

            if(Files.exists(libraryFile) && HashingFunction.SHA1.hash(libraryFile).equals(artifact.sha1())) {
                System.out.println("Skipping library: " + library.name());
                continue;
            }

            Path minecraftLibraryPath = minecraftLibrariesPath.resolve(pathBuilder.toString()).resolve(fileName);
            if(Files.exists(minecraftLibraryPath) && HashingFunction.SHA1.hash(minecraftLibraryPath).equals(artifact.sha1())) {
                try {
                    Files.createDirectories(libraryPath);
                    Files.copy(minecraftLibraryPath, libraryFile);
                } catch (IOException exception) {
                    throw new RuntimeException("Failed to copy library " + library.name() + "!", exception);
                }

                continue;
            }

            Path downloadPath = artifact.downloadToPath(libraryPath, fileName);
            System.out.println("Downloaded to: " + downloadPath);
        }
    }

    private static @NotNull StringBuilder getNormalizedPath(String[] split) {
        var pathBuilder = new StringBuilder();
        for (int index = 0; index < split.length - 1; index++) {
            String string = split[index];

            // check if the string is the version
            String[] slashSplit = string.split("/");
            if(slashSplit.length == 1) {
                pathBuilder.append(string);
            } else {
                // replace all . with /
                pathBuilder.append(string.replace(".", "/"));
            }

            pathBuilder.append("/");
        }

        return pathBuilder;
    }
}
