package dev.turtywurty.testgradleplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.turtywurty.testgradleplugin.piston.version.CLIArguments;
import dev.turtywurty.testgradleplugin.piston.version.Downloads;
import dev.turtywurty.testgradleplugin.piston.version.LaunchRules;
import dev.turtywurty.testgradleplugin.piston.version.VersionPackage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MetadataTest {
    @Test
    public void recognizesLauncherOperatingSystemNames() {
        assertEquals(OperatingSystem.WINDOWS, OperatingSystem.fromName("Windows 11"));
        assertEquals(OperatingSystem.LINUX, OperatingSystem.fromName("Linux"));
        assertEquals(OperatingSystem.OSX, OperatingSystem.fromName("Mac OS X"));
        assertEquals(OperatingSystem.OSX, OperatingSystem.fromName("Darwin"));
    }

    @Test
    public void respectsOrderedRulesPlatformsAndFeatures() {
        JsonArray rules = JsonParser.parseString("""
            [{"action":"allow"}, {"action":"disallow","os":{"name":"windows"}},
             {"action":"allow","os":{"name":"windows","arch":"amd64","version":"10.*"}}]
            """).getAsJsonArray();
        assertTrue(LaunchRules.allows(rules, "linux", "amd64", "6.0", Map.of()));
        assertFalse(LaunchRules.allows(rules, "windows", "aarch64", "10.0", Map.of()));
        assertTrue(LaunchRules.allows(rules, "windows", "amd64", "10.0", Map.of()));
        assertTrue(LaunchRules.allows(null, "windows", "amd64", "10.0", Map.of()));
        assertFalse(LaunchRules.allows(new JsonArray(), "windows", "amd64", "10.0", Map.of()));
        JsonArray feature = JsonParser.parseString("""
            [{"action":"allow","features":{"is_demo_user":true}}]
            """).getAsJsonArray();
        assertFalse(LaunchRules.allows(feature, "linux", "amd64", "6", Map.of()));
        assertTrue(LaunchRules.allows(feature, "linux", "amd64", "6", Map.of("is_demo_user", true)));
    }

    @Test
    public void preservesTokensAndSubstitutesWithoutSplittingPathsOrEquals() {
        JsonArray array = JsonParser.parseString("""
            ["--username", "${name}", "-Dtest=a=b", "-cp", "${classpath}",
             {"rules":[{"action":"allow","features":{"is_demo_user":true}}],"value":"--demo"},
             {"rules":[{"action":"allow"}],"value":["--width","800"]}]
            """).getAsJsonArray();
        assertEquals(List.of("--username", "Test User", "-Dtest=a=b", "-cp", "C:\\game dir\\a.jar",
            "--width", "800"),
            CLIArguments.fromJsonArray(array).resolve(
                Map.of("name", "Test User", "classpath", "C:\\game dir\\a.jar")));
        assertThrows(IllegalArgumentException.class, () -> CLIArguments.substitute("${missing}", Map.of()));
    }

    @Test
    public void readsJvmFromArgumentsAndAcceptsDownloadsWithoutMappings() {
        JsonObject json = JsonParser.parseString("""
            {"arguments":{"game":[],"jvm":["-Dtest=true"]},
             "assetIndex":{"id":"test"},"assets":"test","complianceLevel":1,
             "downloads":{"client":{"url":"file:/client.jar"},"server":{"url":"file:/server.jar"}},
             "id":"26.3","javaVersion":{"majorVersion":25},"libraries":[],
             "logging":{"client":{"argument":"test","file":{},"type":"test"}},
             "mainClass":"test.Main","minimumLauncherVersion":21,
             "releaseTime":"test","time":"test","type":"release"}
            """).getAsJsonObject();
        var version = VersionPackage.fromJson(json);
        assertEquals(List.of("-Dtest=true"), version.arguments().jvm().resolve(Map.of()));
        assertNotNull(Downloads.fromJson(json.getAsJsonObject("downloads")).client());
        assertNotNull(version.downloads().server());
        assertEquals(25, version.javaVersion().majorVersion());
    }
}
