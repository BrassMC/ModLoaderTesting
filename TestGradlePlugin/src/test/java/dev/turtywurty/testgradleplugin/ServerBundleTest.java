package dev.turtywurty.testgradleplugin;

import dev.turtywurty.testgradleplugin.tasks.ExtractServerTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ServerBundleTest {
    @TempDir
    private Path directory;

    @Test
    public void extractsPathsFromListsAndVerifiesHashes() throws IOException {
        byte[] game = "inner-game".getBytes(StandardCharsets.UTF_8);
        byte[] library = "library".getBytes(StandardCharsets.UTF_8);
        Path bundle = directory.resolve("bundle.jar");
        try (var zip = new ZipOutputStream(Files.newOutputStream(bundle))) {
            entry(zip, "META-INF/versions.list", HashingFunction.SHA256.hash(game) + "\t26.3\tunusual/game.jar\n");
            entry(zip, "META-INF/libraries.list",
                HashingFunction.SHA256.hash(library) + "\tgroup:lib:1\tgroup/lib.jar\n");
            entry(zip, "META-INF/versions/unusual/game.jar", game);
            entry(zip, "META-INF/libraries/group/lib.jar", library);
        }

        Path output = directory.resolve("server/game.jar");
        Path libraries = directory.resolve("server/libraries");
        ExtractServerTask.extractBundle(bundle, output, libraries);
        assertArrayEquals(game, Files.readAllBytes(output));
        assertArrayEquals(library, Files.readAllBytes(libraries.resolve("group/lib.jar")));
    }

    @Test
    public void rejectsEscapingBundlerPaths() throws IOException {
        Path bundle = directory.resolve("bad.jar");
        try (var zip = new ZipOutputStream(Files.newOutputStream(bundle))) {
            entry(zip, "META-INF/versions.list", "hash\t26.3\t../../escape.jar\n");
        }

        assertThrows(IllegalArgumentException.class, () -> ExtractServerTask.extractBundle(
            bundle, directory.resolve("server/game.jar"), directory.resolve("server/libraries")));
        assertFalse(Files.exists(directory.resolve("escape.jar")));
    }

    private static void entry(ZipOutputStream zip, String name, String value) throws IOException {
        entry(zip, name, value.getBytes(StandardCharsets.UTF_8));
    }

    private static void entry(ZipOutputStream zip, String name, byte[] value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value);
        zip.closeEntry();
    }
}
