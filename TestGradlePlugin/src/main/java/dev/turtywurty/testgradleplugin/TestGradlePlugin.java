package dev.turtywurty.testgradleplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import dev.turtywurty.testgradleplugin.tasks.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskContainer;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class TestGradlePlugin implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void apply(@NotNull Project target) {
        target.getPlugins().apply("java");

        TestGradleExtension extension = target.getExtensions().create("testGradle", TestGradleExtension.class);

        Path cacheDir = target.getGradle().getGradleUserHomeDir().toPath().resolve("caches/testGradle");
        Property<String> minecraftVersion = extension.getMinecraftVersion();

        TaskContainer tasks = target.getTasks();

        DownloadPistonMetaTask downloadPistonMetaTask = tasks.create("downloadPistonMeta", DownloadPistonMetaTask.class);
        downloadPistonMetaTask.setGroup("minecraft");
        downloadPistonMetaTask.setDescription("Downloads the Piston Meta version manifest and the specified version.");
        downloadPistonMetaTask.getOutputDir().set(cacheDir.toFile());
        downloadPistonMetaTask.getVersion().set(minecraftVersion);

        DownloadClientTask downloadClientTask = tasks.create("downloadClient", DownloadClientTask.class);
        downloadClientTask.setGroup("minecraft");
        downloadClientTask.setDescription("Downloads the Minecraft client jar.");
        downloadClientTask.dependsOn(downloadPistonMetaTask);
        downloadClientTask.getOutputDir().set(downloadPistonMetaTask.getOutputDir());
        downloadClientTask.getVersion().set(minecraftVersion);

        DownloadServerTask downloadServerTask = tasks.create("downloadServer", DownloadServerTask.class);
        downloadServerTask.setGroup("minecraft");
        downloadServerTask.setDescription("Downloads the Minecraft server jar.");
        downloadServerTask.dependsOn(downloadPistonMetaTask);
        downloadServerTask.getOutputDir().set(cacheDir.toFile());
        downloadServerTask.getVersion().set(minecraftVersion);

        DownloadClientMappingsTask downloadClientMappingsTask = tasks.create("downloadClientMappings", DownloadClientMappingsTask.class);
        downloadClientMappingsTask.setGroup("minecraft");
        downloadClientMappingsTask.setDescription("Downloads the Minecraft client mappings.");
        downloadClientMappingsTask.dependsOn(downloadPistonMetaTask);
        downloadClientMappingsTask.getOutputDir().set(cacheDir.toFile());
        downloadClientMappingsTask.getVersion().set(minecraftVersion);

        DownloadServerMappingsTask downloadServerMappingsTask = tasks.create("downloadServerMappings", DownloadServerMappingsTask.class);
        downloadServerMappingsTask.setGroup("minecraft");
        downloadServerMappingsTask.setDescription("Downloads the Minecraft server mappings.");
        downloadServerMappingsTask.dependsOn(downloadPistonMetaTask);
        downloadServerMappingsTask.getOutputDir().set(cacheDir.toFile());
        downloadServerMappingsTask.getVersion().set(minecraftVersion);

        DownloadAssetsTask downloadAssetsTask = tasks.create("downloadAssets", DownloadAssetsTask.class);
        downloadAssetsTask.setGroup("minecraft");
        downloadAssetsTask.setDescription("Downloads the Minecraft assets.");
        downloadAssetsTask.dependsOn(downloadPistonMetaTask);
        downloadAssetsTask.getOutputDir().set(cacheDir.toFile());
        downloadAssetsTask.getVersion().set(minecraftVersion);

        DownloadLibrariesTask downloadLibrariesTask = tasks.create("downloadLibraries", DownloadLibrariesTask.class);
        downloadLibrariesTask.setGroup("minecraft");
        downloadLibrariesTask.setDescription("Downloads the Minecraft libraries.");
        downloadLibrariesTask.dependsOn(downloadPistonMetaTask);
        downloadLibrariesTask.getOutputDir().set(cacheDir.toFile());
        downloadLibrariesTask.getVersion().set(minecraftVersion);

        RemapClassesTask remapClassesTask = tasks.create("remapClasses", RemapClassesTask.class);
        remapClassesTask.setGroup("minecraft");
        remapClassesTask.setDescription("Remaps the Minecraft client and server jars.");
        remapClassesTask.dependsOn(downloadClientTask, downloadServerTask, downloadClientMappingsTask, downloadServerMappingsTask);
        remapClassesTask.getOutputDir().set(cacheDir.toFile());
        remapClassesTask.getVersion().set(minecraftVersion);

        MergeJarTask mergeJarTask = tasks.create("mergeJar", MergeJarTask.class);
        mergeJarTask.setGroup("minecraft");
        mergeJarTask.setDescription("Merges the Minecraft client and server jars.");
        mergeJarTask.dependsOn(remapClassesTask);
        mergeJarTask.getOutputDir().set(cacheDir.toFile());
        mergeJarTask.getVersion().set(minecraftVersion);

        DecompileClientTask decompileClientTask = tasks.create("decompileClient", DecompileClientTask.class);
        decompileClientTask.setGroup("minecraft");
        decompileClientTask.setDescription("Decompiles the Minecraft client jar.");
        decompileClientTask.dependsOn(downloadClientTask, downloadClientMappingsTask, downloadAssetsTask, downloadLibrariesTask);
        decompileClientTask.getOutputDir().set(cacheDir.toFile());
        decompileClientTask.getVersion().set(minecraftVersion);
        decompileClientTask.getVineflowerVersion().set(extension.getVineflowerVersion());

        RunClientTask runClientTask = tasks.create("runClient", RunClientTask.class);
        runClientTask.setGroup("minecraft");
        runClientTask.setDescription("Runs the Minecraft client.");
        runClientTask.dependsOn(downloadClientTask, downloadClientMappingsTask, downloadAssetsTask, downloadLibrariesTask);
        runClientTask.getOutputDir().set(cacheDir.toFile());
        runClientTask.getVersion().set(minecraftVersion);
        runClientTask.getRunDir().set(target.getLayout().getProjectDirectory().dir("run/client"));
    }
}
