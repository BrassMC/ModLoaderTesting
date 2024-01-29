package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.piston.version.Download;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
public class DownloadClientMappingsTask extends DefaultTestGradleTask {
    @InputFile
    @Classpath
    private final Path versionJsonPath;

    @OutputFile
    private final Path clientMappingsPath, clientMappingsHashPath;

    public DownloadClientMappingsTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.versionJsonPath = versionPath.resolve("version.json");
        this.clientMappingsPath = versionPath.resolve("client_mappings.txt");
        this.clientMappingsHashPath = versionPath.resolve("client_mappings.txt.sha1");
    }

    @TaskAction
    public void downloadClientMappings() {
        VersionPackage versionPackage = VersionPackage.fromPath(versionJsonPath);
        System.out.println("Version package path: " + versionJsonPath);

        // Check if the client mappings hash is already downloaded
        if (Files.exists(clientMappingsHashPath) && Files.exists(clientMappingsPath)) {
            String hash = null;
            try {
                hash = Files.readString(clientMappingsHashPath);
            } catch (IOException ignored) {
            }

            if (hash != null) {
                String mappingsHash = versionPackage.downloads().client_mappings().sha1();
                System.out.println("Client mappings hash: " + mappingsHash);

                // Check if the client mappings hash matches the hash in the version manifest
                if (hash.equals(mappingsHash)) {
                    System.out.println("SKIPPING DOWNLOAD: Client mappings already downloaded!");
                    return;
                }
            }
        }

        try {
            Files.deleteIfExists(clientMappingsPath);
            Files.deleteIfExists(clientMappingsHashPath);

            System.out.println("Client mappings hash mismatch! Re-downloading...");

            Download clientMappingsDownload = versionPackage.downloads().client_mappings();
            System.out.println("Client mappings download: " + clientMappingsDownload.url());

            String clientMappingsHash = clientMappingsDownload.sha1();
            System.out.println("Client mappings hash: " + clientMappingsHash);
            Files.writeString(clientMappingsHashPath, clientMappingsHash);

            Path mappingsPath = clientMappingsDownload.downloadToPath(clientMappingsHashPath.getParent(), "client_mappings.txt");
            System.out.println("Client mappings downloaded to: " + mappingsPath);

            Files.move(mappingsPath, clientMappingsPath);

            System.out.println("Done!");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to download client mappings!", exception);
        }
    }

    public Path getVersionJsonPath() {
        return versionJsonPath;
    }

    public Path getClientMappingsPath() {
        return clientMappingsPath;
    }

    public Path getClientMappingsHashPath() {
        return clientMappingsHashPath;
    }
}
