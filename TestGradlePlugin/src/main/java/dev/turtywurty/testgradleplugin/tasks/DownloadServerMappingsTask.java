package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.piston.version.Download;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
public class DownloadServerMappingsTask extends DefaultTestGradleTask {
    @InputFile
    @Classpath
    private final Path versionJsonPath;

    @OutputFile
    private final Path serverMappingsPath, serverMappingsHashPath;

    public DownloadServerMappingsTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.versionJsonPath = versionPath.resolve("version.json");
        this.serverMappingsPath = versionPath.resolve("server_mappings.txt");
        this.serverMappingsHashPath = versionPath.resolve("server_mappings.txt.sha1");
    }

    @TaskAction
    public void downloadServerMappings() {
        VersionPackage versionPackage = VersionPackage.fromPath(versionJsonPath);
        System.out.println("Version package path: " + versionJsonPath);

        // Check if the server mappings hash is already downloaded
        if (Files.exists(serverMappingsHashPath) && Files.exists(serverMappingsPath)) {
            String hash = null;
            try {
                hash = Files.readString(serverMappingsHashPath);
            } catch (IOException ignored) {
            }

            if (hash != null) {
                String mappingsHash = versionPackage.downloads().server_mappings().sha1();
                System.out.println("Server mappings hash: " + mappingsHash);

                // Check if the server mappings hash matches the hash in the version manifest
                if (hash.equals(mappingsHash)) {
                    System.out.println("SKIPPING DOWNLOAD: Server mappings already downloaded!");
                    return;
                }
            }
        }

        try {
            Files.deleteIfExists(serverMappingsPath);
            Files.deleteIfExists(serverMappingsHashPath);

            System.out.println("Server mappings hash mismatch! Re-downloading...");

            Download serverMappingsDownload = versionPackage.downloads().server_mappings();
            System.out.println("Server mappings download: " + serverMappingsDownload.url());

            String serverMappingsHash = serverMappingsDownload.sha1();
            System.out.println("Server mappings hash: " + serverMappingsHash);
            Files.writeString(serverMappingsHashPath, serverMappingsHash);

            Path mappingsPath = serverMappingsDownload.downloadToPath(serverMappingsHashPath.getParent(), "server_mappings.txt");
            System.out.println("Server mappings downloaded to: " + mappingsPath);

            Files.move(mappingsPath, serverMappingsPath);

            System.out.println("Done!");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to download server mappings!", exception);
        }
    }

    public Path getServerMappingsPath() {
        return serverMappingsPath;
    }

    public Path getServerMappingsHashPath() {
        return serverMappingsHashPath;
    }

    public Path getVersionJsonPath() {
        return versionJsonPath;
    }
}
