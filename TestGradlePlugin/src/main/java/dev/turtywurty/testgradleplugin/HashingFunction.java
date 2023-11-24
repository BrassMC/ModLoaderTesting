package dev.turtywurty.testgradleplugin;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public enum HashingFunction {
    MD5("MD5", 32),
    SHA1("SHA-1", 40),
    SHA256("SHA-256", 64),
    SHA512("SHA-512", 128);

    private final String algorithm;
    private final String padding;

    HashingFunction(String algorithm, int length) {
        this.algorithm = algorithm;
        this.padding = String.format(Locale.ROOT, "%0" + length + "d", 0);
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public String getPadding() {
        return this.padding;
    }

    public String asFileExtension() {
        return "." + this.algorithm.toLowerCase(Locale.ROOT);
    }

    public MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(this.algorithm);
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("Failed to get message digest for " + this.algorithm + "!", exception);
        }
    }

    public String hash(Iterable<Path> paths) {
        MessageDigest digest = getMessageDigest();

        for (Path path : paths) {
            try {
                if (Files.notExists(path))
                    continue;

                digest.update(Files.readAllBytes(path));
            } catch (IOException exception) {
                throw new RuntimeException("Failed to read bytes from " + path + "!", exception);
            }
        }

        return pad(toHexString(digest.digest()));
    }

    public String hash(InputStream stream) {
        MessageDigest digest = getMessageDigest();

        try {
            digest.update(stream.readAllBytes());
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read bytes from stream!", exception);
        }

        return pad(toHexString(digest.digest()));
    }

    public String hash(@Nullable String data) {
        return hash(data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8));
    }

    public String hash(Path path) {
        try {
            return hash(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read bytes from " + path + "!", exception);
        }
    }

    public String hash(byte[] data) {
        return pad(toHexString(getMessageDigest().digest(data)));
    }

    private String pad(String hash) {
        return (this.padding + hash).substring(hash.length());
    }

    private static String toHexString(byte[] data) {
        return new BigInteger(1, data).toString(16);
    }
}
