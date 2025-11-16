package fr.matissead.pluginmanagerweb.api;

import java.util.Map;

/**
 * Public API for other plugins to interact with PluginManagerWeb.
 * Other plugins can use this interface to report custom metrics and events.
 * 
 * Usage example:
 * <pre>
 * Plugin pmwPlugin = Bukkit.getPluginManager().getPlugin("PluginManagerWeb");
 * if (pmwPlugin instanceof PluginManagerWeb) {
 *     PluginManagerWebAPI api = ((PluginManagerWeb) pmwPlugin).getAPI();
 *     api.incrementCounter("MyPlugin", "commands_executed", 1);
 * }
 * </pre>
 */
public interface PluginManagerWebAPI {
    
    /**
     * Increments a counter metric for a plugin.
     * 
     * @param pluginName Name of the plugin reporting the metric
     * @param metricKey Key identifying the metric (e.g., "commands_executed")
     * @param delta Amount to increment by (use negative for decrement)
     */
    void incrementCounter(String pluginName, String metricKey, long delta);
    
    /**
     * Records a custom event for a plugin with optional metadata.
     * 
     * @param pluginName Name of the plugin reporting the event
     * @param eventType Type of event (e.g., "player_action", "error")
     * @param payload Additional data about the event
     */
    void recordEvent(String pluginName, String eventType, Map<String, Object> payload);
    
    /**
     * Sets a gauge metric (a value that can go up or down).
     * 
     * @param pluginName Name of the plugin reporting the metric
     * @param metricKey Key identifying the metric
     * @param value Current value of the gauge
     */
    void setGauge(String pluginName, String metricKey, Object value);
    
    /**
     * Retrieves all metrics for a specific plugin.
     * 
     * @param pluginName Name of the plugin
     * @return Map of metric keys to values
     */
    Map<String, Object> getMetrics(String pluginName);
    
    /**
     * Clears all metrics for a specific plugin.
     * Use with caution as this will remove all collected metrics.
     * 
     * @param pluginName Name of the plugin
     */
    void clearMetrics(String pluginName);
}
