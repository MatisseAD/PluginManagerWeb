# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Security Features

### Authentication
- Token-based authentication for all API endpoints
- Configurable admin token in config.yml
- Session management with expiration
- Future support for LuckPerms group integration

### Authorization
- IP whitelisting support
- Per-request authorization checks
- Audit logging of all administrative actions

### Data Protection
- Path traversal protection for config file access
- Input validation on all endpoints
- SQL injection prevention through prepared statements
- Automatic config backups before modifications

### Communication Security
- Optional TLS/HTTPS support
- CORS configuration
- WebSocket secure connections support

## Reporting a Vulnerability

If you discover a security vulnerability, please follow these steps:

1. **Do NOT** open a public issue
2. Email the maintainers at: [security@matissead.fr] (if available) or open a private security advisory
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

We will respond within 48 hours and work on a fix promptly.

## Security Best Practices

### For Server Administrators

1. **Change Default Token**
   ```yaml
   auth:
     admin_token: "USE_A_STRONG_RANDOM_TOKEN_HERE"
   ```
   Generate a secure token: `openssl rand -base64 32`

2. **Enable IP Whitelisting**
   ```yaml
   web:
     allowed_ips:
       - "192.168.1.100"
       - "10.0.0.5"
   ```

3. **Use HTTPS in Production**
   ```yaml
   web:
     tls:
       enabled: true
       cert_path: /path/to/cert.pem
       key_path: /path/to/key.pem
   ```

4. **Use a Reverse Proxy**
   - nginx or Caddy for rate limiting
   - Automatic HTTPS with Let's Encrypt
   - Additional security headers

5. **Regular Updates**
   - Keep PluginManagerWeb updated
   - Monitor security advisories
   - Update dependencies

6. **Restrict Port Access**
   - Use firewall rules
   - Bind to localhost if using reverse proxy
   - Consider VPN for remote access

### For Plugin Developers

When integrating with PluginManagerWeb API:

1. **Validate Input** - Always validate data before reporting metrics
2. **Rate Limiting** - Don't spam the metrics API
3. **Error Handling** - Handle API failures gracefully
4. **Sensitive Data** - Never report sensitive information in metrics

## Known Security Considerations

### Current Implementation

1. **Token Storage** - Admin token is stored in plaintext in config.yml
   - **Mitigation**: Secure file permissions (600 or 640)
   - **Future**: Support for hashed tokens

2. **SQLite Database** - Local file-based database
   - **Mitigation**: File permissions and regular backups
   - **Future**: MySQL/PostgreSQL support for multi-server setups

3. **WebSocket Authentication** - Current WebSocket doesn't require auth
   - **Status**: Under review for next version
   - **Mitigation**: Use IP whitelisting

4. **Session Management** - In-memory session storage
   - **Impact**: Sessions lost on restart
   - **Future**: Persistent session storage option

### Not Implemented Yet

- Two-factor authentication (2FA)
- OAuth2/OpenID Connect integration
- Rate limiting (recommended to use reverse proxy)
- Automated security scanning in CI/CD

## Audit Logging

All sensitive operations are logged to the database:

```sql
SELECT * FROM audit_logs 
WHERE action IN ('PLUGIN_ENABLE', 'PLUGIN_DISABLE', 'CONFIG_UPDATE', 'CONFIG_ROLLBACK')
ORDER BY timestamp DESC;
```

Audit logs include:
- Timestamp
- User/token identifier
- Action performed
- Target (plugin/file)
- IP address
- Success/failure status
- Additional metadata

## Dependency Security

We use the following vetted dependencies:

- Javalin 5.6.2
- HikariCP 5.0.1
- SQLite JDBC 3.43.0.0
- Gson 2.10.1
- OkHttp 4.11.0

All dependencies are regularly updated to address security vulnerabilities.

## Security Checklist

Before deploying to production:

- [ ] Changed default admin token
- [ ] Configured IP whitelist or firewall rules
- [ ] Enabled HTTPS/TLS
- [ ] Set appropriate file permissions (config: 600, database: 640)
- [ ] Configured reverse proxy with rate limiting
- [ ] Reviewed audit log configuration
- [ ] Tested backup and rollback functionality
- [ ] Documented access procedures for team
- [ ] Set up monitoring and alerting

## Contact

For security concerns:
- GitHub Security Advisories: [Create Advisory](https://github.com/MatisseAD/PluginManagerWeb/security/advisories)
- Issues: [GitHub Issues](https://github.com/MatisseAD/PluginManagerWeb/issues) (for non-sensitive bugs)

---

Last Updated: 2024-11-16
