package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import dev.turtywurty.testgradleplugin.util.FileUtil;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@CacheableTask
public class MergeTask extends DefaultTestGradleTask {
    @InputDirectory
    @Classpath
    private final Path clientDir, serverDir;

    @OutputDirectory
    private final Path joinedDir;

    public MergeTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.clientDir = versionPath.resolve("client");
        this.serverDir = versionPath.resolve("server");
        this.joinedDir = versionPath.resolve("joined");
    }

    @TaskAction
    public void mergeJar() {
        System.out.println("Merging client and server!");

        TestGradleExtension.Side side = getSide();
        if (side != TestGradleExtension.Side.BOTH)
            return;

        if (Files.notExists(clientDir))
            throw new IllegalStateException("Client directory is missing, please run the extractClient task!");
        if (Files.notExists(serverDir))
            throw new IllegalStateException("Server directory is missing, please run the extractServer task!");

        try {
            if (Files.exists(joinedDir)) {
                FileUtil.deleteDirectory(joinedDir);
            }

            Files.createDirectories(joinedDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create joined directory!", exception);
        }

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

    public Path getClientDir() {
        return clientDir;
    }

    public Path getServerDir() {
        return serverDir;
    }

    public Path getJoinedDir() {
        return joinedDir;
    }
}
