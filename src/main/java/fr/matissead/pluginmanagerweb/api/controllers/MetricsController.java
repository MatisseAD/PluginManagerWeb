package fr.matissead.pluginmanagerweb.api.controllers;

import fr.matissead.pluginmanagerweb.metrics.PluginMetricsService;
import io.javalin.http.Context;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for metrics and statistics.
 * Provides access to plugin metrics and aggregated statistics.
 */
public class MetricsController {
    private final PluginMetricsService metricsService;
    
    public MetricsController(PluginMetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    /**
     * GET /api/plugins/:name/metrics - Gets metrics for a specific plugin
     */
    public void getPluginMetrics(Context ctx) {
        String pluginName = ctx.pathParam("name");
        Map<String, Object> metrics = metricsService.getMetrics(pluginName);
        
        ctx.json(Map.of(
            "plugin", pluginName,
            "metrics", metrics
        ));
    }
    
    /**
     * GET /api/metrics/overview - Gets aggregated metrics across all plugins
     */
    public void getMetricsOverview(Context ctx) {
        Map<String, Map<String, Object>> allMetrics = metricsService.getAllMetrics();
        
        // Calculate some aggregate statistics
        int totalPluginsWithMetrics = allMetrics.size();
        
        // Find top plugins by various metrics
        List<Map<String, Object>> topByCommandsExecuted = getTopPluginsByMetric(allMetrics, "commands_executed", 5);
        List<Map<String, Object>> topByEventsProcessed = getTopPluginsByMetric(allMetrics, "events_processed", 5);
        
        ctx.json(Map.of(
            "summary", Map.of(
                "totalPluginsWithMetrics", totalPluginsWithMetrics,
                "timestamp", System.currentTimeMillis()
            ),
            "topPlugins", Map.of(
                "byCommandsExecuted", topByCommandsExecuted,
                "byEventsProcessed", topByEventsProcessed
            ),
            "allMetrics", allMetrics
        ));
    }
    
    private List<Map<String, Object>> getTopPluginsByMetric(Map<String, Map<String, Object>> allMetrics,
                                                            String metricKey, int limit) {
        return allMetrics.entrySet().stream()
                .filter(e -> e.getValue().containsKey(metricKey))
                .map(e -> {
                    Object value = e.getValue().get(metricKey);
                    long numValue = (value instanceof Number) ? ((Number) value).longValue() : 0L;
                    Map<String, Object> m = new HashMap<>();
                    m.put("plugin", e.getKey());
                    m.put("value", numValue);
                    return m;
                })
                .sorted((m1, m2) -> Long.compare((Long) m2.get("value"), (Long) m1.get("value")))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
