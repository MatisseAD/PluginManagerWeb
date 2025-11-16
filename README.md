# 🔌 PluginManagerWeb

**PluginManagerWeb** is a powerful web-based management panel for Minecraft (Paper 1.20+) servers. Inspired by LuckPerms' web editor, it provides a modern, feature-rich dashboard for managing plugins, viewing metrics, editing configurations, and monitoring your server.

## ✨ Features

### 🎮 Plugin Management
- **Real-time plugin overview** - View all installed plugins with their status, version, and authors
- **Enable/Disable/Reload** - Control plugins directly from the web interface
- **Plugin details** - Access comprehensive information about each plugin
- **Action audit logging** - Track all administrative actions for security and compliance

### 📦 GitHub Integration
- **Release tracking** - Automatically fetch releases from configured GitHub repositories
- **Version comparison** - See available updates for your plugins
- **One-click updates** - Download and install plugin updates directly from GitHub
- **Release notes** - View changelogs and release information

### ⚙️ Configuration Management
- **File browser** - Navigate and view plugin configuration files
- **Online editor** - Edit config files directly in the browser
- **Automatic backups** - Every config change is backed up automatically
- **Rollback support** - Restore previous configurations with one click
- **Safe editing** - Path validation prevents access outside plugin directories

### 📊 Metrics & Monitoring
- **Server statistics** - CPU, memory, TPS, and player count
- **Plugin metrics** - Custom metrics reported by plugins
- **Aggregated views** - Overview of top plugins by usage
- **Public API** - Other plugins can report custom metrics

### 🔒 Security
- **Token-based authentication** - Secure API access with admin tokens
- **IP whitelisting** - Restrict access to specific IP addresses
- **Audit logging** - All sensitive actions are logged with user, IP, and timestamp
- **Role-based access** - Future support for LuckPerms group integration

### 🌐 Modern Web Interface
- **Dark theme** - Beautiful, modern design optimized for readability
- **Responsive layout** - Works on desktop, tablet, and mobile devices
- **Real-time updates** - WebSocket connection for live server events
- **Intuitive navigation** - Easy-to-use sidebar and tab-based interface

## 📋 Requirements

- **Minecraft Server**: Paper 1.20.4 or higher (Spigot may work but not officially supported)
- **Java**: Java 17 or higher
- **Memory**: Minimal overhead (~20MB)

## 🚀 Installation

