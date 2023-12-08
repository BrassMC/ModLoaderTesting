package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@CacheableTask
public abstract class MergeTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<TestGradleExtension.Side> getSide();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void mergeJar() {
        System.out.println("Merging client and server!");

        Path versionPath = getOutputDir()
                .getOrElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .getAsFile()
                .toPath()
                .resolve(getVersion().get());

        TestGradleExtension.Side side = getSide().get();
        if(side != TestGradleExtension.Side.BOTH)
            return;

        Path clientDir = versionPath.resolve("client");
        Path serverDir = versionPath.resolve("server");
        if (Files.notExists(clientDir))
            throw new IllegalStateException("Client directory is missing, please run the extractClient task!");
        if (Files.notExists(serverDir))
            throw new IllegalStateException("Server directory is missing, please run the extractServer task!");

        Path joinedDir = versionPath.resolve("joined");
        if (Files.notExists(joinedDir)) {
            try {
                Files.createDirectories(joinedDir);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create joined directory!", exception);
            }
        } else {} // TODO: Clear and recreate the directory

        try (Stream<Path> clientFiles = Files.walk(clientDir)) {
            clientFiles.forEach(clientFile -> {
                if (Files.isDirectory(clientFile))
                    return;

                Path joinedFile = joinedDir.resolve(clientDir.relativize(clientFile));
                if (Files.exists(joinedFile))
                    return;

                try {
                    Files.createDirectories(joinedFile.getParent());
                    Files.copy(clientFile, joinedFile);
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to copy client file!", exception);
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to walk client directory!", exception);
        }

        try (Stream<Path> serverFiles = Files.walk(serverDir)) {
            serverFiles.forEach(serverFile -> {
                if (Files.isDirectory(serverFile))
                    return;

                Path joinedFile = joinedDir.resolve(serverDir.relativize(serverFile));
                if (Files.exists(joinedFile))
                    return;

                try {
                    Files.createDirectories(joinedFile.getParent());
                    Files.copy(serverFile, joinedFile);
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to copy server file!", exception);
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to walk server directory!", exception);
        }

        System.out.println("Finished merging jars!");
    }
}
