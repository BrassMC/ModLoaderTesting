package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import dev.turtywurty.testgradleplugin.piston.PistonMeta;
import dev.turtywurty.testgradleplugin.piston.PistonMetaVersion;
import dev.turtywurty.testgradleplugin.piston.version.Download;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

public abstract class DownloadClientTask extends DefaultTask {
    @Input
    public abstract String getVersion();

    @Input
    public abstract String getOutput();

    @TaskAction
    public void downloadClient() {
        System.out.println("Downloading client for version: " + getVersion() + "...");

        PistonMeta versionMeta = PistonMeta.SELF;
        PistonMetaVersion version = versionMeta.findVersion(getVersion());
        if (version == null) {
            System.out.println("Version not found!");
            return;
        }

        System.out.println("Version found!");

        VersionPackage versionPackage = downloadVersionPackage(version);
        System.out.println("Version package downloaded!");
        System.out.println("Version package: " + versionPackage);

        Download clientDownload = versionPackage.downloads().client();
        System.out.println("Client download: " + clientDownload);

        clientDownload.downloadToPath(Path.of(getOutput()));
        System.out.println("Client downloaded!");
        System.out.println("Client path: " + new File(getOutput()).getAbsolutePath());

        System.out.println("Done!");
    }

    private static VersionPackage downloadVersionPackage(PistonMetaVersion version) {
        String url = version.url();
        System.out.println("URL: " + url);

        VersionPackage versionPackage;
        try(InputStream inputStream = new URL(url).openStream()) {
            String jsonStr = new String(inputStream.readAllBytes());
            JsonObject json = TestGradlePlugin.GSON.fromJson(jsonStr, JsonObject.class);
            versionPackage = VersionPackage.fromJson(json);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to download client!", exception);
        }

        return versionPackage;
    }
}
