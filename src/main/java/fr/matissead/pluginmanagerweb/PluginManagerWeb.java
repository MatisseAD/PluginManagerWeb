package fr.matissead.pluginmanagerweb;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Classe principale de PluginManagerWeb.
 * <p>
 * Lorsqu’elle est chargée par le serveur Paper, cette classe lit la configuration,
 * initialise le serveur web embarqué et expose des services internes pour les autres plugins.
 */
public class PluginManagerWeb extends JavaPlugin {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private WebServer webServer;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        // Charger ou créer la configuration
        saveDefaultConfig();
        File configFile = new File(getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        // Vérifier si le plugin doit activer le serveur web
        boolean enabled = config.getBoolean("pluginmanager.enabled", true);
        if (!enabled) {
            logger.info("PluginManagerWeb désactivé via la configuration.");
            return;
        }

        // Démarrage du serveur web
        try {
            webServer = new WebServer(this, config);
            webServer.start();
            logger.info("PluginManagerWeb webserver démarré sur {}:{}", config.getConfigurationSection("pluginmanager.web").getString("bind_address", "0.0.0.0"), config.getConfigurationSection("pluginmanager.web").getInt("port", 8080));
        } catch (Exception e) {
            logger.error("Impossible de démarrer le serveur web", e);
        }
    }

    @Override
    public void onDisable() {
        // Arrêter proprement le serveur web
        if (webServer != null) {
            webServer.stop();
        }
    }

    /**
     * Retourne la configuration chargée du plugin.
     */
    public FileConfiguration getPluginConfig() {
        return config;
    }

    /**
     * Fournit l’accès à l’instance du serveur web pour les autres composants internes.
     */
    public WebServer getWebServer() {
        return webServer;
    }
}
