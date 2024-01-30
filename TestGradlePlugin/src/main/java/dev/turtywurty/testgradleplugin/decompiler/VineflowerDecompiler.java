package dev.turtywurty.testgradleplugin.decompiler;

import org.gradle.api.Project;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VineflowerDecompiler extends Decompiler {
    public VineflowerDecompiler(Project project) {
        super(project);
    }

    public void decompile(Path file, Path outputDir, Collection<Path> libraries) {
        decompile(file, outputDir, new HashMap<>(), libraries);
    }

    @Override
    public void decompile(Path file, Path outputDir, Map<String, Object> options, Collection<Path> libraries) {
        options.put(IFernflowerPreferences.WARN_INCONSISTENT_INNER_CLASSES, "0");

        try(IResultSaver saver = new VineflowerResultSaver(outputDir)) {
            var decompiler = new BaseDecompiler(saver, options, new PrintStreamLogger(System.out));
            decompiler.addSource(file.toFile());
            for (Path library : libraries) {
                decompiler.addLibrary(library.toFile());
            }

            decompiler.decompileContext();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to decompile!", exception);
        }
    }
}
