package dev.turtywurty.testgradleplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import dev.turtywurty.testgradleplugin.tasks.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
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
        Provider<TestGradleExtension.Side> sideProvider = extension.getSideEnum().orElse(TestGradleExtension.Side.BOTH);

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

        ExtractClientTask extractClientTask = tasks.create("extractClient", ExtractClientTask.class);
        extractClientTask.setGroup("minecraft");
        extractClientTask.setDescription("Extracts the Minecraft client jar.");
        extractClientTask.dependsOn(downloadClientTask);
        extractClientTask.getOutputDir().set(cacheDir.toFile());
        extractClientTask.getVersion().set(minecraftVersion);

        ExtractServerTask extractServerTask = tasks.create("extractServer", ExtractServerTask.class);
        extractServerTask.setGroup("minecraft");
        extractServerTask.setDescription("Extracts the Minecraft server jar.");
        extractServerTask.dependsOn(downloadServerTask);
        extractServerTask.getOutputDir().set(cacheDir.toFile());
        extractServerTask.getVersion().set(minecraftVersion);

        RemapClassesTask remapClassesTask = tasks.create("remapClasses", RemapClassesTask.class);
        remapClassesTask.setGroup("minecraft");
        remapClassesTask.setDescription("Remaps the Minecraft client and server jars.");
        remapClassesTask.dependsOn(sideProvider.map(side -> switch (side) {
            case CLIENT -> new Object[]{extractClientTask, downloadClientMappingsTask};
            case SERVER -> new Object[]{extractServerTask, downloadServerMappingsTask};
            case BOTH -> new Object[]{extractClientTask, downloadClientMappingsTask, extractServerTask, downloadServerMappingsTask};
        }).getOrElse(new Object[0]));
        remapClassesTask.getOutputDir().set(cacheDir.toFile());
        remapClassesTask.getVersion().set(minecraftVersion);
        remapClassesTask.getSide().set(sideProvider);

        MergeTask mergeTask = tasks.create("merge", MergeTask.class);
        mergeTask.setGroup("minecraft");
        mergeTask.setDescription("Merges the Minecraft client and server into one directory.");
        mergeTask.dependsOn(remapClassesTask);
        mergeTask.getOutputDir().set(cacheDir.toFile());
        mergeTask.getVersion().set(minecraftVersion);
        mergeTask.getSide().set(sideProvider);

        RecompileTask recompileTask = tasks.create("recompile", RecompileTask.class);
        recompileTask.setGroup("minecraft");
        recompileTask.setDescription("Recompiles the Minecraft client and server jars.");
        recompileTask.dependsOn(sideProvider.map(side -> switch (side) {
            case CLIENT, SERVER -> new Object[]{remapClassesTask};
            case BOTH -> new Object[]{remapClassesTask, mergeTask};
        }).getOrElse(new Object[0]));
        recompileTask.getOutputDir().set(cacheDir.toFile());
        recompileTask.getVersion().set(minecraftVersion);
        recompileTask.getSide().set(sideProvider);

        DecompileTask decompileTask = tasks.create("decompile", DecompileTask.class);
        decompileTask.setGroup("minecraft");
        decompileTask.setDescription("Decompiles the Minecraft client and server jars.");
        decompileTask.dependsOn(sideProvider.map(side -> switch (side) {
            case CLIENT, SERVER -> new Object[]{remapClassesTask};
            case BOTH -> new Object[]{remapClassesTask, mergeTask};
        }).getOrElse(new Object[0]));
        decompileTask.getOutputDir().set(cacheDir.toFile());
        decompileTask.getVersion().set(minecraftVersion);
        decompileTask.getVineflowerVersion().set(extension.getVineflowerVersion());
        decompileTask.getSide().set(sideProvider);

        RunClientTask runClientTask = tasks.create("runClient", RunClientTask.class);
        runClientTask.setGroup("minecraft");
        runClientTask.setDescription("Runs the Minecraft client.");
        runClientTask.dependsOn(downloadClientTask, downloadAssetsTask, downloadLibrariesTask);
        runClientTask.getOutputDir().set(cacheDir.toFile());
        runClientTask.getVersion().set(minecraftVersion);
        runClientTask.getRunDir().set(target.getLayout().getProjectDirectory().dir("run/client"));
    }
}
