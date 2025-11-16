package fr.matissead.pluginmanagerweb.model;

import java.time.Instant;

/**
 * Represents a backup of a plugin configuration file.
 * Used for rollback functionality and audit trail of configuration changes.
 */
public class ConfigBackup {
    private Long id;
    private String pluginName;
    private Instant timestamp;
    private String content;
    private String path;
    private String createdBy;
    
    public ConfigBackup() {
        this.timestamp = Instant.now();
    }
    
    public ConfigBackup(String pluginName, String path, String content) {
        this();
        this.pluginName = pluginName;
        this.path = path;
        this.content = content;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPluginName() {
        return pluginName;
    }
    
    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
