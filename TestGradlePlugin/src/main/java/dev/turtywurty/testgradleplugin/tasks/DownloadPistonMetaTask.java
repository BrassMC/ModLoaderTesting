package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.piston.PistonMeta;
import dev.turtywurty.testgradleplugin.piston.PistonMetaVersion;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Path;

public abstract class DownloadPistonMetaTask extends DefaultTask {
    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getVersion();

    @TaskAction
    public void downloadPistonMeta() {
        Path outputDir = getOutputDir()
                .orElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .get()
                .getAsFile()
                .toPath();

        String version = getVersion().get();

        System.out.println("Downloading Piston Meta...");
        PistonMeta.download(outputDir);

        System.out.println("Loading Piston Meta...");
        var meta = new PistonMeta(outputDir.resolve("version_manifest.json"));

        System.out.println("Finding Piston Meta Version...");
        PistonMetaVersion metaVersion = meta.findVersion(version);

        System.out.println("Downloading Piston Meta Version...");
        VersionPackage.download(metaVersion, outputDir);
    }
}
