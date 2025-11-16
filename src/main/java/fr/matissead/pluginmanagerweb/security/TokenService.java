package fr.matissead.pluginmanagerweb.security;

import fr.matissead.pluginmanagerweb.config.AuthConfig;
import fr.matissead.pluginmanagerweb.model.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing API tokens and user sessions.
 * Handles authentication and session validation.
 */
public class TokenService {
    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    private final AuthConfig authConfig;
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    
    public TokenService(AuthConfig authConfig) {
        this.authConfig = authConfig;
    }
    
    /**
     * Validates a token and returns the associated session if valid.
     */
    public UserSession validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        // Check if it's the admin token
        if (authConfig.getAdminToken().equals(token)) {
            UserSession session = new UserSession();
            session.setId("admin-" + UUID.randomUUID());
            session.setRole("admin");
            session.setUsername("admin");
            session.setToken(token);
            // Admin token never expires
            session.setExpiration(Instant.now().plus(365, ChronoUnit.DAYS));
            return session;
        }
        
        // Check for temporary session tokens
        UserSession session = sessions.get(token);
        if (session != null && !session.isExpired()) {
            return session;
        }
        
        // Remove expired session
        if (session != null && session.isExpired()) {
            sessions.remove(token);
        }
        
        return null;
    }
    
    /**
     * Creates a new temporary session with the given role.
     * Useful for future implementation of login endpoint.
     */
    public UserSession createSession(String username, String role, int durationHours) {
        String token = UUID.randomUUID().toString();
        UserSession session = new UserSession();
        session.setId(UUID.randomUUID().toString());
        session.setUsername(username);
        session.setRole(role);
        session.setToken(token);
        session.setExpiration(Instant.now().plus(durationHours, ChronoUnit.HOURS));
        
        sessions.put(token, session);
        logger.info("Created new session for user: {} with role: {}", username, role);
        
        return session;
    }
    
    /**
     * Invalidates a session token.
     */
    public void invalidateSession(String token) {
        UserSession removed = sessions.remove(token);
        if (removed != null) {
            logger.info("Invalidated session for user: {}", removed.getUsername());
        }
    }
    
    /**
     * Cleans up expired sessions periodically.
     */
    public void cleanupExpiredSessions() {
        int removed = 0;
        for (Map.Entry<String, UserSession> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                sessions.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            logger.info("Cleaned up {} expired sessions", removed);
        }
    }
    
    /**
     * Returns the number of active sessions.
     */
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
                .filter(s -> !s.isExpired())
                .count();
    }
}
