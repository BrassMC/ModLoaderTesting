package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class RunClientTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @OutputDirectory
    public abstract DirectoryProperty getRunDir();

    @TaskAction
    public void downloadClient() {
        Path versionFolder = getOutputDir()
                .orElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .get()
                .getAsFile()
                .toPath()
                .resolve(getVersion().get());

        Path versionJsonPath = versionFolder.resolve("version.json");
        if (Files.notExists(versionJsonPath)) {
            throw new RuntimeException("Version json does not exist!");
        }

        VersionPackage versionPackage = VersionPackage.fromPath(versionJsonPath);

        Path clientJarPath = versionFolder.resolve("client.jar");
        if (Files.notExists(clientJarPath)) {
            throw new RuntimeException("Client jar does not exist!");
        }

        Path clientMappingsPath = versionFolder.resolve("client-mappings.jar");
        if (Files.notExists(clientMappingsPath)) {
            throw new RuntimeException("Client mappings jar does not exist!");
        }

        Path runDir = getRunDir().get().getAsFile().toPath();
        if (Files.notExists(runDir)) {
            throw new RuntimeException("Run directory does not exist!");
        }

        getProject().javaexec(javaExecSpec -> {
            javaExecSpec.getMainClass().set(versionPackage.mainClass());
            javaExecSpec.setClasspath(getProject().files(clientJarPath, clientMappingsPath));
            javaExecSpec.setWorkingDir(runDir.toFile());
            // TODO: Set game args
            // TODO: Set jvm args
        });
    }
}
