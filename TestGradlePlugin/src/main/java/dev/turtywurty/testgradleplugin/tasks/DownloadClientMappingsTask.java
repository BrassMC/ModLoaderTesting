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

@CacheableTask
public abstract class DownloadClientMappingsTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void downloadClientMappings() {
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

        Path clientMappingsPath = versionPath.resolve("client_mappings.txt");
        Path clientMappingsHashPath = versionPath.resolve("client_mappings.txt.sha1");
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

            Path mappingsPath = clientMappingsDownload.downloadToPath(versionPath);
            System.out.println("Client mappings downloaded to: " + mappingsPath);

            Files.move(mappingsPath, clientMappingsPath);

            System.out.println("Done!");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to download client mappings!", exception);
        }
    }
}
