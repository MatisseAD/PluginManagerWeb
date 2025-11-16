package fr.matissead.pluginmanagerweb.api.websocket;

import com.google.gson.Gson;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time event streaming to the dashboard.
 * Broadcasts plugin state changes, new releases, and system events.
 */
public class EventsWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(EventsWebSocketHandler.class);
    private final Set<WsConnectContext> connections = ConcurrentHashMap.newKeySet();
    private final Gson gson = new Gson();
    
    public void onConnect(WsConnectContext ctx) {
        connections.add(ctx);
        logger.info("WebSocket client connected from {}", ctx.session.getRemoteAddress());
        
        // Send welcome message
        sendToClient(ctx, new WebSocketMessage(
            "connected",
            Map.of(
                "message", "Connected to PluginManagerWeb events",
                "timestamp", Instant.now().toString()
            )
        ));
    }
    
    public void onClose(WsCloseContext ctx) {
        connections.remove(ctx);
        logger.info("WebSocket client disconnected from {}", ctx.session.getRemoteAddress());
    }
    
    public void onMessage(WsMessageContext ctx) {
        // Handle incoming messages if needed (e.g., subscription to specific events)
        logger.debug("Received WebSocket message: {}", ctx.message());
    }
    
    /**
     * Broadcasts a plugin state change event to all connected clients.
     */
    public void broadcastPluginStateChange(String pluginName, String state, boolean enabled) {
        WebSocketMessage message = new WebSocketMessage(
            "plugin_state_change",
            Map.of(
                "plugin", pluginName,
                "state", state,
                "enabled", enabled,
                "timestamp", Instant.now().toString()
            )
        );
        broadcast(message);
    }
    
    /**
     * Broadcasts a new release notification.
     */
    public void broadcastNewRelease(String pluginName, String version, String repo) {
        WebSocketMessage message = new WebSocketMessage(
            "new_release",
            Map.of(
                "plugin", pluginName,
                "version", version,
                "repo", repo,
                "timestamp", Instant.now().toString()
            )
        );
        broadcast(message);
    }
    
    /**
     * Broadcasts a system error or warning.
     */
    public void broadcastError(String severity, String message, String details) {
        WebSocketMessage wsMessage = new WebSocketMessage(
            "error",
            Map.of(
                "severity", severity,
                "message", message,
                "details", details,
                "timestamp", Instant.now().toString()
            )
        );
        broadcast(wsMessage);
    }
    
    /**
     * Broadcasts a log line to connected clients (for log streaming).
     */
    public void broadcastLog(String pluginName, String level, String message) {
        WebSocketMessage wsMessage = new WebSocketMessage(
            "log",
            Map.of(
                "plugin", pluginName,
                "level", level,
                "message", message,
                "timestamp", Instant.now().toString()
            )
        );
        broadcast(wsMessage);
    }
    
    private void broadcast(WebSocketMessage message) {
        String json = gson.toJson(message);
        connections.forEach(ctx -> {
            try {
                ctx.send(json);
            } catch (Exception e) {
                logger.error("Failed to send WebSocket message to client", e);
            }
        });
    }
    
    private void sendToClient(WsConnectContext ctx, WebSocketMessage message) {
        try {
            ctx.send(gson.toJson(message));
        } catch (Exception e) {
            logger.error("Failed to send WebSocket message to client", e);
        }
    }
    
    public int getConnectionCount() {
        return connections.size();
    }

    /**
         * Simple message structure for WebSocket events.
         */
        public record WebSocketMessage(String type, Map<String, Object> payload) {
    }
}
