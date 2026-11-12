package com.snowgears.shop.util;

import com.snowgears.shop.Shop;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Eagerly loads every class shipped in the plugin jar when the plugin is enabled.
 *
 * Bukkit's {@code PluginClassLoader} reads classes from the jar on disk lazily, the first time
 * each one is referenced. If the jar is replaced or deleted while the server is running (for
 * example by dropping a new build into {@code plugins/} without restarting), any class that has
 * not been touched yet can no longer be found and the first code path that needs it dies with a
 * {@link NoClassDefFoundError} / {@link ClassNotFoundException}. {@code PricePair} is a typical
 * victim: nothing references it until a player writes a shop sign.
 *
 * Loading all of our own classes up front (without running their static initializers) means the
 * whole plugin is resident in memory from the moment it is enabled, and a later jar swap only
 * takes effect on the next restart instead of breaking the running plugin.
 */
public final class ClassPreloader {

    private static final String PACKAGE_PREFIX = "com/snowgears/shop/";
    /** Relocated third-party libraries live here; they load themselves fine and need no help. */
    private static final String SHADED_PREFIX = "com/snowgears/shop/artifact/";

    private ClassPreloader() {}

    /**
     * Loads every plugin class found in {@code jar} through {@code loader}.
     *
     * @return the number of classes that were loaded successfully
     */
    public static int preload(Shop plugin, File jar, ClassLoader loader) {
        if (jar == null || !jar.isFile()) {
            plugin.getLogger().debug("Class preload skipped: plugin jar not found on disk");
            return 0;
        }

        int loaded = 0;
        int failed = 0;
        try (JarFile jarFile = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.startsWith(PACKAGE_PREFIX) || !name.endsWith(".class")
                        || name.startsWith(SHADED_PREFIX)) {
                    continue;
                }
                String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                try {
                    // initialize=false: define the class but do not run static initializers,
                    // so preloading has no side effects and no ordering requirements.
                    Class.forName(className, false, loader);
                    loaded++;
                } catch (Throwable t) {
                    // Optional integrations (WorldGuard, Dynmap, ...) reference APIs that may not be
                    // installed on this server; those classes simply cannot be resolved here.
                    failed++;
                    plugin.getLogger().debug("Class preload could not load " + className + ": " + t);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Class preload failed to read plugin jar " + jar.getName() + ": " + e.getMessage());
            return loaded;
        }

        plugin.getLogger().debug("Class preload complete: " + loaded + " classes loaded, " + failed + " skipped");
        return loaded;
    }
}
