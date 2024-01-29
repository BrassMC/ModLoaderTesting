package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.util.FileUtil;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@DisableCachingByDefault(because = "It's unnecessary")
public class ExtractClientTask extends DefaultTestGradleTask {
    @InputFile
    private final Path jarPath;

    private final Path outputDir;

    public ExtractClientTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.jarPath = versionPath.resolve("client.jar");
        this.outputDir = versionPath.resolve("client");
    }

    @TaskAction
    public void extractJar() {
        if (Files.notExists(jarPath))
            throw new IllegalStateException("Client jar does not exist!");

        try {
            if (Files.notExists(outputDir))
                Files.createDirectories(outputDir);
            else
                FileUtil.deleteDirectory(outputDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create output directory!", exception);
        }

        long start = System.nanoTime();
        FileUtil.extractArchive(getProject(), jarPath, outputDir);
        System.out.println("Extracted client jar in " + (System.nanoTime() - start) / 1_000_000_000 + " seconds!");
    }

    public Path getJarPath() {
        return jarPath;
    }
}
