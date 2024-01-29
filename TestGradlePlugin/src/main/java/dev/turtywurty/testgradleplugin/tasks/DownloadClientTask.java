package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.piston.version.Download;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@CacheableTask
public class DownloadClientTask extends DefaultTestGradleTask {
    @InputFile
    @Classpath
    private final Path versionJsonPath;

    @OutputFile
    private final Path clientJarPath, clientHashPath;

    public DownloadClientTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.versionJsonPath = versionPath.resolve("version.json");
        this.clientJarPath = versionPath.resolve("client.jar");
        this.clientHashPath = versionPath.resolve("client.jar.sha1");
    }

    @TaskAction
    public void downloadClient() {
        VersionPackage versionPackage = VersionPackage.fromPath(versionJsonPath);
        System.out.println("Version package path: " + versionJsonPath);

        // Check if the client hash is already downloaded
        if (Files.exists(clientHashPath) && Files.exists(clientJarPath)) {
            String hash = null;
            try {
                hash = Files.readString(clientHashPath);
            } catch (IOException ignored) {
            }

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

            Path jarPath = clientDownload.downloadToPath(clientJarPath.getParent(), "client.jar");
            System.out.println("Client jar downloaded to: " + jarPath);

            Files.move(jarPath, clientJarPath);

            System.out.println("Done!");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to download client jar!", exception);
        }
    }

    public Path getVersionJsonPath() {
        return versionJsonPath;
    }

    public Path getClientJarPath() {
        return clientJarPath;
    }

    public Path getClientHashPath() {
        return clientHashPath;
    }
}
