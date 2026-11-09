package dev.turtywurty.testgradleplugin.tasks;

import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.OperatingSystem;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import dev.turtywurty.testgradleplugin.util.Downloads;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import dev.turtywurty.testgradleplugin.piston.version.Library;

@DisableCachingByDefault(because = "Prepares locally cached Minecraft artifacts")
public abstract class DownloadLibrariesTask extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getVersionJson();
    @OutputDirectory
    public abstract DirectoryProperty getLibrariesDirectory();
    @OutputFile
    public abstract RegularFileProperty getLibrariesJson();
    @Input
    public abstract Property<String> getPlatform();

    @TaskAction
    public void download() throws IOException {
        var version = VersionPackage.fromPath(getVersionJson().get().getAsFile().toPath());
        Path root = getLibrariesDirectory().get().getAsFile().toPath();
        var index = new JsonObject();
        for (Library library : version.libraries()) {
            if (!library.allowed())
                continue;

            Path output = Downloads.resolveInside(root, library.path());
            Path installed = OperatingSystem.getMinecraftDir().resolve("libraries").resolve(library.path());
            if (!Downloads.valid(output, library.artifact()) && Downloads.valid(installed, library.artifact())) {
                Files.createDirectories(output.getParent());
                Files.copy(installed, output, StandardCopyOption.REPLACE_EXISTING);
            }

            Downloads.fetch(library.artifact(), output);
            index.addProperty(library.name(), library.path());
        }

        Path indexPath = getLibrariesJson().get().getAsFile().toPath();
        Files.createDirectories(indexPath.getParent());
        Files.writeString(indexPath, TestGradlePlugin.GSON.toJson(index));
    }
}
