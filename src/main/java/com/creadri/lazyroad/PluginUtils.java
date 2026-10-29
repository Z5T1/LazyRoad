package com.creadri.lazyroad;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.*;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginUtils {

    public static void extractResourceZip(JavaPlugin plugin, String resourceName, File destination) {
        try (InputStream is = plugin.getResource(resourceName)) {
            if (is == null) {
                plugin.getLogger().warning("Resource " + resourceName + " not found!");
                return;
            }

            try (ZipInputStream zis = new ZipInputStream(is)) {
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    try {
                        File f = new File(destination, ze.getName());
                        if (!f.toPath().normalize().startsWith(destination.toPath().normalize())) {
                            plugin.getLogger().warning("Blocked potential path traversal entry: " + ze.getName());
                            continue;
                        }

                        if (ze.isDirectory()) {
                            if (!f.exists()) {
                                f.mkdirs();
                            }
                            continue;
                        }

                        File parent = f.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }

                        byte[] content = zis.readAllBytes();

                        // If file already exists and content is identical, skip writing to avoid unnecessary I/O
                        // and avoid permission errors on read-only files.
                        if (f.exists()) {
                            if (f.length() == content.length) {
                                try {
                                    byte[] existing = Files.readAllBytes(f.toPath());
                                    if (Arrays.equals(content, existing)) {
                                        continue;
                                    }
                                } catch (Exception ignored) {
                                    // If reading fails, proceed to attempt writing
                                }
                            }

                            // Try making existing file writable in case permissions were restricted
                            if (!f.canWrite()) {
                                try {
                                    f.setWritable(true);
                                } catch (SecurityException ignored) {}
                            }
                        }

                        try (FileOutputStream fos = new FileOutputStream(f)) {
                            fos.write(content);
                        } catch (Exception writeEx) {
                            if (f.exists()) {
                                plugin.getLogger().warning("Could not update template '" + ze.getName() + "' (" + writeEx.getMessage() + ") - keeping existing file.");
                            } else {
                                plugin.getLogger().warning("Error extracting '" + ze.getName() + "': " + writeEx.getMessage());
                            }
                        }
                    } catch (Exception entryEx) {
                        plugin.getLogger().warning("Error processing entry '" + ze.getName() + "': " + entryEx.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Error extracting " + resourceName + ": " + ex.getMessage());
        }
    }
}
