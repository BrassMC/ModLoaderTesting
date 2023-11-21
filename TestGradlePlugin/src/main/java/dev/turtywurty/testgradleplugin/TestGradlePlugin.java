package dev.turtywurty.testgradleplugin;

import dev.turtywurty.testgradleplugin.tasks.DownloadClientTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.internal.impldep.com.google.gson.Gson;
import org.gradle.internal.impldep.com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

public class TestGradlePlugin implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void apply(@NotNull Project target) {
        TaskContainer tasks = target.getTasks();

        DownloadClientTask downloadClientTask = tasks.create("downloadClient", DownloadClientTask.class);
        downloadClientTask.setGroup("minecraft");
        downloadClientTask.setDescription("Downloads the Minecraft client jar.");
    }
}
