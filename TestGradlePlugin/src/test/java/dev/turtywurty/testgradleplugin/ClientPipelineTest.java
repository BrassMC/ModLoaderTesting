package dev.turtywurty.testgradleplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.*;

public class ClientPipelineTest {
    @TempDir
    private Path directory;

    @Test
    public void buildsAndLaunchesFromFreshTaskBackedArtifactsWithoutRemappingOrDecompilation() throws IOException {
        Path game = jar("game", "fixture.Game",
            """
                package fixture;
                import java.util.List;
                public class Game {
                    public static void main(String[] args) throws ClassNotFoundException {
                        if (!"true".equals(System.getProperty("fixture"))) throw new AssertionError("JVM args");
                        if (System.getProperty("testmodloader.minecraftMainClass") != null) throw new AssertionError("loader property");
                        if (!List.of("--version", "26.3").equals(List.of(args))) throw new AssertionError("game args");
                        Class.forName("fixture.Library");
                        try {
                            Class.forName("ProjectCode");
                            throw new AssertionError("Project output must not be on the game classpath");
                        } catch (ClassNotFoundException expected) {
                        }
                        System.out.println("CLIENT_OK");
                    }
                }
                """);
        Path library = jar("library", "fixture.Library", "package fixture; public class Library {}");
        Path assets = directory.resolve("assets.json");
        Files.writeString(assets, "{\"objects\":{}}");
        JsonObject version = TestGradlePlugin.GSON.fromJson("""
            {"id":"26.3","arguments":{"game":["--version","${version_name}"],
             "jvm":["-Dfixture=true","-cp","${classpath}"]},
             "assets":"test","complianceLevel":1,"javaVersion":{"majorVersion":25},
             "mainClass":"fixture.Game","minimumLauncherVersion":21,
             "releaseTime":"test","time":"test","type":"release"}
            """, JsonObject.class);
        var downloads = new JsonObject();
        downloads.add("client", download(game));
        downloads.add("server", download(game));
        version.add("downloads", downloads);
        JsonObject assetIndex = download(assets);
        assetIndex.addProperty("id", "test");
        version.add("assetIndex", assetIndex);
        JsonObject artifact = download(library);
        artifact.addProperty("path", "fixture/library.jar");
        var libraryDownloads = new JsonObject();
        libraryDownloads.add("artifact", artifact);
        var libraryEntry = new JsonObject();
        libraryEntry.addProperty("name", "fixture:library:1");
        libraryEntry.add("downloads", libraryDownloads);
        JsonArray libraries = new JsonArray();
        libraries.add(libraryEntry);
        version.add("libraries", libraries);
        Path metadata = directory.resolve("version.json");
        Files.writeString(metadata, version.toString());
        JsonObject manifestEntry = download(metadata);
        manifestEntry.addProperty("id", "26.3");
        JsonArray versions = new JsonArray();
        versions.add(manifestEntry);
        var manifest = new JsonObject();
        manifest.add("versions", versions);
        Path manifestFile = directory.resolve("manifest.json");
        Files.writeString(manifestFile, manifest.toString());

        Path project = directory.resolve("project with spaces");
        Files.createDirectories(project.resolve("src/main/java"));
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'fixture'");
        Files.writeString(project.resolve("build.gradle"), """
            plugins { id 'dev.turtywurty.testgradleplugin' }
            testGradle { minecraftVersion = '26.3' }
            tasks.named('downloadPistonMeta') { manifestUrl = '%s' }
            """.formatted(manifestFile.toUri()));
        Files.writeString(project.resolve("src/main/java/ProjectCode.java"),
            """
                import fixture.Game;
                import fixture.Library;
                public class ProjectCode {
                    Game game; Library library;
                }
                """);
        BuildResult launch = runner(project, "runClient").build();
        assertTrue(launch.getOutput().contains("CLIENT_OK"));
        assertNull(launch.task(":compileJava"));
        assertNull(launch.task(":classes"));
        BuildResult result = runner(project, "build", "runClient").build();
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava").getOutcome());
        assertTrue(result.getOutput().contains("CLIENT_OK"));
        assertNull(result.task(":decompile"));
        assertNull(result.task(":remapClasses"));
        assertNull(result.task(":extractServer"));
        assertTrue(Files.isRegularFile(project.resolve(".gradle/minecraft/26.3/client.jar")));
        BuildResult second = runner(project, "build").build();
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":compileJava").getOutcome());
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":downloadClient").getOutcome());

        // The optional generated IDEA model attaches existing decompiled sources.
        Path sources = project.resolve(".gradle/minecraft/26.3/sources");
        Files.createDirectories(sources);
        Files.writeString(sources.resolve("Game.java"), "package fixture; public class Game {}");
        runner(project, "ideaModule").build();
        assertTrue(Files.readString(project.resolve("fixture.iml")).contains("26.3/sources"));
    }

    private GradleRunner runner(Path project, String... arguments) {
        return GradleRunner.create().withProjectDir(project.toFile()).withPluginClasspath()
            .withArguments(arguments);
    }

    private JsonObject download(Path file) throws IOException {
        var json = new JsonObject();
        json.addProperty("url", file.toUri().toString());
        json.addProperty("sha1", HashingFunction.SHA1.hash(file));
        json.addProperty("size", Files.size(file));
        return json;
    }

    private Path jar(String name, String className, String source) throws IOException {
        Path sourceDir = directory.resolve(name + "-src");
        Path classes = directory.resolve(name + "-classes");
        Path javaFile = sourceDir.resolve(className.replace('.', '/') + ".java");
        Files.createDirectories(javaFile.getParent());
        Files.createDirectories(classes);
        Files.writeString(javaFile, source);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
            "--release", "25", "-d", classes.toString(), javaFile.toString()));
        Path output = directory.resolve(name + ".jar");
        try (var jar = new JarOutputStream(Files.newOutputStream(output))) {
            String relative = className.replace('.', '/') + ".class";
            jar.putNextEntry(new JarEntry(relative));
            jar.write(Files.readAllBytes(classes.resolve(relative)));
            jar.closeEntry();
        }

        return output;
    }
}
