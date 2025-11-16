package fr.matissead.pluginmanagerweb;

import fr.matissead.pluginmanagerweb.api.PluginManagerWebAPI;
import fr.matissead.pluginmanagerweb.config.PluginManagerConfig;
import fr.matissead.pluginmanagerweb.github.GitHubClient;
import fr.matissead.pluginmanagerweb.metrics.PluginMetricsService;
import fr.matissead.pluginmanagerweb.persistence.AuditLogDao;
import fr.matissead.pluginmanagerweb.persistence.ConfigBackupDao;
import fr.matissead.pluginmanagerweb.persistence.DataSourceFactory;
import fr.matissead.pluginmanagerweb.security.TokenService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * Main plugin class for PluginManagerWeb.
 * <p>
 * Initializes all services and starts the embedded web server.
 * Provides dependency injection for all components.
 */
public class PluginManagerWeb extends JavaPlugin implements PluginManagerWebAPI {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private WebServer webServer;
    private PluginManagerConfig pluginConfig;
    private DataSourceFactory dataSourceFactory;
    private TokenService tokenService;
    private PluginMetricsService metricsService;
    private GitHubClient githubClient;
    private AuditLogDao auditLogDao;
    private ConfigBackupDao configBackupDao;

    @Override
    public void onEnable() {
        try {
            // Load configuration
            saveDefaultConfig();
            File configFile = new File(getDataFolder(), "config.yml");
            FileConfiguration rawConfig = YamlConfiguration.loadConfiguration(configFile);
            
            try {
                pluginConfig = new PluginManagerConfig(rawConfig);
            } catch (IllegalStateException e) {
                logger.error("Configuration error: {}", e.getMessage());
                logger.error("Please fix your configuration and restart the server.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            if (!pluginConfig.isEnabled()) {
                logger.info("PluginManagerWeb disabled via configuration.");
                return;
            }

            // Initialize database
            dataSourceFactory = new DataSourceFactory(pluginConfig.getDatabaseConfig(), getDataFolder());
            auditLogDao = new AuditLogDao(dataSourceFactory.getDataSource());
            configBackupDao = new ConfigBackupDao(dataSourceFactory.getDataSource());

            // Initialize services
            tokenService = new TokenService(pluginConfig.getAuthConfig());
            metricsService = new PluginMetricsService(dataSourceFactory.getDataSource());
            githubClient = new GitHubClient(pluginConfig.getGithubConfig());

            // Start web server
            webServer = new WebServer(
                this,
                pluginConfig,
                tokenService,
                metricsService,
                githubClient,
                auditLogDao,
                configBackupDao
            );
            webServer.start();
            
            logger.info("PluginManagerWeb started successfully");
            logger.info("Web server: http://{}:{}/", 
                pluginConfig.getWebConfig().getBindAddress(), 
                pluginConfig.getWebConfig().getPort());
            logger.info("Use token '{}...' to authenticate", 
                pluginConfig.getAuthConfig().getAdminToken().substring(0, Math.min(8, pluginConfig.getAuthConfig().getAdminToken().length())));

        } catch (Exception e) {
            logger.error("Failed to start PluginManagerWeb", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Stop web server
        if (webServer != null) {
            webServer.stop();
        }

        // Close database connections
        if (dataSourceFactory != null) {
            dataSourceFactory.close();
        }

        logger.info("PluginManagerWeb stopped");
    }

    // Public API implementation for other plugins

    @Override
    public void incrementCounter(String pluginName, String metricKey, long delta) {
        if (metricsService != null) {
            metricsService.incrementCounter(pluginName, metricKey, delta);
        }
    }

    @Override
    public void recordEvent(String pluginName, String eventType, Map<String, Object> payload) {
        if (metricsService != null) {
            metricsService.recordEvent(pluginName, eventType, payload);
        }
    }

    @Override
    public void setGauge(String pluginName, String metricKey, Object value) {
        if (metricsService != null) {
            metricsService.setGauge(pluginName, metricKey, value);
        }
    }

    @Override
    public Map<String, Object> getMetrics(String pluginName) {
        if (metricsService != null) {
            return metricsService.getMetrics(pluginName);
        }
        return Map.of();
    }

    @Override
    public void clearMetrics(String pluginName) {
        if (metricsService != null) {
            metricsService.clearMetrics(pluginName);
        }
    }

    /**
     * Returns the public API instance for other plugins.
     */
    public PluginManagerWebAPI getAPI() {
        return this;
    }

    /**
     * Returns the web server instance.
     */
    public WebServer getWebServer() {
        return webServer;
    }
}
