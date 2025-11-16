package fr.matissead.pluginmanagerweb.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration holder for authentication settings.
 * Manages admin token and LuckPerms integration options.
 */
public class AuthConfig {
    private final String adminToken;
    private final boolean useLuckPermsGroups;
    
    public AuthConfig(ConfigurationSection config) {
        ConfigurationSection authSection = config.getConfigurationSection("pluginmanager.auth");
        if (authSection == null) {
            throw new IllegalArgumentException("Missing 'pluginmanager.auth' configuration section");
        }
        
        this.adminToken = authSection.getString("admin_token", "CHANGE_ME");
        this.useLuckPermsGroups = authSection.getBoolean("use_luckperms_groups", false);
        
        if ("CHANGE_ME".equals(adminToken)) {
            throw new IllegalStateException("Admin token must be changed from default value! Please set a secure token in config.yml");
        }
    }
    
    public String getAdminToken() {
        return adminToken;
    }
    
    public boolean isUseLuckPermsGroups() {
        return useLuckPermsGroups;
    }
}
