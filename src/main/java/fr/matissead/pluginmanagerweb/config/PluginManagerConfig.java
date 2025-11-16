package fr.matissead.pluginmanagerweb.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Main configuration holder for PluginManagerWeb.
 * Aggregates all configuration sections for easy access.
 */
public class PluginManagerConfig {
    private final boolean enabled;
    private final WebConfig webConfig;
    private final AuthConfig authConfig;
    private final GitHubConfig githubConfig;
    private final DatabaseConfig databaseConfig;
    
    public PluginManagerConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("pluginmanager.enabled", true);
        
        try {
            this.webConfig = new WebConfig(config);
            this.authConfig = new AuthConfig(config);
            this.githubConfig = new GitHubConfig(config);
            this.databaseConfig = new DatabaseConfig(config);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load PluginManagerWeb configuration: " + e.getMessage(), e);
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public WebConfig getWebConfig() {
        return webConfig;
    }
    
    public AuthConfig getAuthConfig() {
        return authConfig;
    }
    
    public GitHubConfig getGithubConfig() {
        return githubConfig;
    }
    
    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }
}
