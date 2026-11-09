package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.HashingFunction;
import dev.turtywurty.testgradleplugin.util.Downloads;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipFile;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;

@DisableCachingByDefault(because = "Prepares locally cached Minecraft artifacts")
public abstract class ExtractServerTask extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getServerJar();
    @OutputFile
    public abstract RegularFileProperty getGameJar();
    @OutputDirectory
    public abstract DirectoryProperty getLibrariesDirectory();

    @TaskAction
    public void extract() throws IOException {
        extractBundle(getServerJar().get().getAsFile().toPath(), getGameJar().get().getAsFile().toPath(),
            getLibrariesDirectory().get().getAsFile().toPath());
    }

    public static void extractBundle(Path bundle, Path gameJar, Path libraries) throws IOException {
        Files.createDirectories(libraries);
        try (ZipFile zip = new ZipFile(bundle.toFile())) {
            extractList(zip, "versions", gameJar.getParent(), gameJar);
            extractList(zip, "libraries", libraries, null);
        }
    }

    private static void extractList(ZipFile zip, String kind, Path root, Path gameJar) throws IOException {
        ZipEntry list = zip.getEntry("META-INF/" + kind + ".list");
        if (list == null)
            throw new IOException("Missing bundler " + kind + ".list");

        String text;
        try (InputStream input = zip.getInputStream(list)) {
            text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<String> rows = text.lines().filter(line -> !line.isBlank()).toList();
        if (gameJar != null && rows.size() != 1)
            throw new IOException("Expected one bundled game jar");

        for (String row : rows) {
            String[] fields = row.split("\t");
            if (fields.length != 3)
                throw new IOException("Malformed bundler row: " + row);

            // Validate even the version entry path before looking it up.
            Downloads.resolveInside(root, fields[2]);
            Path target = gameJar == null ? Downloads.resolveInside(root, fields[2]) : gameJar;
            ZipEntry entry = zip.getEntry("META-INF/" + kind + "/" + fields[2]);
            if (entry == null)
                throw new IOException("Missing bundled entry: " + fields[2]);

            Files.createDirectories(target.getParent());
            try (InputStream input = zip.getInputStream(entry)) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }

            if (!HashingFunction.SHA256.hash(target).equalsIgnoreCase(fields[0])) {
                Files.delete(target);
                throw new IOException("Bundled entry checksum mismatch: " + fields[2]);
            }
        }
    }
}
