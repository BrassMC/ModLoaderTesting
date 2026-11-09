package dev.turtywurty.testgradleplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import dev.turtywurty.testgradleplugin.piston.PistonMeta;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import dev.turtywurty.testgradleplugin.tasks.DownloadAssetsTask;
import dev.turtywurty.testgradleplugin.tasks.DownloadClientTask;
import dev.turtywurty.testgradleplugin.tasks.DownloadLibrariesTask;
import dev.turtywurty.testgradleplugin.tasks.DownloadPistonMetaTask;
import dev.turtywurty.testgradleplugin.tasks.DownloadServerTask;
import dev.turtywurty.testgradleplugin.tasks.ExtractServerTask;
import dev.turtywurty.testgradleplugin.tasks.RunClientTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.Module;
import org.gradle.plugins.ide.idea.model.ModuleLibrary;
import org.gradle.plugins.ide.idea.model.PathFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.plugins.ide.idea.model.Dependency;
import org.gradle.plugins.ide.idea.model.FilePath;

public class TestGradlePlugin implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void apply(Project project) {
        project.getPlugins().apply("java");
        project.getPlugins().apply("idea");
        TestGradleExtension extension = project.getExtensions().create("testGradle", TestGradleExtension.class);
        extension.getMinecraftVersion().convention("26.3");
        extension.getSide().convention("CLIENT");
        extension.getVineflowerVersion().convention("1.12.0");
        extension.getUsername()
            .convention(project.getProviders().gradleProperty("minecraftUsername").orElse("Developer"));
        extension.getUuid().convention(project.getProviders().gradleProperty("minecraftUuid"));
        extension.getAccessToken()
            .convention(project.getProviders().environmentVariable("MINECRAFT_ACCESS_TOKEN").orElse("0"));
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        java.getToolchain().getLanguageVersion().convention(JavaLanguageVersion.of(25));
        JavaToolchainService toolchains = project.getExtensions().getByType(JavaToolchainService.class);
        Provider<JavaLauncher> launcher = toolchains.launcherFor(java.getToolchain());
        Provider<Directory> cache = extension.getMinecraftVersion()
            .map(version -> project.getLayout().getProjectDirectory().dir(".gradle/minecraft/" + version));
        TaskContainer tasks = project.getTasks();
        TaskProvider<DownloadPistonMetaTask> metadata = tasks.register("downloadPistonMeta",
            DownloadPistonMetaTask.class, task -> {
                task.setGroup("minecraft");
                task.getMinecraftVersion().set(extension.getMinecraftVersion());
                task.getManifestUrl().convention(PistonMeta.META_URL);
                task.getVersionJson().set(cache.map(dir -> dir.file("version.json")));
            });

        TaskProvider<DownloadClientTask> client = tasks.register("downloadClient", DownloadClientTask.class, task -> {
            task.setGroup("minecraft");
            task.getVersionJson().set(metadata.flatMap(DownloadPistonMetaTask::getVersionJson));
            task.getClientJar().set(cache.map(dir -> dir.file("client.jar")));
        });

        TaskProvider<DownloadLibrariesTask> libraries = tasks.register("downloadLibraries", DownloadLibrariesTask.class,
            task -> {
                task.setGroup("minecraft");
                task.getVersionJson().set(metadata.flatMap(DownloadPistonMetaTask::getVersionJson));
                task.getLibrariesDirectory().set(cache.map(dir -> dir.dir("libraries")));
                task.getLibrariesJson().set(cache.map(dir -> dir.file("libraries.json")));
                task.getPlatform().set(System.getProperty("os.name") + "/" + System.getProperty("os.arch")
                    + "/" + System.getProperty("os.version"));
            });

        FileCollection libraryFiles = project
            .files(libraries.flatMap(DownloadLibrariesTask::getLibrariesJson).map(index -> {
                File file = index.getAsFile();
                if (!file.isFile())
                    return List.of();

                try {
                    JsonObject json = GSON.fromJson(Files.readString(file.toPath()), JsonObject.class);
                    List<File> files = new ArrayList<>();
                    json.entrySet().forEach(entry -> files
                        .add(new File(file.getParentFile(), "libraries/" + entry.getValue().getAsString())));
                    return files;
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot read Minecraft libraries", exception);
                }
            })).builtBy(libraries);
        Configuration minecraft = project.getConfigurations().create("minecraft");
        minecraft.setCanBeConsumed(false);
        minecraft.setCanBeResolved(false);
        project.getConfigurations().getByName("implementation").extendsFrom(minecraft);
        project.getDependencies().add("minecraft", project.files(client.flatMap(DownloadClientTask::getClientJar)));
        project.getDependencies().add("minecraft", libraryFiles);

        tasks.register("prepareMinecraft", task -> {
            task.setGroup("minecraft");
            task.setDescription("Prepare the original Minecraft client and libraries for compilation and IDE import.");
            task.dependsOn(client, libraries);
        });

