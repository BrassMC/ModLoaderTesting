package dev.turtywurty.testgradleplugin.tasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // add client jar to classpath
        getProject().getDependencies().add("implementation", getProject().files(clientJarPath));

        Path librariesJsonPath = versionFolder.resolve("libraries.json");
        if (Files.notExists(librariesJsonPath)) {
            throw new RuntimeException("Libraries json does not exist!");
        }

        Map<String, Path> libraryJars = new HashMap<>();
        try {
            String librariesJson = Files.readString(librariesJsonPath);
            JsonObject librariesObject = TestGradlePlugin.GSON.fromJson(librariesJson, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : librariesObject.entrySet()) {
                String name = entry.getKey();
                String path = entry.getValue().getAsString();
                libraryJars.put(name, Path.of(path));
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read libraries json!", exception);
        }

        // add libraries to classpath
        for (Path path : libraryJars.values()) {
            getProject().getDependencies().add("implementation", getProject().files(path));
        }

        Path runDir = getRunDir().get().getAsFile().toPath();
        if (Files.notExists(runDir)) {
            throw new RuntimeException("Run directory does not exist!");
        }

        getProject().javaexec(javaExecSpec -> {
            javaExecSpec.getMainClass().set(versionPackage.mainClass());
            javaExecSpec.setWorkingDir(runDir.toFile());

            for (Path path : libraryJars.values()) {
                System.out.println("Library: " + path);
            }

            List<Path> classpathJars = new ArrayList<>(libraryJars.values());
            classpathJars.add(clientJarPath);
            javaExecSpec.setClasspath(getProject().files(classpathJars));
            javaExecSpec.setArgs(List.of(
                    "--accessToken", "****",
                    "--version", getVersion().get(),
                    "--assetIndex", versionPackage.assetIndex().id(),
                    "--assetsDir", versionFolder.resolve("assets").toAbsolutePath().toString(),
                    "--userProperties", "{}"));
        });
    }
}
