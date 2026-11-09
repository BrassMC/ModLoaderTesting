package dev.turtywurty.testgradleplugin.piston.version;

import com.google.gson.JsonObject;

public record Downloads(Download client, Download server) {
    public static Downloads fromJson(JsonObject json) {
        JsonObject clientJson = json.getAsJsonObject("client");
        Download client = Download.fromJson(clientJson);

        JsonObject serverJson = json.getAsJsonObject("server");
        Download server = Download.fromJson(serverJson);

        return new Downloads(client, server);
    }
}
