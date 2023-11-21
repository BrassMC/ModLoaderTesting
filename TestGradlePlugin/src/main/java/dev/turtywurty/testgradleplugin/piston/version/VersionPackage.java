package dev.turtywurty.testgradleplugin.piston.version;

import org.gradle.internal.impldep.com.google.gson.JsonArray;
import org.gradle.internal.impldep.com.google.gson.JsonObject;

import java.util.List;

public record VersionPackage(MinecraftArguments arguments, AssetIndex assetIndex, String assets, int complianceLevel,
                             Downloads downloads, String id, JavaVersion javaVersion, List<Library> libraries,
                             Logging logging, String mainClass, int minimumLauncherVersion, String releaseTime,
                             String time, String type) {
    public static VersionPackage fromJson(JsonObject json) {
        JsonObject argumentsJson = json.getAsJsonObject("arguments");
        MinecraftArguments minecraftArguments = MinecraftArguments.fromJson(argumentsJson);

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

        return new VersionPackage(minecraftArguments, assetIndex, assets, complianceLevel, downloads, id, javaVersion,
                libraries, logging, mainClass, minimumLauncherVersion, releaseTime, time, type);
    }
}
