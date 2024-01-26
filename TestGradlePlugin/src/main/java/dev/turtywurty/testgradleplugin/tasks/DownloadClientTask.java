package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.piston.version.Download;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@CacheableTask
public abstract class DownloadClientTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void downloadClient() {
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

        Path clientJarPath = versionPath.resolve("client.jar");
        Path clientHashPath = versionPath.resolve("client.jar.sha1");
        // Check if the client hash is already downloaded
        if (Files.exists(clientHashPath) && Files.exists(clientJarPath)) {
            String hash = null;
            try {
                hash = Files.readString(clientHashPath);
            } catch (IOException ignored) {}

            if (hash != null) {
                String jarHash = versionPackage.downloads().client().sha1();
                System.out.println("Client jar hash: " + jarHash);

                // Check if the client jar hash matches the hash in the version manifest
                if (Objects.equals(hash, jarHash)) {
                    System.out.println("SKIPPING DOWNLOAD: Client jar already downloaded!");
                    return;
                }
            }
        }

        try {
            Files.deleteIfExists(clientJarPath);
            Files.deleteIfExists(clientHashPath);

            System.out.println("Client jar hash mismatch! Re-downloading...");

            Download clientDownload = versionPackage.downloads().client();
            System.out.println("Client download: " + clientDownload.url());

            String clientHash = clientDownload.sha1();
            System.out.println("Client hash: " + clientHash);
            Files.writeString(clientHashPath, clientHash);

            Path jarPath = clientDownload.downloadToPath(versionPath);
            System.out.println("Client jar downloaded to: " + jarPath);

            Files.move(jarPath, clientJarPath);

            System.out.println("Done!");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to download client jar!", exception);
        }
    }
}
