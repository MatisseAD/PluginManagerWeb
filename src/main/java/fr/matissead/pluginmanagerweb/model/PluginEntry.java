package fr.matissead.pluginmanagerweb.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a plugin entry with metadata and runtime information.
 * Contains information about installed plugins, their versions, state, and associated metrics.
 */
public class PluginEntry {
    private String name;
    private String version;
    private boolean enabled;
    private List<String> authors;
    private String description;
    private Instant lastSeen;
    private Map<String, Object> metrics;
    private String githubRepo;
    private List<String> tags;
    
    public PluginEntry() {
    }
    
    public PluginEntry(String name, String version, boolean enabled) {
        this.name = name;
        this.version = version;
        this.enabled = enabled;
        this.lastSeen = Instant.now();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public List<String> getAuthors() {
        return authors;
    }
    
    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Instant getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public Map<String, Object> getMetrics() {
        return metrics;
    }
    
    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }
    
    public String getGithubRepo() {
        return githubRepo;
    }
    
    public void setGithubRepo(String githubRepo) {
        this.githubRepo = githubRepo;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
