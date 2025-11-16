package fr.matissead.pluginmanagerweb;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsConnectContext;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Wrapper autour de Javalin pour démarrer et arrêter un serveur web embarqué.
 * <p>
 * Ce serveur expose une API REST minimaliste et un endpoint WebSocket. Les routes
 * sont définies dans des méthodes dédiées pour une meilleure lisibilité.
 */
public class WebServer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PluginManagerWeb plugin;
    private final Javalin app;
    private final FileConfiguration config;

    public WebServer(PluginManagerWeb plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        this.app = Javalin.create(config -> {
            // Activer les CORS si nécessaire en fonction de la configuration
            config.http.defaultContentType = "application/json";
        });
        registerRoutes();
    }

    /**
     * Démarre le serveur web sur l’adresse et le port définis dans la configuration.
     */
    public void start() {
        ConfigurationSection web = config.getConfigurationSection("pluginmanager.web");
        String host = web.getString("bind_address", "0.0.0.0");
        int port = web.getInt("port", 8080);
        app.start(host, port);
    }

    /**
     * Arrête le serveur web.
     */
    public void stop() {
        try {
            app.stop();
        } catch (Exception e) {
            logger.error("Erreur lors de l’arrêt du serveur web", e);
        }
    }

    /**
     * Enregistre les routes REST et WebSocket. Pour l’instant, seul un endpoint de
     * démonstration est exposé. Les fonctionnalités complètes devront être
     * implémentées (liste des plugins, actions d’administration, etc.).
     */
    private void registerRoutes() {
        // Endpoint de santé
        app.get("/api/health", ctx -> ctx.json(Map.of(
                "status", "ok",
                "server", Bukkit.getVersion()
        )));

        // Endpoint pour lister les plugins installés (squelette)
        app.get("/api/plugins", this::handleListPlugins);

        // WebSocket pour démonstration (push d’événements)
        app.ws("/ws/events", ws -> {
            ws.onConnect(this::handleWsConnect);
            ws.onClose(ctx -> {
                // Aucun traitement pour l’instant
            });
        });
    }

    /**
     * Gère la réponse JSON pour la liste des plugins installés.
     * <p>
     * Cette implémentation parcourt les plugins chargés et retourne un tableau
     * contenant leur nom, leur version et s’ils sont activés.
     */
    private void handleListPlugins(Context ctx) {
        PluginManager pluginManager = Bukkit.getPluginManager();
        List<Map<String, Object>> entries = pluginManager.getPlugins().stream().map(plugin -> Map.of(
                "name", plugin.getName(),
                "version", plugin.getDescription().getVersion(),
                "enabled", plugin.isEnabled(),
                "authors", plugin.getDescription().getAuthors()
        )).toList();
        ctx.json(Map.of(
                "plugins", entries
        ));
    }

    /**
     * Handler appelé lorsqu’un client se connecte au WebSocket. Dans cette
     * implémentation de base, un message de bienvenue est envoyé.
     */
    private void handleWsConnect(WsConnectContext ctx) {
        ctx.send("Connecté à PluginManagerWeb.");
    }
}
