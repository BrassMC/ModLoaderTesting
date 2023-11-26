package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.mappings.MappingFile;
import dev.turtywurty.testgradleplugin.mappings.OfficialMappingsFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

@CacheableTask
public abstract class RemapClassesTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void remapClasses() {
        System.out.println("Remapping classes!");

        Path versionPath = getOutputDir()
                .getOrElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .getAsFile()
                .toPath()
                .resolve(getVersion().get());

        Path clientJarPath = versionPath.resolve("client.jar");
        if (Files.notExists(clientJarPath)) {
            throw new RuntimeException("client.jar is missing, please run the downloadClient task!");
        }

        Path serverJarPath = versionPath.resolve("server.jar");
        if (Files.notExists(serverJarPath)) {
            throw new RuntimeException("server.jar is missing, please run the downloadServer task!");
        }

        Path clientMappingsPath = versionPath.resolve("client_mappings.txt");
        if (Files.notExists(clientMappingsPath)) {
            throw new RuntimeException("client_mappings.txt is missing, please run the downloadClientMappings task!");
        }

        Path serverMappingsPath = versionPath.resolve("server_mappings.txt");
        if (Files.notExists(serverMappingsPath)) {
            throw new RuntimeException("server_mappings.txt is missing, please run the downloadServerMappings task!");
        }

        Path remappedClientJarPath = versionPath.resolve("client-remapped.jar");
        Path remappedServerJarPath = versionPath.resolve("server-remapped.jar");
        System.out.println("Remapped client jar: " + remappedClientJarPath);
        System.out.println("Remapped server jar: " + remappedServerJarPath);

        // parse mappings
        var serverMappings = new OfficialMappingsFile(serverMappingsPath);
        // serverMappings.getCachedTree().print();

        var clientMappings = new OfficialMappingsFile(clientMappingsPath);
        // clientMappings.getCachedTree().print();

        // remap jars
        remapJar(clientJarPath, remappedClientJarPath, clientMappings);
        remapJar(serverJarPath, remappedServerJarPath, serverMappings);
    }

    private static void remapJar(Path inputJar, Path outputJar, MappingFile mappings) {
        try(JarFile jarFile = new JarFile(inputJar.toFile())) {
            var outputStream = new JarOutputStream(Files.newOutputStream(outputJar));

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if(entry.isDirectory())
                    continue;
                if(!entry.getName().endsWith(".class"))
                    continue;

                try(InputStream stream = jarFile.getInputStream(entry)) {
                    String packageName = mappings.findPackage(entry.getName());
                    String remappedFileName = mappings.remapClass(entry.getName());
                    String remappedEntryName = packageName + "/" + remappedFileName;

                    System.out.println("Remapped " + entry.getName() + " to " + remappedEntryName);

                    var newEntry = new JarEntry(remappedEntryName);
                    outputStream.putNextEntry(newEntry);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = stream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                outputStream.closeEntry();
            }

            outputStream.close();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to remap jar!", exception);
        }
    }
}
