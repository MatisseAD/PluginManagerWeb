package fr.matissead.pluginmanagerweb.persistence;

import fr.matissead.pluginmanagerweb.model.ConfigBackup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for ConfigBackup entries.
 * Manages configuration file backups for rollback functionality.
 */
public class ConfigBackupDao {
    private static final Logger logger = LoggerFactory.getLogger(ConfigBackupDao.class);
    private final DataSource dataSource;
    
    public ConfigBackupDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Saves a configuration backup to the database.
     */
    public void save(ConfigBackup backup) {
        String sql = "INSERT INTO config_backups (plugin_name, timestamp, content, path, created_by) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, backup.getPluginName());
            stmt.setString(2, backup.getTimestamp().toString());
            stmt.setString(3, backup.getContent());
            stmt.setString(4, backup.getPath());
            stmt.setString(5, backup.getCreatedBy());
            
            stmt.executeUpdate();
            
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    backup.setId(keys.getLong(1));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to save config backup", e);
        }
    }
    
    /**
     * Retrieves all backups for a specific plugin.
     */
    public List<ConfigBackup> getByPlugin(String pluginName) {
        String sql = "SELECT * FROM config_backups WHERE plugin_name = ? ORDER BY timestamp DESC";
        List<ConfigBackup> backups = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, pluginName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    backups.add(mapResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to retrieve config backups for plugin: " + pluginName, e);
        }
        
        return backups;
    }
    
    /**
     * Retrieves a specific backup by ID.
     */
    public ConfigBackup getById(long id) {
        String sql = "SELECT * FROM config_backups WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to retrieve config backup by ID: " + id, e);
        }
        
        return null;
    }
    
    private ConfigBackup mapResultSet(ResultSet rs) throws SQLException {
        ConfigBackup backup = new ConfigBackup();
        backup.setId(rs.getLong("id"));
        backup.setPluginName(rs.getString("plugin_name"));
        backup.setTimestamp(Instant.parse(rs.getString("timestamp")));
        backup.setContent(rs.getString("content"));
        backup.setPath(rs.getString("path"));
        backup.setCreatedBy(rs.getString("created_by"));
        return backup;
    }
}