1. **Download** the latest release from the [Releases page](https://github.com/MatisseAD/PluginManagerWeb/releases)

2. **Place** the JAR file in your server's `plugins/` directory

3. **Start** your Minecraft server

4. **Configure** the plugin by editing `plugins/PluginManagerWeb/config.yml`

5. **Restart** the server to apply configuration changes

## ⚙️ Configuration

Edit `plugins/PluginManagerWeb/config.yml`:

```yaml
pluginmanager:
  enabled: true
  web:
    enabled: true
    port: 8080
    bind_address: 0.0.0.0
    tls:
      enabled: false
      cert_path: cert.pem
      key_path: key.pem
    allowed_ips: [] # Empty = allow all
  auth:
    # IMPORTANT: Change this to a secure random token!
    admin_token: "CHANGE_ME"
    use_luckperms_groups: false
  github:
    token: "" # Optional GitHub personal access token
    repos:
      - "MatisseAD/ReanimateMC"
      - "MatisseAD/EvenMoreItems"
      - "MatisseAD/CryptocurrencyMC"
      - "MatisseAD/HammerMC"
    auto_update: false
  database:
    type: sqlite
    sqlite_path: data/pluginmanager.sqlite
```

### Configuration Options

#### Web Server
- `port` - HTTP port (default: 8080)
- `bind_address` - Interface to bind to (0.0.0.0 = all interfaces)
- `allowed_ips` - IP whitelist (empty array = allow all)
- `tls` - HTTPS configuration (requires valid certificates)

#### Authentication
- `admin_token` - API access token (**MUST BE CHANGED**)
- `use_luckperms_groups` - Enable LuckPerms integration (coming soon)

#### GitHub Integration
- `token` - GitHub personal access token (optional, for private repos or higher rate limits)
- `repos` - List of repositories to track for updates
- `auto_update` - Automatically download updates (not recommended for production)

#### Database
- `type` - Database type (currently only `sqlite` is supported)
- `sqlite_path` - Path to SQLite database file

## 🌐 Accessing the Dashboard

1. Open your browser and navigate to `http://YOUR_SERVER_IP:8080/`

2. Enter your **admin_token** from the configuration

3. Click **Login**

You're now ready to manage your plugins!

## 🔑 API Usage

### Authentication

All API requests (except `/api/health`) require an `Authorization` header:

```bash
Authorization: Bearer YOUR_ADMIN_TOKEN
```

### Available Endpoints

#### Server Information
```http
GET /api/health              # Health check (no auth required)
GET /api/server              # Detailed server information
```

#### Plugin Management
```http
GET /api/plugins                      # List all plugins
GET /api/plugins/{name}               # Get plugin details
POST /api/plugins/{name}/action       # Enable/disable/reload plugin
GET /api/plugins/{name}/releases      # Get GitHub releases
```

#### Configuration
```http
GET /api/plugins/{name}/config                 # List config files
GET /api/plugins/{name}/config/file?path=...   # Get file content
POST /api/plugins/{name}/config/file           # Save config file
GET /api/plugins/{name}/config/backups         # List backups
POST /api/plugins/{name}/config/rollback       # Restore backup
```

#### Metrics
```http
GET /api/plugins/{name}/metrics    # Get plugin metrics
GET /api/metrics/overview          # Get all metrics overview
```

### Example: Enable a Plugin

```bash
curl -X POST http://localhost:8080/api/plugins/MyPlugin/action \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action": "enable"}'
```

## 🔌 Plugin Integration

Other plugins can report custom metrics to PluginManagerWeb:

```java
import fr.matissead.pluginmanagerweb.PluginManagerWeb;
import fr.matissead.pluginmanagerweb.api.PluginManagerWebAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class MyPlugin extends JavaPlugin {
    
    private PluginManagerWebAPI pmwAPI;
    
    @Override
    public void onEnable() {
        // Get PluginManagerWeb API
        Plugin pmw = Bukkit.getPluginManager().getPlugin("PluginManagerWeb");
        if (pmw instanceof PluginManagerWeb) {
            pmwAPI = ((PluginManagerWeb) pmw).getAPI();
        }
    }
    
    public void reportMetric() {
        if (pmwAPI != null) {
            // Increment a counter
            pmwAPI.incrementCounter("MyPlugin", "commands_executed", 1);
            
            // Record an event
            Map<String, Object> payload = Map.of(
                "player", "Steve",
                "command", "/mycommand"
            );
            pmwAPI.recordEvent("MyPlugin", "command_executed", payload);
            
            // Set a gauge value
            pmwAPI.setGauge("MyPlugin", "active_users", 42);
        }
    }
}
```

## 🏗️ Building from Source

```bash
# Clone the repository
git clone https://github.com/MatisseAD/PluginManagerWeb.git
cd PluginManagerWeb

# Build with Gradle
./gradlew shadowJar

# The JAR will be in build/libs/PluginManagerWeb.jar
```

## 📁 Project Structure

```
src/main/java/fr/matissead/pluginmanagerweb/
├── api/
│   ├── controllers/         # REST API controllers
│   │   ├── ServerController.java
│   │   ├── PluginController.java
│   │   ├── ConfigController.java
│   │   └── MetricsController.java
│   ├── websocket/           # WebSocket handlers
│   │   └── EventsWebSocketHandler.java
│   └── PluginManagerWebAPI.java  # Public API interface
├── config/                  # Configuration management
├── github/                  # GitHub API client
├── metrics/                 # Metrics collection service
├── model/                   # Data models
├── persistence/             # Database layer (DAOs)
├── security/                # Authentication & authorization
├── PluginManagerWeb.java    # Main plugin class
└── WebServer.java          # Javalin web server

src/main/resources/
├── config.yml              # Default configuration
├── plugin.yml              # Bukkit plugin definition
└── web/                    # Frontend assets
    ├── index.html
    ├── css/style.css
    └── js/
        ├── api.js
        └── app.js
```

## 🛠️ Technology Stack

- **Backend Framework**: [Javalin](https://javalin.io/) 5.6
- **Database**: SQLite with [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pooling
- **HTTP Client**: [OkHttp](https://square.github.io/okhttp/)
- **JSON**: [Gson](https://github.com/google/gson)
- **Logging**: SLF4J
- **Frontend**: Vanilla JavaScript (no frameworks)

## 🔐 Security Considerations

1. **Change the default token** - The `admin_token` must be changed immediately
2. **Use HTTPS in production** - Configure TLS for encrypted communication
3. **Restrict IPs** - Use the `allowed_ips` whitelist in production environments
4. **Secure the port** - Consider using a reverse proxy (nginx, Caddy) with rate limiting
5. **Regular updates** - Keep the plugin updated for security patches

## 🐛 Troubleshooting

### Web server doesn't start
- Check if the port is already in use: `netstat -tulpn | grep 8080`
- Check server logs for error messages
- Ensure Java 17+ is installed

### Can't access the dashboard
- Verify the bind_address is correct (use 0.0.0.0 for all interfaces)
- Check firewall rules allow the port
- Confirm your IP is in the allowed_ips list (if configured)

### Authentication fails
- Verify the token matches the one in config.yml
- Check for extra spaces or quotes in the token
- Try clearing browser cache/cookies

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Inspired by [LuckPerms](https://luckperms.net/) web editor
- Built with [Javalin](https://javalin.io/)
- Icons from [Emoji](https://emojipedia.org/)

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/MatisseAD/PluginManagerWeb/issues)
- **Discussions**: [GitHub Discussions](https://github.com/MatisseAD/PluginManagerWeb/discussions)
- **Website**: [MatisseAD on GitHub](https://github.com/MatisseAD)

---

**Made with ❤️ by MatisseAD & Copilot**
