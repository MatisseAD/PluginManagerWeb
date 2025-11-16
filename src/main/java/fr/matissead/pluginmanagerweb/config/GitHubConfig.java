package fr.matissead.pluginmanagerweb.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

/**
 * Configuration holder for GitHub integration settings.
 * Manages GitHub API token and repository tracking.
 */
public class GitHubConfig {
    private final String token;
    private final List<String> repos;
    private final boolean autoUpdate;
    
    public GitHubConfig(ConfigurationSection config) {
        ConfigurationSection githubSection = config.getConfigurationSection("pluginmanager.github");
        if (githubSection == null) {
            this.token = "";
            this.repos = Collections.emptyList();
            this.autoUpdate = false;
            return;
        }
        
        this.token = githubSection.getString("token", "");
        this.repos = githubSection.getStringList("repos");
        this.autoUpdate = githubSection.getBoolean("auto_update", false);
    }
    
    public String getToken() {
        return token;
    }
    
    public boolean hasToken() {
        return token != null && !token.isEmpty();
    }
    
    public List<String> getRepos() {
        return repos != null ? repos : Collections.emptyList();
    }
    
    public boolean isAutoUpdate() {
        return autoUpdate;
    }
}
