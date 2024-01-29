package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import org.gradle.api.Project;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.model.idea.IdeaProject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

@CacheableTask
public class IdeaTask extends DefaultTestGradleTask {
    @OutputFile
    private final Path outputJar;

    public IdeaTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.outputJar = versionPath.resolve("recomp_" + switch (getSide()) {
            case CLIENT -> "client";
            case SERVER -> "server";
            case BOTH -> "joined";
        } + ".jar");
    }

    private static void addDependency(Project project, String name, String group, String version) {
        // get idea module file
        // add dependency to it

        IdeaProject ideaProject = project.getExtensions().getByType(IdeaProject.class);
    }

    @TaskAction
    public void idea() {
        TestGradleExtension.Side side = getSide();
        System.out.println("Generating idea files for side " + side.name().toLowerCase());

        if (Files.notExists(outputJar))
            throw new IllegalStateException("The " + side.name().toLowerCase() + " has not been recompiled yet!");

        Path projectDir = getProject().getProjectDir().toPath();
        Path ideaDir = projectDir.resolve(".idea");
        if (Files.notExists(ideaDir)) {
            try {
                Files.createDirectory(ideaDir);

                Path moduleFile = ideaDir.resolve("modules.xml");
                if (Files.notExists(moduleFile)) {
                    Files.createFile(moduleFile);

                    try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(moduleFile.toFile()))) {
                        jarOutputStream.putNextEntry(new JarEntry("modules.xml"));
                        jarOutputStream.write("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <modules>
                                    <module fileurl="file://\\$MODULE_DIR\\$/build/classes/java/main" filepath="\\$MODULE_DIR\\$/build/classes/java/main" />
                                    <module fileurl="file://\\$MODULE_DIR\\$/build/resources/main" filepath="\\$MODULE_DIR\\$/build/resources/main" />
                                    <module fileurl="jar://${jarFileName}!/main" filepath="${jarFileName}" />
                                </modules>
                                """.replace("${jarFileName}", outputJar.getFileName().toString()).getBytes());
                        jarOutputStream.closeEntry();
                    }
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create .idea directory!", exception);
            }
        }
    }

    public Path getOutputJar() {
        return outputJar;
    }
}
