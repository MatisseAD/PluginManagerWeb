package fr.matissead.pluginmanagerweb.model;

import java.time.Instant;

/**
 * Represents a user session with authentication information.
 * Used for tracking authenticated API access and managing session expiration.
 */
public class UserSession {
    private String id;
    private String role;
    private Instant expiration;
    private String ipAddress;
    private String token;
    private String username;
    
    public UserSession() {
    }
    
    public UserSession(String id, String role, Instant expiration) {
        this.id = id;
        this.role = role;
        this.expiration = expiration;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public Instant getExpiration() {
        return expiration;
    }
    
    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public boolean isExpired() {
        return expiration != null && Instant.now().isAfter(expiration);
    }
}
