package fr.matissead.pluginmanagerweb.api.controllers;

import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Map;

/**
 * REST API controller for server information.
 * Provides health checks and server stats.
 */
public class ServerController {
    
    /**
     * GET /api/health - Simple health check endpoint
     */
    public void health(Context ctx) {
        ctx.json(Map.of(
            "status", "ok",
            "serverVersion", Bukkit.getVersion(),
            "pluginVersion", "0.1.0",
            "time", Instant.now().toString(),
            "uptime", ManagementFactory.getRuntimeMXBean().getUptime()
        ));
    }
    
    /**
     * GET /api/server - Detailed server information
     */
    public void serverInfo(Context ctx) {
        Server server = Bukkit.getServer();
        Runtime runtime = Runtime.getRuntime();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        ctx.json(Map.of(
            "server", Map.of(
                "name", server.getName(),
                "version", server.getVersion(),
                "bukkitVersion", server.getBukkitVersion(),
                "minecraftVersion", server.getMinecraftVersion(),
                "motd", server.getMotd(),
                "port", server.getPort(),
                "maxPlayers", server.getMaxPlayers(),
                "viewDistance", server.getViewDistance()
            ),
            "players", Map.of(
                "online", server.getOnlinePlayers().size(),
                "max", server.getMaxPlayers()
            ),
            "worlds", Map.of(
                "count", server.getWorlds().size(),
                "names", server.getWorlds().stream().map(w -> w.getName()).toList()
            ),
            "memory", Map.of(
                "used", usedMemory,
                "free", freeMemory,
                "total", totalMemory,
                "max", maxMemory,
                "usedMB", usedMemory / (1024 * 1024),
                "totalMB", totalMemory / (1024 * 1024),
                "maxMB", maxMemory / (1024 * 1024)
            ),
            "tps", Map.of(
                "current", getCurrentTPS(),
                "description", "TPS calculation may vary by server implementation"
            ),
            "uptime", ManagementFactory.getRuntimeMXBean().getUptime(),
            "timestamp", Instant.now().toString()
        ));
    }
    
    private double getCurrentTPS() {
        // This is a simplified TPS calculation
        // In production, you'd want to use Paper's TPS API or track tick times
        // For now, we return a placeholder
        try {
            // Try to use Paper's TPS API if available
            Server server = Bukkit.getServer();
            double[] tps = server.getTPS();
            if (tps != null && tps.length > 0) {
                return Math.min(20.0, tps[0]);
            }
        } catch (Exception e) {
            // Not supported on this server version
        }
        return 20.0; // Assume 20 TPS if not available
    }
}
