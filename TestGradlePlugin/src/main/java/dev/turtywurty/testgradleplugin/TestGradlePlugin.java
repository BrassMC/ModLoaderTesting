package dev.turtywurty.testgradleplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.turtywurty.testgradleplugin.tasks.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;
import org.jetbrains.annotations.NotNull;

public class TestGradlePlugin implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void apply(@NotNull Project target) {
        TaskContainer tasks = target.getTasks();

        DownloadPistonMetaTask downloadPistonMetaTask = tasks.create("downloadPistonMeta", DownloadPistonMetaTask.class);
        downloadPistonMetaTask.setGroup("minecraft");
        downloadPistonMetaTask.setDescription("Downloads the Piston Meta version manifest and the specified version.");

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
        downloadClientMappingsTask.setDescription("Downloads the Minecraft client mappings jar.");
        downloadClientMappingsTask.dependsOn(downloadPistonMetaTask);
        downloadClientMappingsTask.getOutputDir().set(downloadPistonMetaTask.getOutputDir());
        downloadClientMappingsTask.getVersion().set(downloadPistonMetaTask.getVersion());

        DownloadServerMappingsTask downloadServerMappingsTask = tasks.create("downloadServerMappings", DownloadServerMappingsTask.class);
        downloadServerMappingsTask.setGroup("minecraft");
        downloadServerMappingsTask.setDescription("Downloads the Minecraft server mappings jar.");
        downloadServerMappingsTask.dependsOn(downloadPistonMetaTask);
        downloadServerMappingsTask.getOutputDir().set(downloadPistonMetaTask.getOutputDir());
        downloadServerMappingsTask.getVersion().set(downloadPistonMetaTask.getVersion());

        RunClientTask runClientTask = tasks.create("runClient", RunClientTask.class);
        runClientTask.setGroup("minecraft");
        runClientTask.setDescription("Runs the Minecraft client.");
        runClientTask.dependsOn(downloadClientTask, downloadClientMappingsTask);
        runClientTask.getOutputDir().set(downloadClientTask.getOutputDir());
        runClientTask.getVersion().set(downloadPistonMetaTask.getVersion());
        runClientTask.getRunDir().set(target.getLayout().getBuildDirectory().dir("run/client"));
    }
}
