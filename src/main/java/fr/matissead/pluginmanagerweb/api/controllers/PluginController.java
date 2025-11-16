package fr.matissead.pluginmanagerweb.api.controllers;

import com.google.gson.Gson;
import fr.matissead.pluginmanagerweb.config.GitHubConfig;
import fr.matissead.pluginmanagerweb.github.GitHubClient;
import fr.matissead.pluginmanagerweb.metrics.PluginMetricsService;
import fr.matissead.pluginmanagerweb.model.AuditLog;
import fr.matissead.pluginmanagerweb.model.PluginEntry;
import fr.matissead.pluginmanagerweb.model.ReleaseEntry;
import fr.matissead.pluginmanagerweb.persistence.AuditLogDao;
import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for plugin management.
 * Handles listing plugins, enabling/disabling, and retrieving plugin details.
 */
public class PluginController {
    private static final Logger logger = LoggerFactory.getLogger(PluginController.class);
    private final PluginMetricsService metricsService;
    private final GitHubClient githubClient;
    private final GitHubConfig githubConfig;
    private final AuditLogDao auditLogDao;
    private final Gson gson;
    
    public PluginController(PluginMetricsService metricsService, GitHubClient githubClient, 
                           GitHubConfig githubConfig, AuditLogDao auditLogDao) {
        this.metricsService = metricsService;
        this.githubClient = githubClient;
        this.githubConfig = githubConfig;
        this.auditLogDao = auditLogDao;
        this.gson = new Gson();
    }
    
    /**
     * GET /api/plugins - Lists all installed plugins
     */
    public void listPlugins(Context ctx) {
        PluginManager pluginManager = Bukkit.getPluginManager();
        List<PluginEntry> entries = new ArrayList<>();
        
        for (Plugin plugin : pluginManager.getPlugins()) {
            PluginEntry entry = new PluginEntry();
            entry.setName(plugin.getName());
            entry.setVersion(plugin.getDescription().getVersion());
            entry.setEnabled(plugin.isEnabled());
            entry.setAuthors(plugin.getDescription().getAuthors());
            entry.setDescription(plugin.getDescription().getDescription());
            entry.setLastSeen(Instant.now());
            
            // Add metrics if available
            Map<String, Object> metrics = metricsService.getMetrics(plugin.getName());
            entry.setMetrics(metrics);
            
            // Check if plugin is tracked on GitHub
            String repo = findGitHubRepo(plugin.getName());
            if (repo != null) {
                entry.setGithubRepo(repo);
                entry.setTags(Arrays.asList("MatisseAD", "GitHub"));
            }
            
            entries.add(entry);
        }
        
        ctx.json(Map.of(
            "plugins", entries,
            "total", entries.size()
        ));
    }
    
    /**
     * GET /api/plugins/:name - Gets detailed information about a specific plugin
     */
    public void getPlugin(Context ctx) {
        String pluginName = ctx.pathParam("name");
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        
        if (plugin == null) {
            ctx.status(404).json(Map.of("error", "Plugin not found"));
            return;
        }
        
        PluginEntry entry = new PluginEntry();
        entry.setName(plugin.getName());
        entry.setVersion(plugin.getDescription().getVersion());
        entry.setEnabled(plugin.isEnabled());
        entry.setAuthors(plugin.getDescription().getAuthors());
        entry.setDescription(plugin.getDescription().getDescription());
        entry.setLastSeen(Instant.now());
        entry.setMetrics(metricsService.getMetrics(plugin.getName()));
        
        String repo = findGitHubRepo(plugin.getName());
        if (repo != null) {
            entry.setGithubRepo(repo);
        }
        
        // Add config file paths
        File pluginFolder = plugin.getDataFolder();
        List<String> configFiles = new ArrayList<>();
        if (pluginFolder.exists()) {
            findConfigFiles(pluginFolder, configFiles, "");
        }
        
        ctx.json(Map.of(
            "plugin", entry,
            "configFiles", configFiles,
            "jarPath", getPluginJarPath(plugin)
        ));
    }
    
