package dev.turtywurty.testgradleplugin.piston.version;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import dev.turtywurty.testgradleplugin.piston.PistonMetaVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record VersionPackage(Arguments arguments, AssetIndex assetIndex, String assets, int complianceLevel,
                             Downloads downloads, String id, JavaVersion javaVersion, List<Library> libraries,
                             Logging logging, String mainClass, int minimumLauncherVersion, String releaseTime,
                             String time, String type) {
    public static VersionPackage fromJson(JsonObject json) {
        JsonObject argumentsJson = json.getAsJsonObject("arguments");
        JsonArray gameJson = argumentsJson.getAsJsonArray("game");
        JsonArray jvmJson = json.getAsJsonArray("jvm");
        CLIArguments gameArguments = CLIArguments.fromJsonArray(gameJson);
        CLIArguments jvmArguments = CLIArguments.fromJsonArray(jvmJson);
        var arguments = new Arguments(gameArguments, jvmArguments);

        JsonObject assetIndexJson = json.getAsJsonObject("assetIndex");
        AssetIndex assetIndex = AssetIndex.fromJson(assetIndexJson);

        String assets = json.get("assets").getAsString();
        int complianceLevel = json.get("complianceLevel").getAsInt();

        JsonObject downloadsJson = json.getAsJsonObject("downloads");
        Downloads downloads = Downloads.fromJson(downloadsJson);

        String id = json.get("id").getAsString();

        JsonObject javaVersionJson = json.getAsJsonObject("javaVersion");
        JavaVersion javaVersion = JavaVersion.fromJson(javaVersionJson);

        JsonArray librariesJson = json.getAsJsonArray("libraries");
        List<Library> libraries = Library.fromJsonArray(librariesJson);

        JsonObject loggingJson = json.getAsJsonObject("logging");
        Logging logging = Logging.fromJson(loggingJson);

        String mainClass = json.get("mainClass").getAsString();
        int minimumLauncherVersion = json.get("minimumLauncherVersion").getAsInt();
        String releaseTime = json.get("releaseTime").getAsString();
        String time = json.get("time").getAsString();
        String type = json.get("type").getAsString();

        return new VersionPackage(arguments, assetIndex, assets, complianceLevel, downloads, id, javaVersion,
                libraries, logging, mainClass, minimumLauncherVersion, releaseTime, time, type);
    }

    public static VersionPackage fromPath(Path path) {
        Path versionJsonPath = path.toAbsolutePath();
        VersionPackage versionPackage;
        try {
            String jsonStr = Files.readString(versionJsonPath);
            JsonObject json = TestGradlePlugin.GSON.fromJson(jsonStr, JsonObject.class);
            versionPackage = VersionPackage.fromJson(json);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to download client!", exception);
        }

        return versionPackage;
    }

    public static void download(PistonMetaVersion metaVersion, Path outputDir) {
        Path versionPath = outputDir.resolve(metaVersion.id());
        Path versionJson = versionPath.resolve("version.json");

        String url = metaVersion.url();
        try(InputStream inputStream = new URL(url).openStream()) {
            Files.createDirectories(versionPath);
            Files.write(versionJson, inputStream.readAllBytes());
        } catch (IOException exception) {
            throw new RuntimeException("Failed to download version!", exception);
        }
    }
}
