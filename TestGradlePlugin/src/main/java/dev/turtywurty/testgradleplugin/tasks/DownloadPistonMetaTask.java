package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.piston.PistonMeta;
import dev.turtywurty.testgradleplugin.piston.PistonMetaVersion;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Path;

@CacheableTask
public class DownloadPistonMetaTask extends DefaultTestGradleTask {
    @OutputFile
    private final Path pistonFile, versionManifestFile;

    public DownloadPistonMetaTask() {
        Path cacheDir = getCacheDir();
        this.pistonFile = cacheDir.resolve("version_manifest.json");
        this.versionManifestFile = cacheDir.resolve(getMinecraftVersion()).resolve("version.json");
    }

    @TaskAction
    public void downloadPistonMeta() {
        String version = getMinecraftVersion();

        System.out.println("Downloading Piston Meta...");
        PistonMeta.download(this.pistonFile);

        var meta = new PistonMeta(this.pistonFile);

        PistonMetaVersion metaVersion = meta.findVersion(version);

        System.out.println("Downloading Piston Meta Version...");
        VersionPackage.download(metaVersion, this.versionManifestFile);
    }

    public Path getPistonFile() {
        return pistonFile;
    }

    public Path getVersionManifestFile() {
        return versionManifestFile;
    }
}