    /**
     * POST /api/plugins/:name/action - Performs an action on a plugin (enable/disable/reload)
     */
    public void performAction(Context ctx) {
        String pluginName = ctx.pathParam("name");
        String action = ctx.bodyAsClass(ActionRequest.class).action;
        String user = ctx.attribute("user");
        String ip = ctx.ip();
        
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            ctx.status(404).json(Map.of("error", "Plugin not found"));
            return;
        }
        
        PluginManager pluginManager = Bukkit.getPluginManager();
        boolean success = false;
        String message;
        
        try {
            switch (action.toLowerCase()) {
                case "enable":
                    if (!plugin.isEnabled()) {
                        pluginManager.enablePlugin(plugin);
                        success = true;
                        message = "Plugin enabled successfully";
                    } else {
                        message = "Plugin is already enabled";
                        success = true;
                    }
                    break;
                    
                case "disable":
                    if (plugin.isEnabled()) {
                        pluginManager.disablePlugin(plugin);
                        success = true;
                        message = "Plugin disabled successfully";
                    } else {
                        message = "Plugin is already disabled";
                        success = true;
                    }
                    break;
                    
                case "reload":
                    if (plugin.isEnabled()) {
                        pluginManager.disablePlugin(plugin);
                        pluginManager.enablePlugin(plugin);
                        success = true;
                        message = "Plugin reloaded successfully";
                    } else {
                        message = "Cannot reload a disabled plugin";
                    }
                    break;
                    
                default:
                    message = "Invalid action: " + action;
            }
        } catch (Exception e) {
            logger.error("Failed to perform action {} on plugin {}", action, pluginName, e);
            message = "Error: " + e.getMessage();
        }
        
        // Log the action
        AuditLog log = new AuditLog(user, "PLUGIN_" + action.toUpperCase(), pluginName, ip);
        log.setSuccess(success);
        log.setMetadata(gson.toJson(Map.of("action", action)));
        auditLogDao.save(log);
        
        ctx.json(Map.of(
            "success", success,
            "message", message,
            "plugin", pluginName,
            "action", action
        ));
    }
    
    /**
     * GET /api/plugins/:name/releases - Gets available GitHub releases for a plugin
     */
    public void getReleases(Context ctx) {
        String pluginName = ctx.pathParam("name");
        String repo = findGitHubRepo(pluginName);
        
        if (repo == null) {
            ctx.status(404).json(Map.of("error", "Plugin not tracked on GitHub"));
            return;
        }
        
        List<ReleaseEntry> releases = githubClient.getReleases(repo);
        
        // Get current plugin version for comparison
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        String currentVersion = plugin != null ? plugin.getDescription().getVersion() : null;
        
        ctx.json(Map.of(
            "plugin", pluginName,
            "repo", repo,
            "currentVersion", currentVersion,
            "releases", releases
        ));
    }
    
    private String findGitHubRepo(String pluginName) {
        for (String repo : githubConfig.getRepos()) {
            String repoName = repo.substring(repo.lastIndexOf('/') + 1);
            if (repoName.equalsIgnoreCase(pluginName)) {
                return repo;
            }
        }
        return null;
    }
    
    private void findConfigFiles(File dir, List<String> result, String relativePath) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            String path = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
            
            if (file.isDirectory()) {
                findConfigFiles(file, result, path);
            } else if (file.isFile()) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".yml") || name.endsWith(".yaml") || 
                    name.endsWith(".json") || name.endsWith(".properties") ||
                    name.endsWith(".conf") || name.endsWith(".config") ||
                    name.endsWith(".txt")) {
                    result.add(path);
                }
            }
        }
    }
    
    private String getPluginJarPath(Plugin plugin) {
        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            return jarFile.getAbsolutePath();
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    public static class ActionRequest {
        public String action;
    }
}
