package fr.matissead.pluginmanagerweb.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

/**
 * Configuration holder for web server settings.
 * Validates and exposes web-related configuration options.
 */
public class WebConfig {
    private final boolean enabled;
    private final int port;
    private final String bindAddress;
    private final boolean tlsEnabled;
    private final String certPath;
    private final String keyPath;
    private final List<String> allowedIps;
    
    public WebConfig(ConfigurationSection config) {
        ConfigurationSection webSection = config.getConfigurationSection("pluginmanager.web");
        if (webSection == null) {
            throw new IllegalArgumentException("Missing 'pluginmanager.web' configuration section");
        }
        
        this.enabled = webSection.getBoolean("enabled", true);
        this.port = webSection.getInt("port", 8080);
        this.bindAddress = webSection.getString("bind_address", "0.0.0.0");
        
        ConfigurationSection tlsSection = webSection.getConfigurationSection("tls");
        if (tlsSection != null) {
            this.tlsEnabled = tlsSection.getBoolean("enabled", false);
            this.certPath = tlsSection.getString("cert_path", "cert.pem");
            this.keyPath = tlsSection.getString("key_path", "key.pem");
        } else {
            this.tlsEnabled = false;
            this.certPath = null;
            this.keyPath = null;
        }
        
        this.allowedIps = webSection.getStringList("allowed_ips");
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getBindAddress() {
        return bindAddress;
    }
    
    public boolean isTlsEnabled() {
        return tlsEnabled;
    }
    
    public String getCertPath() {
        return certPath;
    }
    
    public String getKeyPath() {
        return keyPath;
    }
    
    public List<String> getAllowedIps() {
        return allowedIps != null ? allowedIps : Collections.emptyList();
    }
    
    public boolean isIpAllowed(String ip) {
        if (allowedIps == null || allowedIps.isEmpty()) {
            return true; // Empty list means all IPs allowed
        }
        return allowedIps.contains(ip);
    }
}
