package fr.matissead.pluginmanagerweb.metrics;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for collecting and managing plugin metrics.
 * Provides API for plugins to report custom metrics and aggregates them.
 * Future extension point for Prometheus metrics export.
 */
public class PluginMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(PluginMetricsService.class);
    private final DataSource dataSource;
    private final Gson gson;
    
    // In-memory cache for quick access
    private final Map<String, Map<String, Object>> metricsCache = new ConcurrentHashMap<>();
    
    public PluginMetricsService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.gson = new Gson();
        loadMetricsFromDatabase();
    }
    
    /**
     * Increments a counter metric for a plugin.
     */
    public void incrementCounter(String pluginName, String metricKey, long delta) {
        Map<String, Object> pluginMetrics = metricsCache.computeIfAbsent(pluginName, k -> new ConcurrentHashMap<>());
        
        Object current = pluginMetrics.get(metricKey);
        long newValue;
        
        if (current instanceof Number) {
            newValue = ((Number) current).longValue() + delta;
        } else {
            newValue = delta;
        }
        
        pluginMetrics.put(metricKey, newValue);
        persistMetric(pluginName, metricKey, newValue);
    }
    
    /**
     * Records a custom event for a plugin with metadata.
     */
    public void recordEvent(String pluginName, String eventType, Map<String, Object> payload) {
        String eventKey = "event_" + eventType;
        Map<String, Object> pluginMetrics = metricsCache.computeIfAbsent(pluginName, k -> new ConcurrentHashMap<>());
        
        // Store event count
        incrementCounter(pluginName, eventKey + "_count", 1);
        
        // Store last event data
        pluginMetrics.put(eventKey + "_last", payload);
        pluginMetrics.put(eventKey + "_last_time", Instant.now().toString());
        
        logger.debug("Recorded event {} for plugin {}", eventType, pluginName);
    }
    
    /**
     * Sets a gauge metric (a value that can go up or down).
     */
    public void setGauge(String pluginName, String metricKey, Object value) {
        Map<String, Object> pluginMetrics = metricsCache.computeIfAbsent(pluginName, k -> new ConcurrentHashMap<>());
        pluginMetrics.put(metricKey, value);
        persistMetric(pluginName, metricKey, value);
    }
    
    /**
     * Retrieves all metrics for a specific plugin.
     */
    public Map<String, Object> getMetrics(String pluginName) {
        Map<String, Object> metrics = metricsCache.get(pluginName);
        return metrics != null ? new HashMap<>(metrics) : new HashMap<>();
    }
    
    /**
     * Retrieves metrics for all plugins.
     */
    public Map<String, Map<String, Object>> getAllMetrics() {
        Map<String, Map<String, Object>> allMetrics = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : metricsCache.entrySet()) {
            allMetrics.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return allMetrics;
    }
    
    /**
     * Clears metrics for a specific plugin.
     */
    public void clearMetrics(String pluginName) {
        metricsCache.remove(pluginName);
        deleteMetricsFromDatabase(pluginName);
        logger.info("Cleared metrics for plugin: {}", pluginName);
    }
    
    private void persistMetric(String pluginName, String metricKey, Object value) {
        String sql = "INSERT OR REPLACE INTO plugin_metrics (plugin_name, metric_key, metric_value, timestamp) " +
                     "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, pluginName);
            stmt.setString(2, metricKey);
            stmt.setString(3, gson.toJson(value));
            stmt.setString(4, Instant.now().toString());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Failed to persist metric for plugin: " + pluginName, e);
        }
    }
    
    private void loadMetricsFromDatabase() {
        String sql = "SELECT plugin_name, metric_key, metric_value FROM plugin_metrics";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String pluginName = rs.getString("plugin_name");
                String metricKey = rs.getString("metric_key");
                String metricValueJson = rs.getString("metric_value");
                
                Map<String, Object> pluginMetrics = metricsCache.computeIfAbsent(pluginName, k -> new ConcurrentHashMap<>());
                
                // Try to parse as different types
                try {
                    Object value = gson.fromJson(metricValueJson, Object.class);
                    pluginMetrics.put(metricKey, value);
                } catch (Exception e) {
                    pluginMetrics.put(metricKey, metricValueJson);
                }
            }
            
            logger.info("Loaded metrics for {} plugins from database", metricsCache.size());
            
        } catch (SQLException e) {
            logger.error("Failed to load metrics from database", e);
        }
    }
    
    private void deleteMetricsFromDatabase(String pluginName) {
        String sql = "DELETE FROM plugin_metrics WHERE plugin_name = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, pluginName);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Failed to delete metrics for plugin: " + pluginName, e);
        }
    }
}
