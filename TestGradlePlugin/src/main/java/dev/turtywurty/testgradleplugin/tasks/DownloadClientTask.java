package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import dev.turtywurty.testgradleplugin.util.Downloads;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;

@DisableCachingByDefault(because = "Prepares locally cached Minecraft artifacts")
public abstract class DownloadClientTask extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getVersionJson();
    @OutputFile
    public abstract RegularFileProperty getClientJar();

    @TaskAction
    public void download() throws IOException {
        var version = VersionPackage.fromPath(getVersionJson().get().getAsFile().toPath());
        Downloads.fetch(version.downloads().client(), getClientJar().get().getAsFile().toPath());
    }
}
