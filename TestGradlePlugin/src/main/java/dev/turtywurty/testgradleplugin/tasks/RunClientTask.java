package dev.turtywurty.testgradleplugin.tasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunClientTask extends DefaultTestGradleTask {
    @InputFile
    @Classpath
    private final Path versionJsonPath, librariesJsonPath, clientJarPath;

    @InputDirectory
    @Classpath
    private final Path assetsDir;

    @OutputDirectory
    private final Path runDir;

    public RunClientTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.versionJsonPath = versionPath.resolve("version.json");
        this.librariesJsonPath = versionPath.resolve("libraries.json");
        this.clientJarPath = versionPath.resolve("client.jar");

        Path projectDir = getProject().getProjectDir().toPath();
        this.assetsDir = projectDir.resolve("assets");
        this.runDir = projectDir.resolve("run");
    }

    public static void readLibraries(Map<String, Path> libraryJars, Path librariesJsonPath) throws IOException {
        String librariesJson = Files.readString(librariesJsonPath);
        JsonObject librariesObject = TestGradlePlugin.GSON.fromJson(librariesJson, JsonObject.class);
        for (Map.Entry<String, JsonElement> entry : librariesObject.entrySet()) {
            String name = entry.getKey();
            String path = entry.getValue().getAsString();
            libraryJars.put(name, Path.of(path));
        }
    }

    @TaskAction
    public void downloadClient() {
        if (Files.notExists(versionJsonPath))
            throw new RuntimeException("Version json does not exist!");

        VersionPackage versionPackage = VersionPackage.fromPath(versionJsonPath);

        if (Files.notExists(clientJarPath))
            throw new RuntimeException("Client jar does not exist!");

        // add client jar to classpath
        getProject().getDependencies().add("implementation", getProject().files(clientJarPath));

        if (Files.notExists(librariesJsonPath))
            throw new RuntimeException("Libraries json does not exist!");

        Map<String, Path> libraryJars = new HashMap<>();
        try {
            readLibraries(libraryJars, librariesJsonPath);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read libraries json!", exception);
        }

        // add libraries to classpath
        for (Path path : libraryJars.values()) {
            getProject().getDependencies().add("implementation", getProject().files(path));
        }

        if (Files.notExists(runDir)) {
            try {
                Files.createDirectories(runDir);
            } catch (IOException exception) {
                throw new RuntimeException("Failed to create run directory!", exception);
            }
        }

        if (Files.notExists(assetsDir))
            System.err.println("Assets directory does not exist!");

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
                    "--version", getMinecraftVersion(),
                    "--assetIndex", versionPackage.assetIndex().id(),
                    "--assetsDir", assetsDir.toAbsolutePath().toString(),
                    "--userProperties", "{}"));
        });
    }

    public Path getVersionJsonPath() {
        return versionJsonPath;
    }

    public Path getLibrariesJsonPath() {
        return librariesJsonPath;
    }

    public Path getClientJarPath() {
        return clientJarPath;
    }

    public Path getAssetsDir() {
        return assetsDir;
    }

    public Path getRunDir() {
        return runDir;
    }
}
