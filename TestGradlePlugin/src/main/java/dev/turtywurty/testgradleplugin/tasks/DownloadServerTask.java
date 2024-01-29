package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.piston.version.Download;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@CacheableTask
public class DownloadServerTask extends DefaultTestGradleTask {
    @InputFile
    @Classpath
    private final Path versionJsonPath;

    @OutputFile
    private final Path serverJarPath, serverHashPath;

    public DownloadServerTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.versionJsonPath = versionPath.resolve("version.json");
        this.serverJarPath = versionPath.resolve("server.jar");
        this.serverHashPath = versionPath.resolve("server.jar.sha1");
    }

    @TaskAction
    public void downloadServer() {
        VersionPackage versionPackage = VersionPackage.fromPath(versionJsonPath);
        System.out.println("Version package path: " + versionJsonPath);

        // Check if the server hash is already downloaded
        if (Files.exists(serverHashPath) && Files.exists(serverJarPath)) {
            String hash = null;
            try {
                hash = Files.readString(serverHashPath);
            } catch (IOException ignored) {
            }

            if (hash != null) {
                String jarHash = versionPackage.downloads().server().sha1();
                System.out.println("Server jar hash: " + jarHash);

                // Check if the server jar hash matches the hash in the version manifest
                if (Objects.equals(hash, jarHash)) {
                    System.out.println("SKIPPING DOWNLOAD: Server jar already downloaded!");
                    return;
                }
            }
        }

        try {
            Files.deleteIfExists(serverJarPath);
            Files.deleteIfExists(serverHashPath);

            System.out.println("Server jar hash mismatch! Re-downloading...");

            Download serverDownload = versionPackage.downloads().client();
            System.out.println("Server download: " + serverDownload.url());

            String serverHash = serverDownload.sha1();
            System.out.println("Server hash: " + serverHash);
            Files.writeString(serverHashPath, serverHash);

            Path jarPath = serverDownload.downloadToPath(serverJarPath.getParent(), "server.jar");
            System.out.println("Server jar downloaded to: " + jarPath);

            Files.move(jarPath, serverJarPath);

            System.out.println("Done!");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to download server jar!", exception);
        }
    }

    public Path getServerJarPath() {
        return serverJarPath;
    }

    public Path getServerHashPath() {
        return serverHashPath;
    }

    public Path getVersionJsonPath() {
        return versionJsonPath;
    }
}
