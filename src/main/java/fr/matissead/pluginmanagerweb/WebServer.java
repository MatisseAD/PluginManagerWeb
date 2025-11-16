package fr.matissead.pluginmanagerweb;

import com.google.gson.Gson;
import fr.matissead.pluginmanagerweb.api.controllers.*;
import fr.matissead.pluginmanagerweb.api.websocket.EventsWebSocketHandler;
import fr.matissead.pluginmanagerweb.config.PluginManagerConfig;
import fr.matissead.pluginmanagerweb.github.GitHubClient;
import fr.matissead.pluginmanagerweb.metrics.PluginMetricsService;
import fr.matissead.pluginmanagerweb.persistence.AuditLogDao;
import fr.matissead.pluginmanagerweb.persistence.ConfigBackupDao;
import fr.matissead.pluginmanagerweb.security.AuthMiddleware;
import fr.matissead.pluginmanagerweb.security.TokenService;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Web server wrapper managing Javalin and all REST/WebSocket routes.
 * Configures security, static file serving, and exception handling.
 */
public class WebServer {

    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    private final PluginManagerWeb plugin;
    private final PluginManagerConfig config;
    private final Javalin app;
    private final Gson gson;
    
    // Controllers
    private final ServerController serverController;
    private final PluginController pluginController;
    private final ConfigController configController;
    private final MetricsController metricsController;
    
    // WebSocket handlers
    private final EventsWebSocketHandler eventsHandler;
    
    // Middleware
    private final AuthMiddleware authMiddleware;

    public WebServer(PluginManagerWeb plugin, PluginManagerConfig config, TokenService tokenService,
                    PluginMetricsService metricsService, GitHubClient githubClient,
                    AuditLogDao auditLogDao, ConfigBackupDao configBackupDao) {
        this.plugin = plugin;
        this.config = config;
        this.gson = new Gson();
        
        // Initialize controllers
        this.serverController = new ServerController();
        this.pluginController = new PluginController(metricsService, githubClient, 
                                                      config.getGithubConfig(), auditLogDao);
        this.configController = new ConfigController(configBackupDao, auditLogDao);
        this.metricsController = new MetricsController(metricsService);
        
        // Initialize WebSocket handlers
        this.eventsHandler = new EventsWebSocketHandler();
        
        // Initialize middleware
        this.authMiddleware = new AuthMiddleware(tokenService, config.getWebConfig(), auditLogDao);
        
        // Create Javalin app
        this.app = Javalin.create(javalinConfig -> {
            javalinConfig.http.defaultContentType = "application/json";
            javalinConfig.http.maxRequestSize = 10_000_000L; // 10MB for config files
            
            // Set up static file serving for frontend
            Path webDir = plugin.getDataFolder().toPath().resolve("web");
            try {
                if (!Files.exists(webDir)) {
                    Files.createDirectories(webDir);
                    logger.info("Created web directory: {}", webDir);
                }
                javalinConfig.staticFiles.add(webDir.toString(), Location.EXTERNAL);
            } catch (IOException e) {
                logger.error("Failed to create web directory", e);
            }
            
            // Enable CORS for development
            javalinConfig.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
        });
        
        registerRoutes();
        registerExceptionHandlers();
    }

    private void registerRoutes() {
        // Public routes (no authentication required)
        app.get("/api/health", serverController::health);
        
        // Authenticated routes - server info
        app.get("/api/server", authMiddleware, serverController::serverInfo);
        
        // Authenticated routes - plugins
        app.get("/api/plugins", authMiddleware, pluginController::listPlugins);
        app.get("/api/plugins/{name}", authMiddleware, pluginController::getPlugin);
        app.post("/api/plugins/{name}/action", authMiddleware, pluginController::performAction);
        app.get("/api/plugins/{name}/releases", authMiddleware, pluginController::getReleases);
        
        // Authenticated routes - configuration
        app.get("/api/plugins/{name}/config", authMiddleware, configController::listConfigFiles);
        app.get("/api/plugins/{name}/config/file", authMiddleware, configController::getConfigFile);
        app.post("/api/plugins/{name}/config/file", authMiddleware, configController::saveConfigFile);
        app.get("/api/plugins/{name}/config/backups", authMiddleware, configController::listBackups);
        app.post("/api/plugins/{name}/config/rollback", authMiddleware, configController::rollbackConfig);
        
        // Authenticated routes - metrics
        app.get("/api/plugins/{name}/metrics", authMiddleware, metricsController::getPluginMetrics);
        app.get("/api/metrics/overview", authMiddleware, metricsController::getMetricsOverview);
        
        // WebSocket - events (consider adding auth here too)
        app.ws("/ws/events", ws -> {
            ws.onConnect(eventsHandler::onConnect);
            ws.onClose(eventsHandler::onClose);
            ws.onMessage(eventsHandler::onMessage);
        });
        
        // Root route - serve dashboard or redirect to index.html
        app.get("/", ctx -> {
            ctx.html("<!DOCTYPE html><html><head><title>PluginManagerWeb</title></head>" +
                    "<body><h1>PluginManagerWeb</h1><p>Dashboard coming soon! Use the API endpoints for now.</p>" +
                    "<p>API Health: <a href='/api/health'>/api/health</a></p></body></html>");
        });
    }

    private void registerExceptionHandlers() {
        // Global exception handler
        app.exception(Exception.class, (e, ctx) -> {
            logger.error("Unhandled exception in request: {} {}", ctx.method(), ctx.path(), e);
            ctx.status(500).json(new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An internal error occurred: " + e.getMessage()
            ));
        });
        
        // 404 handler
        app.error(404, ctx -> {
            ctx.json(new ErrorResponse(
                "NOT_FOUND",
                "The requested endpoint does not exist"
            ));
        });
        
        // 401 handler
        app.error(401, ctx -> {
            ctx.json(new ErrorResponse(
                "UNAUTHORIZED",
                "Authentication required"
            ));
        });
    }

    public void start() {
        int port = config.getWebConfig().getPort();
        String bindAddress = config.getWebConfig().getBindAddress();
        app.start(bindAddress, port);
        logger.info("Web server started on {}:{}", bindAddress, port);
    }

    public void stop() {
        try {
            app.stop();
            logger.info("Web server stopped");
        } catch (Exception e) {
            logger.error("Error stopping web server", e);
        }
    }

    public EventsWebSocketHandler getEventsHandler() {
        return eventsHandler;
    }

    /**
     * Simple error response class for JSON serialization.
     */
    public static class ErrorResponse {
        private final String error;
        private final String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }
    }
}
