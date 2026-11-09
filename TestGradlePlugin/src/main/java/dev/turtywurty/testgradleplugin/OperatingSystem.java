package dev.turtywurty.testgradleplugin;

import java.nio.file.Path;
import java.util.Locale;

public enum OperatingSystem {
    WINDOWS("win"),
    OSX("mac", "macos", "darwin"),
    LINUX("unix"),
    UNKNOWN;

    private final String name;
    private final String[] aliases;

    OperatingSystem(String... aliases) {
        this.name = name().toLowerCase(Locale.ROOT);
        this.aliases = aliases == null ? new String[0] : aliases;
    }

    public String getName() {
        return this.name;
    }

    public String[] getAliases() {
        return this.aliases;
    }

    public static OperatingSystem determineCurrentOperatingSystem() {
        return fromName(System.getProperty("os.name"));
    }

    public static OperatingSystem fromName(String name) {
        String osName = name.toLowerCase(Locale.ROOT);
        if (osName.contains("darwin"))
            return OSX;

        for (OperatingSystem operatingSystem : values()) {
            if (osName.contains(operatingSystem.name))
                return operatingSystem;

            for (String alias : operatingSystem.aliases) {
                if (osName.contains(alias))
                    return operatingSystem;
            }
        }

        return UNKNOWN;
    }

    public static Path getMinecraftDir(OperatingSystem operatingSystem) {
        return switch (operatingSystem) {
            case WINDOWS -> Path.of(System.getenv("APPDATA"), ".minecraft");
            case OSX -> Path.of(System.getProperty("user.home"), "Library", "Application Support", "minecraft");
            case LINUX, UNKNOWN -> Path.of(System.getProperty("user.home"), ".minecraft");
        };
    }

    public static Path getMinecraftDir() {
        return getMinecraftDir(determineCurrentOperatingSystem());
    }
}
