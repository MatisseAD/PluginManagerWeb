package fr.matissead.pluginmanagerweb.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration holder for database settings.
 * Supports SQLite by default with optional MySQL/PostgreSQL.
 */
public class DatabaseConfig {
    private final String type;
    private final String sqlitePath;
    
    public DatabaseConfig(ConfigurationSection config) {
        ConfigurationSection dbSection = config.getConfigurationSection("pluginmanager.database");
        if (dbSection == null) {
            this.type = "sqlite";
            this.sqlitePath = "data/pluginmanager.sqlite";
            return;
        }
        
        this.type = dbSection.getString("type", "sqlite");
        this.sqlitePath = dbSection.getString("sqlite_path", "data/pluginmanager.sqlite");
    }
    
    public String getType() {
        return type;
    }
    
    public String getSqlitePath() {
        return sqlitePath;
    }
    
    public boolean isSQLite() {
        return "sqlite".equalsIgnoreCase(type);
    }
}
