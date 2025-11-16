package fr.matissead.pluginmanagerweb.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.matissead.pluginmanagerweb.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Factory for creating and managing database connections.
 * Supports SQLite by default with HikariCP connection pooling.
 * Can be extended to support MySQL/PostgreSQL in the future.
 */
public class DataSourceFactory {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceFactory.class);
    private final HikariDataSource dataSource;
    
    public DataSourceFactory(DatabaseConfig config, File dataFolder) {
        if (config.isSQLite()) {
            this.dataSource = createSQLiteDataSource(config, dataFolder);
        } else {
            throw new UnsupportedOperationException("Only SQLite is currently supported. Type: " + config.getType());
        }
        
        initializeTables();
    }
    
    private HikariDataSource createSQLiteDataSource(DatabaseConfig config, File dataFolder) {
        File dbFile = new File(dataFolder, config.getSqlitePath());
        dbFile.getParentFile().mkdirs();
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setPoolName("PluginManagerWeb-SQLite-Pool");
        
        logger.info("Initializing SQLite database at: {}", dbFile.getAbsolutePath());
        return new HikariDataSource(hikariConfig);
    }
    
    /**
     * Creates database tables if they don't exist.
     * Simple migration system - in production, consider using Flyway or Liquibase.
     */
    private void initializeTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // AuditLog table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    user TEXT,
                    action TEXT NOT NULL,
                    target TEXT,
                    ip_address TEXT,
                    metadata TEXT,
                    success INTEGER NOT NULL DEFAULT 1
                )
                """);
            
            // ConfigBackup table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS config_backups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plugin_name TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    content TEXT NOT NULL,
                    path TEXT NOT NULL,
                    created_by TEXT
                )
                """);
            
            // PluginMetrics table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS plugin_metrics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plugin_name TEXT NOT NULL,
                    metric_key TEXT NOT NULL,
                    metric_value TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    UNIQUE(plugin_name, metric_key)
                )
                """);
            
            // Create indexes for better query performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_config_backups_plugin ON config_backups(plugin_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_plugin_metrics_plugin ON plugin_metrics(plugin_name)");
            
            logger.info("Database tables initialized successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize database tables", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}
