package dev.turtywurty.testgradleplugin.decompiler;

import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public abstract class Decompiler {
    private final Project project;

    public Decompiler(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public abstract void decompile(Path file, Path outputDir, Map<String, Object> options, Collection<Path> libraries);

    @Deprecated
    public void decompile(File file, File outputDir, Map<String, Object> options, Collection<File> libraries) {
        decompile(file.toPath(), outputDir.toPath(), options, libraries.stream().map(File::toPath).toList());
    }
}
