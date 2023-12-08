package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

@CacheableTask
public abstract class RecompileTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<TestGradleExtension.Side> getSide();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void recompile() {
        Path versionPath = getOutputDir()
                .orElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .get()
                .getAsFile()
                .toPath()
                .resolve(getVersion().get());

        TestGradleExtension.Side side = getSide().get();
        Path location = switch (side) {
            case CLIENT -> versionPath.resolve("client");
            case SERVER -> versionPath.resolve("server");
            case BOTH -> versionPath.resolve("joined");
        };

        if (Files.notExists(location))
            throw new IllegalStateException("The " + side.name().toLowerCase() + " has not been extracted yet!");

        System.out.println("Recompiling " + side.name().toLowerCase() + " for version " + getVersion().get());

        Path targetJar = versionPath.resolve("recomp_" + switch (side) {
            case CLIENT -> "client";
            case SERVER -> "server";
            case BOTH -> "joined";
        } + ".jar");

        try {
            Files.deleteIfExists(targetJar);
            Files.createDirectories(targetJar.getParent());
            Files.createFile(targetJar);

            try (var fos = new FileOutputStream(targetJar.toFile()); var jos = new JarOutputStream(fos);
                 Stream<Path> walk = Files.walk(location)) {
                walk.filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                var entry = new JarEntry(location.relativize(path).toString());
                                jos.putNextEntry(entry);
                                jos.write(Files.readAllBytes(path));
                                jos.closeEntry();

                                System.out.println("Added " + entry.getName() + " to " + targetJar.getFileName());
                            } catch (IOException exception) {
                                throw new IllegalStateException("Failed to add " + path.getFileName() + " to " + targetJar.getFileName(), exception);
                            }
                        });
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete old jar!", exception);
        }

        System.out.println("Successfully recompiled " + side.name().toLowerCase() + " for version " + getVersion().get());

        // add the recompiled jar to the classpath
        getProject().getRepositories().flatDir(repo -> repo.dir(targetJar.getParent()));
        getProject().getDependencies().add("implementation", getProject().files(targetJar));
    }
}
