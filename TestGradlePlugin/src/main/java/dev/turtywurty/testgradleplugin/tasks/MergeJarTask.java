package dev.turtywurty.testgradleplugin.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class MergeJarTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void mergeJar() {
        System.out.println("Merging jars!");

        Path versionPath = getOutputDir()
                .getOrElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .getAsFile()
                .toPath()
                .resolve(getVersion().get());

        Path clientJarPath = versionPath.resolve("client.jar");
        if (Files.notExists(clientJarPath)) {
            throw new RuntimeException("client.jar is missing, please run the downloadClient task!");
        }

        Path serverJarPath = versionPath.resolve("server.jar");
        if (Files.notExists(serverJarPath)) {
            throw new RuntimeException("server.jar is missing, please run the downloadServer task!");
        }

        Path joinedJarPath = versionPath.resolve("joined.jar");
        try {
            Files.createDirectories(joinedJarPath.getParent());
            Files.deleteIfExists(joinedJarPath);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create directories!", exception);
        }

        try (ZipFile clientJar = new ZipFile(clientJarPath.toFile());
             ZipFile serverJar = new ZipFile(serverJarPath.toFile())) {
            List<? extends ZipEntry> clientEntries = Collections.list(clientJar.entries());
            List<? extends ZipEntry> serverEntries = Collections.list(serverJar.entries());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                for (ZipEntry entry : clientEntries) {
                    System.out.println("Adding: " + entry.getName() + " from client.jar");
                    zipOutputStream.putNextEntry(entry);
                    if(!entry.isDirectory()) {
                        clientJar.getInputStream(entry).transferTo(zipOutputStream);
                    }

                    zipOutputStream.closeEntry();
                }

                for (ZipEntry entry : serverEntries) {
                    // check if it has already been added first
                    if (clientEntries.stream().anyMatch(zipEntry -> zipEntry.getName().equals(entry.getName()))) {
                        continue;
                    }

                    System.out.println("Adding: " + entry.getName() + " from server.jar");
                    zipOutputStream.putNextEntry(entry);
                    if(!entry.isDirectory()) {
                        serverJar.getInputStream(entry).transferTo(zipOutputStream);
                    }

                    zipOutputStream.closeEntry();
                }

                zipOutputStream.flush();
            }

            Files.write(joinedJarPath, outputStream.toByteArray());
        } catch (IOException exception) {
            throw new RuntimeException("Failed to merge jars!", exception);
        }
    }
}