        tasks.named("ideaModule", task -> task.dependsOn(client, libraries));

        TaskProvider<DownloadAssetsTask> assets = tasks.register("downloadAssets", DownloadAssetsTask.class, task -> {
            task.setGroup("minecraft");
            task.getVersionJson().set(metadata.flatMap(DownloadPistonMetaTask::getVersionJson));
            task.getAssetsDirectory().set(cache.map(dir -> dir.dir("assets")));
            task.getAssetsUrl().convention("https://resources.download.minecraft.net/");
        });

        TaskProvider<DownloadServerTask> server = tasks.register("downloadServer", DownloadServerTask.class, task -> {
            task.setGroup("minecraft");
            task.getVersionJson().set(metadata.flatMap(DownloadPistonMetaTask::getVersionJson));
            task.getServerJar().set(cache.map(dir -> dir.file("server.jar")));
        });

        tasks.register("extractServer", ExtractServerTask.class, task -> {
            task.setGroup("minecraft");
            task.getServerJar().set(server.flatMap(DownloadServerTask::getServerJar));
            task.getGameJar().set(cache.map(dir -> dir.file("server/game.jar")));
            task.getLibrariesDirectory().set(cache.map(dir -> dir.dir("server/libraries")));
        });

        tasks.register("runClient", RunClientTask.class, task -> {
            task.setGroup("minecraft");
            task.setDescription("Launch the Minecraft client directly.");
            task.getMainClass().set(metadata.flatMap(DownloadPistonMetaTask::getVersionJson)
                .map(file -> VersionPackage.fromPath(file.getAsFile().toPath()).mainClass()));
            task.getJavaLauncher().set(launcher);
            task.getVersionJson().set(metadata.flatMap(DownloadPistonMetaTask::getVersionJson));
            task.getAssetsDirectory().set(assets.flatMap(DownloadAssetsTask::getAssetsDirectory));
            task.getRunDirectory().set(project.getLayout().getProjectDirectory().dir("run"));
            task.getUsername().set(extension.getUsername());
            task.getUuid().set(extension.getUuid());
            task.getAccessToken().set(extension.getAccessToken());
            task.setClasspath(project.files(client.flatMap(DownloadClientTask::getClientJar), libraryFiles));
            task.setMaxHeapSize("2G");
        });

        Configuration vineflower = project.getConfigurations().create("vineflower");
        vineflower.setCanBeConsumed(false);
        vineflower.defaultDependencies(dependencies -> dependencies.add(project.getDependencies().create(
            "org.vineflower:vineflower:" + extension.getVineflowerVersion().get())));
        project.getRepositories().mavenCentral();
        tasks.register("decompile", JavaExec.class, task -> {
            task.setGroup("minecraft");
            task.setDescription("Optionally generate Minecraft Java sources with Vineflower.");
            task.getJavaLauncher().set(launcher);
            task.getMainClass().set("org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler");
            task.setClasspath(vineflower);
            task.dependsOn(client, libraries);
            task.getInputs().file(client.flatMap(DownloadClientTask::getClientJar));
            task.getInputs().files(libraryFiles);
            task.getOutputs().dir(cache.map(dir -> dir.dir("sources")));
            task.setMaxHeapSize("4G");
            task.doFirst(_ -> {
                File output = cache.get().dir("sources").getAsFile();
                output.mkdirs();
                List<String> args = new ArrayList<>();
                args.add("-log=WARN");
                for (File library : libraryFiles) {
                    args.add("-e=" + library.getAbsolutePath());
                }

                args.add(client.get().getClientJar().get().getAsFile().getAbsolutePath());
                args.add(output.getAbsolutePath());
                task.setArgs(args);
            });
        });

        IdeaModel idea = project.getExtensions().getByType(IdeaModel.class);
        idea.getModule().getIml().whenMerged(model -> {
            Module module = (Module) model;
            var paths = new PathFactory();
            FilePath gamePath = paths.path(client.get().getClientJar().get().getAsFile());
            File sources = cache.get().dir("sources").getAsFile();
            if (sources.isDirectory()) {
                for (Dependency dependency : module.getDependencies()) {
                    if (dependency instanceof ModuleLibrary library && library.getClasses().contains(gamePath)) {
                        library.getSources().add(paths.path(sources));
                    }
                }
            }
        });

        project.afterEvaluate(_ -> {
            if (extension.getSideEnum().get() != TestGradleExtension.Side.CLIENT)
                throw new IllegalArgumentException(
                    "Only CLIENT compilation/launch is supported. Use downloadServer/extractServer for the separate server bundler.");

            if (!extension.getMinecraftVersion().get().equals("26.3"))
                throw new IllegalArgumentException("This unobfuscated pipeline currently targets Minecraft 26.3 only.");
        });
    }
}
