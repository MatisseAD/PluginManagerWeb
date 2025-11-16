package fr.matissead.pluginmanagerweb.security;

import fr.matissead.pluginmanagerweb.config.WebConfig;
import fr.matissead.pluginmanagerweb.model.AuditLog;
import fr.matissead.pluginmanagerweb.model.UserSession;
import fr.matissead.pluginmanagerweb.persistence.AuditLogDao;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication middleware for Javalin routes.
 * Validates tokens and enforces IP restrictions.
 */
public class AuthMiddleware implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(AuthMiddleware.class);
    private final TokenService tokenService;
    private final WebConfig webConfig;
    private final AuditLogDao auditLogDao;
    
    public AuthMiddleware(TokenService tokenService, WebConfig webConfig, AuditLogDao auditLogDao) {
        this.tokenService = tokenService;
        this.webConfig = webConfig;
        this.auditLogDao = auditLogDao;
    }
    
    @Override
    public void handle(Context ctx) throws Exception {
        String clientIp = ctx.ip();
        
        // Check IP whitelist
        if (!webConfig.isIpAllowed(clientIp)) {
            logger.warn("Blocked access from unauthorized IP: {}", clientIp);
            logFailedAccess(clientIp, "IP_NOT_ALLOWED", ctx.path());
            ctx.status(403).json(new ErrorResponse("Access denied from this IP address"));
            return;
        }
        
        // Extract token from Authorization header
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header from IP: {}", clientIp);
            logFailedAccess(clientIp, "MISSING_TOKEN", ctx.path());
            ctx.status(401).json(new ErrorResponse("Missing or invalid Authorization header"));
            return;
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        UserSession session = tokenService.validateToken(token);
        
        if (session == null) {
            logger.warn("Invalid token from IP: {}", clientIp);
            logFailedAccess(clientIp, "INVALID_TOKEN", ctx.path());
            ctx.status(401).json(new ErrorResponse("Invalid or expired token"));
            return;
        }
        
        // Store session in context for route handlers
        ctx.attribute("session", session);
        ctx.attribute("user", session.getUsername());
        ctx.attribute("role", session.getRole());
    }
    
    private void logFailedAccess(String ip, String reason, String path) {
        AuditLog log = new AuditLog("anonymous", "ACCESS_DENIED", path, ip);
        log.setSuccess(false);
        log.setMetadata("{\"reason\":\"" + reason + "\"}");
        auditLogDao.save(log);
    }

    /**
         * Simple error response class for JSON serialization.
         */
        public record ErrorResponse(String error, String message) {
            public ErrorResponse(String message) {
                this("UNAUTHORIZED", message);
            }

    }
}
