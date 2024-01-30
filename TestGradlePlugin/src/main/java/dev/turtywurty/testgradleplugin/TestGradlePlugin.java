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

public class TestGradlePlugin implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void apply(@NotNull Project target) {
        target.getPlugins().apply("java");

        final TestGradleExtension extension = target.getExtensions().create("testGradle", TestGradleExtension.class);
        final Property<String> minecraftVersion = extension.getMinecraftVersion().convention("1.20.4");
        final Provider<TestGradleExtension.Side> sideProvider = extension.getSideEnum();

        System.out.println("Minecraft Version: " + minecraftVersion.get());
        System.out.println("Side: " + sideProvider.get());

        final TaskContainer tasks = target.getTasks();

        DownloadPistonMetaTask downloadPistonMetaTask = tasks.create("downloadPistonMeta", DownloadPistonMetaTask.class);
        downloadPistonMetaTask.setGroup("minecraft");
        downloadPistonMetaTask.setDescription("Downloads the Piston Meta version manifest and the specified version.");

        DownloadClientTask downloadClientTask = tasks.create("downloadClient", DownloadClientTask.class);
        downloadClientTask.setGroup("minecraft");
        downloadClientTask.setDescription("Downloads the Minecraft client jar.");
        downloadClientTask.dependsOn(downloadPistonMetaTask);

        ExtractClientTask extractClientTask = tasks.create("extractClient", ExtractClientTask.class);
        extractClientTask.setGroup("minecraft");
        extractClientTask.setDescription("Extracts the Minecraft client jar.");
        extractClientTask.dependsOn(downloadClientTask);

        DownloadClientMappingsTask downloadClientMappingsTask = tasks.create("downloadClientMappings", DownloadClientMappingsTask.class);
        downloadClientMappingsTask.setGroup("minecraft");
        downloadClientMappingsTask.setDescription("Downloads the Minecraft client mappings.");
        downloadClientMappingsTask.dependsOn(downloadPistonMetaTask);

        DownloadServerTask downloadServerTask = tasks.create("downloadServer", DownloadServerTask.class);
        downloadServerTask.setGroup("minecraft");
        downloadServerTask.setDescription("Downloads the Minecraft server jar.");
        downloadServerTask.dependsOn(downloadPistonMetaTask);

        ExtractServerTask extractServerTask = tasks.create("extractServer", ExtractServerTask.class);
        extractServerTask.setGroup("minecraft");
        extractServerTask.setDescription("Extracts the Minecraft server jar.");
        extractServerTask.dependsOn(downloadServerTask);

        DownloadServerMappingsTask downloadServerMappingsTask = tasks.create("downloadServerMappings", DownloadServerMappingsTask.class);
        downloadServerMappingsTask.setGroup("minecraft");
        downloadServerMappingsTask.setDescription("Downloads the Minecraft server mappings.");
        downloadServerMappingsTask.dependsOn(downloadPistonMetaTask);

        DownloadAssetsTask downloadAssetsTask = tasks.create("downloadAssets", DownloadAssetsTask.class);
        downloadAssetsTask.setGroup("minecraft");
        downloadAssetsTask.setDescription("Downloads the Minecraft assets.");
        downloadAssetsTask.dependsOn(downloadPistonMetaTask);

        DownloadLibrariesTask downloadLibrariesTask = tasks.create("downloadLibraries", DownloadLibrariesTask.class);
        downloadLibrariesTask.setGroup("minecraft");
        downloadLibrariesTask.setDescription("Downloads the Minecraft libraries.");
        downloadLibrariesTask.dependsOn(downloadPistonMetaTask);

        RemapClassesTask remapClassesTask = tasks.create("remapClasses", RemapClassesTask.class);
        remapClassesTask.setGroup("minecraft");
        remapClassesTask.setDescription("Remaps the Minecraft client and server jars.");
        remapClassesTask.dependsOn(sideProvider.map(side -> switch (side) {
            case CLIENT -> new Object[]{extractClientTask, downloadClientMappingsTask};
            case SERVER -> new Object[]{extractServerTask, downloadServerMappingsTask};
            case BOTH ->
                    new Object[]{extractClientTask, extractServerTask, downloadClientMappingsTask, downloadServerMappingsTask};
        }).getOrElse(new Object[0]));

        MergeTask mergeTask = tasks.create("merge", MergeTask.class);
        mergeTask.setGroup("minecraft");
        mergeTask.setDescription("Merges the Minecraft client and server into one directory.");
        mergeTask.dependsOn(remapClassesTask);

        RecompileTask recompileTask = tasks.create("recompile", RecompileTask.class);
        recompileTask.setGroup("minecraft");
        recompileTask.setDescription("Recompiles the Minecraft client and server into a jar.");
        recompileTask.dependsOn(sideProvider.map(side -> switch (side) {
            case CLIENT, SERVER -> new Object[]{remapClassesTask};
            case BOTH -> new Object[]{remapClassesTask, mergeTask};
        }).getOrElse(new Object[0]));

        DecompileTask decompileTask = tasks.create("decompile", DecompileTask.class);
        decompileTask.setGroup("minecraft");
        decompileTask.setDescription("Decompiles the Minecraft client and server jars.");
        decompileTask.dependsOn(recompileTask);

        SourcesStatsTask sourcesStatsTask = tasks.create("sourcesStats", SourcesStatsTask.class);
        sourcesStatsTask.setGroup("minecraft");
        sourcesStatsTask.setDescription("Gets the stats of the decompiled Minecraft client and server jars.");
        sourcesStatsTask.dependsOn(decompileTask);

        RunClientTask runClientTask = tasks.create("runClient", RunClientTask.class);
        runClientTask.setGroup("minecraft");
        runClientTask.setDescription("Runs the Minecraft client.");
        runClientTask.dependsOn(downloadClientTask, downloadAssetsTask, downloadLibrariesTask);
    }
}
