package fr.matissead.pluginmanagerweb.model;

import java.time.Instant;

/**
 * Represents an audit log entry for tracking administrative actions.
 * Records who did what, when, and from where for security and compliance.
 */
public class AuditLog {
    private Long id;
    private Instant timestamp;
    private String user;
    private String action;
    private String target;
    private String ipAddress;
    private String metadata; // JSON string for additional context
    private boolean success;
    
    public AuditLog() {
        this.timestamp = Instant.now();
        this.success = true;
    }
    
    public AuditLog(String user, String action, String target, String ipAddress) {
        this();
        this.user = user;
        this.action = action;
        this.target = target;
        this.ipAddress = ipAddress;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
}
