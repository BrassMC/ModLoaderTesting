package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.piston.version.Download;
import dev.turtywurty.testgradleplugin.piston.version.Library;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@CacheableTask
public abstract class DownloadLibrariesTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void downloadLibraries() {
        System.out.println("Downloading libraries for version " + getVersion().get());

        Path versionPath = getOutputDir()
                .orElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .get()
                .getAsFile()
                .toPath()
                .resolve(getVersion().get());

        Path versionJsonPath = versionPath.resolve("version.json");

        VersionPackage versionPackage = VersionPackage.fromPath(versionJsonPath);
        System.out.println("Version package path: " + versionJsonPath);

        List<Library> libraries = versionPackage.libraries();
        System.out.println("Libraries: " + libraries.size());

        libraryLoop: for (Library library : libraries) {
            java.util.Optional<List<Library.DownloadRule>> rules = library.rules();
            if (rules.isPresent()) {
                List<Library.DownloadRule> downloadRules = rules.get();
                for (Library.DownloadRule rule : downloadRules) {
                    Library.DownloadRule.Action action = rule.action();
                    String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
                    String ruleOsName = rule.os().name().toLowerCase(Locale.ROOT);
                    if (ruleOsName.equals(osName)) {
                        if (action == Library.DownloadRule.Action.ALLOW) {
                            System.out.println("Allowed to download library " + library.name());
                        } else if (action == Library.DownloadRule.Action.DISALLOW) {
                            System.out.println("Disallowed to download library " + library.name());
                            continue libraryLoop;
                        }
                    }
                }
            }

            System.out.println("Library: " + library.name());
            Download artifact = library.artifact();

            Path libraryPath = versionPath.resolve("libraries")
                    .resolve(library.name().replace(":", "/"));

            Path downloadPath = artifact.downloadToPath(libraryPath);
            System.out.println("Downloaded library " + library.name() + " to " + downloadPath);
        }
    }
}
