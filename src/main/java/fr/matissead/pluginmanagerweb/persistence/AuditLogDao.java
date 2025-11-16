package fr.matissead.pluginmanagerweb.persistence;

import fr.matissead.pluginmanagerweb.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for AuditLog entries.
 * Handles persistence of audit trail for administrative actions.
 */
public class AuditLogDao {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogDao.class);
    private final DataSource dataSource;
    
    public AuditLogDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Saves an audit log entry to the database.
     */
    public void save(AuditLog auditLog) {
        String sql = "INSERT INTO audit_logs (timestamp, user, action, target, ip_address, metadata, success) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, auditLog.getTimestamp().toString());
            stmt.setString(2, auditLog.getUser());
            stmt.setString(3, auditLog.getAction());
            stmt.setString(4, auditLog.getTarget());
            stmt.setString(5, auditLog.getIpAddress());
            stmt.setString(6, auditLog.getMetadata());
            stmt.setInt(7, auditLog.isSuccess() ? 1 : 0);
            
            stmt.executeUpdate();
            
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    auditLog.setId(keys.getLong(1));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to save audit log", e);
        }
    }
    
    /**
     * Retrieves recent audit logs with optional filtering.
     */
    public List<AuditLog> getRecent(int limit) {
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
        List<AuditLog> logs = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to retrieve audit logs", e);
        }
        
        return logs;
    }
    
    /**
     * Retrieves audit logs for a specific user.
     */
    public List<AuditLog> getByUser(String user, int limit) {
        String sql = "SELECT * FROM audit_logs WHERE user = ? ORDER BY timestamp DESC LIMIT ?";
        List<AuditLog> logs = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user);
            stmt.setInt(2, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to retrieve audit logs for user: " + user, e);
        }
        
        return logs;
    }
    
    private AuditLog mapResultSet(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getLong("id"));
        log.setTimestamp(Instant.parse(rs.getString("timestamp")));
        log.setUser(rs.getString("user"));
        log.setAction(rs.getString("action"));
        log.setTarget(rs.getString("target"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setMetadata(rs.getString("metadata"));
        log.setSuccess(rs.getInt("success") == 1);
        return log;
    }
}
