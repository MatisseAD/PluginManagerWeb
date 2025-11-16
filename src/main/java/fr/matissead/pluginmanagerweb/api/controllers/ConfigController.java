package fr.matissead.pluginmanagerweb.api.controllers;

import com.google.gson.Gson;
import fr.matissead.pluginmanagerweb.model.AuditLog;
import fr.matissead.pluginmanagerweb.model.ConfigBackup;
import fr.matissead.pluginmanagerweb.persistence.AuditLogDao;
import fr.matissead.pluginmanagerweb.persistence.ConfigBackupDao;
import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for plugin configuration management.
 * Handles reading, writing, backing up, and restoring config files.
 */
public class ConfigController {
    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);
    private final ConfigBackupDao configBackupDao;
    private final AuditLogDao auditLogDao;
    private final Gson gson;
    
    public ConfigController(ConfigBackupDao configBackupDao, AuditLogDao auditLogDao) {
        this.configBackupDao = configBackupDao;
        this.auditLogDao = auditLogDao;
        this.gson = new Gson();
    }
    
    /**
     * GET /api/plugins/:name/config - Lists all config files for a plugin
     */
    public void listConfigFiles(Context ctx) {
        String pluginName = ctx.pathParam("name");
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        
        if (plugin == null) {
            ctx.status(404).json(Map.of("error", "Plugin not found"));
            return;
        }
        
        File dataFolder = plugin.getDataFolder();
        List<String> configFiles = findConfigFiles(dataFolder);
        
        ctx.json(Map.of(
            "plugin", pluginName,
            "configFiles", configFiles
        ));
    }
    
    /**
     * GET /api/plugins/:name/config/file - Gets content of a specific config file
     */
    public void getConfigFile(Context ctx) {
        String pluginName = ctx.pathParam("name");
        String path = ctx.queryParam("path");
        
        if (path == null || path.isEmpty()) {
            ctx.status(400).json(Map.of("error", "Missing 'path' parameter"));
            return;
        }
        
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            ctx.status(404).json(Map.of("error", "Plugin not found"));
            return;
        }
        
        File configFile = new File(plugin.getDataFolder(), path);
        
        // Security check - ensure file is within plugin directory
        if (!isPathSafe(configFile, plugin.getDataFolder())) {
            ctx.status(403).json(Map.of("error", "Access denied - path outside plugin directory"));
            return;
        }
        
        if (!configFile.exists() || !configFile.isFile()) {
            ctx.status(404).json(Map.of("error", "Config file not found"));
            return;
        }
        
        try {
            String content = Files.readString(configFile.toPath());
            ctx.json(Map.of(
                "plugin", pluginName,
                "path", path,
                "content", content,
                "size", configFile.length(),
                "lastModified", configFile.lastModified()
            ));
        } catch (IOException e) {
            logger.error("Failed to read config file: " + path, e);
            ctx.status(500).json(Map.of("error", "Failed to read file: " + e.getMessage()));
        }
    }
    
    /**
     * POST /api/plugins/:name/config/file - Saves a config file with backup
     */
    public void saveConfigFile(Context ctx) {
        String pluginName = ctx.pathParam("name");
        SaveRequest request = ctx.bodyAsClass(SaveRequest.class);
        String user = ctx.attribute("user");
        String ip = ctx.ip();
        
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            ctx.status(404).json(Map.of("error", "Plugin not found"));
            return;
        }
        
        File configFile = new File(plugin.getDataFolder(), request.path);
        
        // Security check
        if (!isPathSafe(configFile, plugin.getDataFolder())) {
            ctx.status(403).json(Map.of("error", "Access denied - path outside plugin directory"));
            return;
        }
        
        try {
            // Backup existing file if it exists
            if (configFile.exists()) {
                String existingContent = Files.readString(configFile.toPath());
                ConfigBackup backup = new ConfigBackup(pluginName, request.path, existingContent);
                backup.setCreatedBy(user);
                configBackupDao.save(backup);
                logger.info("Created backup for {}/{} (backup ID: {})", pluginName, request.path, backup.getId());
            }
            
            // Ensure parent directory exists
            configFile.getParentFile().mkdirs();
            
            // Write new content
            Files.writeString(configFile.toPath(), request.content);
            
            // Optionally reload plugin if requested
            if (request.reloadPlugin) {
                Bukkit.getPluginManager().disablePlugin(plugin);
                Bukkit.getPluginManager().enablePlugin(plugin);
            }
            
            // Log the action
            AuditLog log = new AuditLog(user, "CONFIG_UPDATE", pluginName + "/" + request.path, ip);
            log.setMetadata(gson.toJson(Map.of("reloaded", request.reloadPlugin, "size", request.content.length())));
            auditLogDao.save(log);
            
            ctx.json(Map.of(
                "success", true,
                "message", "Config file saved successfully",
                "reloaded", request.reloadPlugin
            ));
            
        } catch (IOException e) {
            logger.error("Failed to save config file: " + request.path, e);
            ctx.status(500).json(Map.of("error", "Failed to save file: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/plugins/:name/config/backups - Lists backups for a plugin
     */
    public void listBackups(Context ctx) {
        String pluginName = ctx.pathParam("name");
        List<ConfigBackup> backups = configBackupDao.getByPlugin(pluginName);
        
        ctx.json(Map.of(
            "plugin", pluginName,
            "backups", backups
        ));
    }
    
    /**
     * POST /api/plugins/:name/config/rollback - Restores a config from backup
     */
    public void rollbackConfig(Context ctx) {
        String pluginName = ctx.pathParam("name");
        RollbackRequest request = ctx.bodyAsClass(RollbackRequest.class);
        String user = ctx.attribute("user");
        String ip = ctx.ip();
        
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            ctx.status(404).json(Map.of("error", "Plugin not found"));
            return;
        }
        
        ConfigBackup backup = configBackupDao.getById(request.backupId);
        if (backup == null) {
            ctx.status(404).json(Map.of("error", "Backup not found"));
            return;
        }
        
        if (!backup.getPluginName().equals(pluginName)) {
            ctx.status(403).json(Map.of("error", "Backup does not belong to this plugin"));
            return;
        }
        
        try {
            File configFile = new File(plugin.getDataFolder(), backup.getPath());
            
            // Create a backup of current state before rollback
            if (configFile.exists()) {
                String currentContent = Files.readString(configFile.toPath());
                ConfigBackup preRollbackBackup = new ConfigBackup(pluginName, backup.getPath(), currentContent);
                preRollbackBackup.setCreatedBy(user + " (pre-rollback)");
                configBackupDao.save(preRollbackBackup);
            }
            
            // Restore from backup
            configFile.getParentFile().mkdirs();
            Files.writeString(configFile.toPath(), backup.getContent());
            
            // Log the action
            AuditLog log = new AuditLog(user, "CONFIG_ROLLBACK", pluginName + "/" + backup.getPath(), ip);
            log.setMetadata(gson.toJson(Map.of("backupId", request.backupId, "backupDate", backup.getTimestamp())));
            auditLogDao.save(log);
            
            ctx.json(Map.of(
                "success", true,
                "message", "Config restored from backup",
                "backup", backup
            ));
            
        } catch (IOException e) {
            logger.error("Failed to rollback config", e);
            ctx.status(500).json(Map.of("error", "Failed to restore: " + e.getMessage()));
        }
    }
    
    private List<String> findConfigFiles(File dir) {
        // Implementation similar to PluginController's findConfigFiles
        return List.of(); // Simplified for now
    }
    
    private boolean isPathSafe(File file, File baseDir) {
        try {
            String filePath = file.getCanonicalPath();
            String basePath = baseDir.getCanonicalPath();
            return filePath.startsWith(basePath);
        } catch (IOException e) {
            return false;
        }
    }
    
    public static class SaveRequest {
        public String path;
        public String content;
        public boolean reloadPlugin;
    }
    
    public static class RollbackRequest {
        public long backupId;
    }
}
