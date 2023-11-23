package dev.turtywurty.testgradleplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import dev.turtywurty.testgradleplugin.tasks.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;
import org.jetbrains.annotations.NotNull;

public class TestGradlePlugin implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void apply(@NotNull Project target) {
        TestGradleExtension extension = target.getExtensions().create("testGradle", TestGradleExtension.class);
        TaskContainer tasks = target.getTasks();

        DownloadPistonMetaTask downloadPistonMetaTask = tasks.create("downloadPistonMeta", DownloadPistonMetaTask.class);
        downloadPistonMetaTask.setGroup("minecraft");
        downloadPistonMetaTask.setDescription("Downloads the Piston Meta version manifest and the specified version.");
        downloadPistonMetaTask.getVersion().set(extension.getMinecraftVersion());

        DownloadClientTask downloadClientTask = tasks.create("downloadClient", DownloadClientTask.class);
        downloadClientTask.setGroup("minecraft");
        downloadClientTask.setDescription("Downloads the Minecraft client jar.");
        downloadClientTask.dependsOn(downloadPistonMetaTask);
        downloadClientTask.getOutputDir().set(downloadPistonMetaTask.getOutputDir());
        downloadClientTask.getVersion().set(downloadPistonMetaTask.getVersion());

        DownloadServerTask downloadServerTask = tasks.create("downloadServer", DownloadServerTask.class);
        downloadServerTask.setGroup("minecraft");
        downloadServerTask.setDescription("Downloads the Minecraft server jar.");
        downloadServerTask.dependsOn(downloadPistonMetaTask);
        downloadServerTask.getOutputDir().set(downloadPistonMetaTask.getOutputDir());
        downloadServerTask.getVersion().set(downloadPistonMetaTask.getVersion());

        DownloadClientMappingsTask downloadClientMappingsTask = tasks.create("downloadClientMappings", DownloadClientMappingsTask.class);
        downloadClientMappingsTask.setGroup("minecraft");
        downloadClientMappingsTask.setDescription("Downloads the Minecraft client mappings.");
        downloadClientMappingsTask.dependsOn(downloadPistonMetaTask);
        downloadClientMappingsTask.getOutputDir().set(downloadPistonMetaTask.getOutputDir());
        downloadClientMappingsTask.getVersion().set(downloadPistonMetaTask.getVersion());

        DownloadServerMappingsTask downloadServerMappingsTask = tasks.create("downloadServerMappings", DownloadServerMappingsTask.class);
        downloadServerMappingsTask.setGroup("minecraft");
        downloadServerMappingsTask.setDescription("Downloads the Minecraft server mappings.");
        downloadServerMappingsTask.dependsOn(downloadPistonMetaTask);
        downloadServerMappingsTask.getOutputDir().set(downloadPistonMetaTask.getOutputDir());
        downloadServerMappingsTask.getVersion().set(downloadPistonMetaTask.getVersion());

        DownloadAssetsTask downloadAssetsTask = tasks.create("downloadAssets", DownloadAssetsTask.class);
        downloadAssetsTask.setGroup("minecraft");
        downloadAssetsTask.setDescription("Downloads the Minecraft assets.");
        downloadAssetsTask.dependsOn(downloadPistonMetaTask);
        downloadAssetsTask.getOutputDir().set(downloadPistonMetaTask.getOutputDir());
        downloadAssetsTask.getVersion().set(downloadPistonMetaTask.getVersion());

        DownloadLibrariesTask downloadLibrariesTask = tasks.create("downloadLibraries", DownloadLibrariesTask.class);
        downloadLibrariesTask.setGroup("minecraft");
        downloadLibrariesTask.setDescription("Downloads the Minecraft libraries.");
        downloadLibrariesTask.dependsOn(downloadPistonMetaTask);
        downloadLibrariesTask.getOutputDir().set(downloadPistonMetaTask.getOutputDir());
        downloadLibrariesTask.getVersion().set(downloadPistonMetaTask.getVersion());

        RunClientTask runClientTask = tasks.create("runClient", RunClientTask.class);
        runClientTask.setGroup("minecraft");
        runClientTask.setDescription("Runs the Minecraft client.");
        runClientTask.dependsOn(downloadClientTask, downloadClientMappingsTask, downloadAssetsTask, downloadLibrariesTask);
        runClientTask.getOutputDir().set(downloadClientTask.getOutputDir());
        runClientTask.getVersion().set(downloadPistonMetaTask.getVersion());
        runClientTask.getRunDir().set(target.getLayout().getProjectDirectory().dir("run/client"));
    }
}
