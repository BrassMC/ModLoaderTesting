package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.piston.version.CLIArguments;
import dev.turtywurty.testgradleplugin.piston.version.Download;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import dev.turtywurty.testgradleplugin.util.Downloads;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.turtywurty.testgradleplugin.piston.version.Logging;
import java.util.List;

@DisableCachingByDefault(because = "Starts an interactive game process")
public abstract class RunClientTask extends JavaExec {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getVersionJson();
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getAssetsDirectory();
    @Internal
    public abstract DirectoryProperty getRunDirectory();
    @Input
    public abstract Property<String> getUsername();
    @Input
    @Optional
    public abstract Property<String> getUuid();
    @Internal
    public abstract Property<String> getAccessToken();

    @Override
    @TaskAction
    public void exec() {
        try {
            prepareLaunch();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare Minecraft launch", exception);
        }

        super.exec();
    }

    private void prepareLaunch() throws IOException {
        var version = VersionPackage.fromPath(getVersionJson().get().getAsFile().toPath());
        Path run = getRunDirectory().get().getAsFile().toPath();
        Path natives = run.resolve("natives");
        Files.createDirectories(natives);
        setWorkingDir(run);
        Map<String, String> values = new HashMap<>();
        values.put("auth_player_name", getUsername().get());
        values.put("auth_uuid", getUuid().getOrElse(UUID.nameUUIDFromBytes(
            ("OfflinePlayer:" + getUsername().get()).getBytes(StandardCharsets.UTF_8)).toString().replace("-", "")));
        values.put("auth_access_token", getAccessToken().get());
        values.put("auth_xuid", "");
        values.put("clientid", "");
        values.put("user_type", "msa");
        values.put("user_properties", "{}");
        values.put("version_name", version.id());
        values.put("version_type", version.type());
        values.put("game_directory", run.toAbsolutePath().toString());
        values.put("assets_root", getAssetsDirectory().get().getAsFile().getAbsolutePath());
        values.put("assets_index_name", version.assetIndex().id());
        values.put("natives_directory", natives.toAbsolutePath().toString());
        values.put("launcher_name", "TestGradlePlugin");
        values.put("launcher_version", "1.0-SNAPSHOT");
        values.put("classpath", getClasspath().getAsPath());
        values.put("classpath_separator", System.getProperty("path.separator"));
        values.put("library_directory",
            getVersionJson().get().getAsFile().getParentFile().toPath().resolve("libraries").toString());

        List<String> metadataJvm = version.arguments().jvm().resolve(values);
        List<String> jvm = new ArrayList<>();
        for (int index = 0; index < metadataJvm.size(); index++) {
            String token = metadataJvm.get(index);
            if (token.equals("-cp") || token.equals("-classpath") || token.equals("--class-path")) {
                index++;
            } else {
                jvm.add(token);
            }
        }

        if (version.logging() != null && version.logging().client() != null) {
            Logging.Client logging = version.logging().client();
            Logging.Client.LoggingFile file = logging.file();
            Path output = run.resolve("logging").resolve(file.id());
            Downloads.fetch(new Download(file.sha1(), file.size(), file.url()), output);
            jvm.add(CLIArguments.substitute(logging.argument(), Map.of("path", output.toAbsolutePath().toString())));
        }

        jvmArgs(jvm);
        List<String> game = new ArrayList<>(version.arguments().game().resolve(values));
        if (getArgs() != null) {
            game.addAll(getArgs());
        }

        setArgs(game);
    }
}
