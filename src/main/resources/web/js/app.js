/**
 * Main application logic for PluginManagerWeb dashboard
 */

// Application state
const app = {
    plugins: [],
    serverInfo: null,
    currentPlugin: null,
    
    init() {
        this.setupLogin();
        this.checkAuth();
    },
    
    setupLogin() {
        const loginBtn = document.getElementById('login-btn');
        const tokenInput = document.getElementById('token-input');
        const loginError = document.getElementById('login-error');
        
        loginBtn.addEventListener('click', async () => {
            const token = tokenInput.value.trim();
            if (!token) {
                this.showError(loginError, 'Please enter a token');
                return;
            }
            
            window.api.setToken(token);
            
            try {
                await window.api.getHealth();
                this.showDashboard();
            } catch (error) {
                this.showError(loginError, error.message);
                window.api.clearToken();
            }
        });
        
        tokenInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') loginBtn.click();
        });
    },
    
    async checkAuth() {
        const token = window.api.getToken();
        if (token) {
            try {
                await window.api.getHealth();
                this.showDashboard();
            } catch (error) {
                this.showLogin();
            }
        } else {
            this.showLogin();
        }
    },
    
    showLogin() {
        document.getElementById('login-screen').style.display = 'flex';
        document.getElementById('dashboard').style.display = 'none';
    },
    
    showDashboard() {
        document.getElementById('login-screen').style.display = 'none';
        document.getElementById('dashboard').style.display = 'grid';
        this.setupDashboard();
        this.loadData();
    },
    
    setupDashboard() {
        // Navigation
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const view = item.dataset.view;
                this.switchView(view);
            });
        });
        
        // Logout
        document.getElementById('logout-btn').addEventListener('click', () => {
            window.api.clearToken();
            window.api.disconnectWebSocket();
            this.showLogin();
        });
        
        // Modal
        const modal = document.getElementById('plugin-modal');
        const closeBtn = modal.querySelector('.modal-close');
        closeBtn.addEventListener('click', () => {
            modal.style.display = 'none';
        });
        
        // Tab switching
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const tab = btn.dataset.tab;
                this.switchTab(tab);
            });
        });
        
        // WebSocket connection
        window.api.connectWebSocket({
            onMessage: (data) => this.handleWebSocketMessage(data)
        });
    },
    
    switchView(view) {
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.toggle('active', item.dataset.view === view);
        });
        
        document.querySelectorAll('.view').forEach(v => {
            v.classList.toggle('active', v.id === `view-${view}`);
        });
        
        if (view === 'plugins') this.loadPlugins();
        if (view === 'metrics') this.loadMetrics();
    },
    
    switchTab(tab) {
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tab);
        });
        
        document.querySelectorAll('.tab-pane').forEach(pane => {
            pane.classList.toggle('active', pane.id === `tab-${tab}`);
        });
        
        if (tab === 'versions' && this.currentPlugin) {
            this.loadPluginReleases(this.currentPlugin);
        }
        if (tab === 'config' && this.currentPlugin) {
            this.loadPluginConfig(this.currentPlugin);
        }
        if (tab === 'metrics' && this.currentPlugin) {
            this.loadPluginMetricsDetail(this.currentPlugin);
        }
    },
    
    async loadData() {
        await Promise.all([
            this.loadServerInfo(),
            this.loadPlugins()
        ]);
        this.updateOverview();
    },
    
    async loadServerInfo() {
        try {
            this.serverInfo = await window.api.getServerInfo();
            document.getElementById('server-info').textContent = 
                `${this.serverInfo.server.name} ${this.serverInfo.server.minecraftVersion}`;
            
            const detailsHTML = `
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;">
                    <div><strong>Server:</strong> ${this.serverInfo.server.name}</div>
                    <div><strong>Version:</strong> ${this.serverInfo.server.minecraftVersion}</div>
                    <div><strong>Players:</strong> ${this.serverInfo.players.online}/${this.serverInfo.players.max}</div>
                    <div><strong>Worlds:</strong> ${this.serverInfo.worlds.count}</div>
                    <div><strong>Memory:</strong> ${this.serverInfo.memory.usedMB}MB / ${this.serverInfo.memory.maxMB}MB</div>
                    <div><strong>TPS:</strong> ${this.serverInfo.tps.current.toFixed(2)}</div>
                </div>
            `;
            document.getElementById('server-details-content').innerHTML = detailsHTML;
        } catch (error) {
            console.error('Failed to load server info', error);
        }
    },
    
    async loadPlugins() {
        try {
            const data = await window.api.getPlugins();
            this.plugins = data.plugins || [];
            this.renderPlugins();
        } catch (error) {
            console.error('Failed to load plugins', error);
        }
    },
    
    renderPlugins() {
        const container = document.getElementById('plugins-list');
        
        if (this.plugins.length === 0) {
            container.innerHTML = '<p>No plugins found</p>';
            return;
        }
        
        container.innerHTML = this.plugins.map(plugin => `
            <div class="plugin-card" data-plugin="${plugin.name}">
                <div class="plugin-header">
                    <div>
                        <div class="plugin-name">${plugin.name}</div>
                        <div class="plugin-version">v${plugin.version}</div>
                    </div>
                    <span class="plugin-status ${plugin.enabled ? 'enabled' : 'disabled'}">
                        ${plugin.enabled ? 'Enabled' : 'Disabled'}
                    </span>
                </div>
                ${plugin.authors && plugin.authors.length > 0 ? `
                    <div class="plugin-authors">By: ${plugin.authors.join(', ')}</div>
                ` : ''}
                <div class="plugin-tags">
                    ${plugin.tags ? plugin.tags.map(tag => `<span class="tag ${tag.toLowerCase()}">${tag}</span>`).join('') : ''}
                </div>
            </div>
        `).join('');
        
        // Add click handlers
        container.querySelectorAll('.plugin-card').forEach(card => {
            card.addEventListener('click', () => {
                this.showPluginModal(card.dataset.plugin);
            });
        });
    },
    
    updateOverview() {
        const enabled = this.plugins.filter(p => p.enabled).length;
        const github = this.plugins.filter(p => p.githubRepo).length;
        
        document.getElementById('stat-total-plugins').textContent = this.plugins.length;
        document.getElementById('stat-enabled-plugins').textContent = enabled;
        document.getElementById('stat-github-plugins').textContent = github;
        
        if (this.serverInfo) {
            const uptimeMs = this.serverInfo.uptime;
            const hours = Math.floor(uptimeMs / (1000 * 60 * 60));
            const minutes = Math.floor((uptimeMs % (1000 * 60 * 60)) / (1000 * 60));
            document.getElementById('stat-uptime').textContent = `${hours}h ${minutes}m`;
        }
    },
    
    async showPluginModal(pluginName) {
        this.currentPlugin = pluginName;
        const modal = document.getElementById('plugin-modal');
        document.getElementById('modal-plugin-name').textContent = pluginName;
        
        try {
            const data = await window.api.getPlugin(pluginName);
            const plugin = data.plugin;
            
            const detailsHTML = `
                <p><strong>Version:</strong> ${plugin.version}</p>
                <p><strong>Status:</strong> ${plugin.enabled ? '✅ Enabled' : '❌ Disabled'}</p>
                <p><strong>Authors:</strong> ${plugin.authors ? plugin.authors.join(', ') : 'Unknown'}</p>
                ${plugin.description ? `<p><strong>Description:</strong> ${plugin.description}</p>` : ''}
                <div style="margin-top: 20px;">
                    <button onclick="app.performAction('${pluginName}', '${plugin.enabled ? 'disable' : 'enable'}')">
                        ${plugin.enabled ? 'Disable' : 'Enable'}
                    </button>
                    <button onclick="app.performAction('${pluginName}', 'reload')" style="margin-left: 10px;">
                        Reload
                    </button>
                </div>
            `;
            
            document.getElementById('plugin-details').innerHTML = detailsHTML;
            modal.style.display = 'flex';
        } catch (error) {
            alert('Failed to load plugin details: ' + error.message);
        }
    },
    
    async performAction(pluginName, action) {
        try {
            await window.api.performPluginAction(pluginName, action);
            alert(`Action "${action}" performed successfully`);
            await this.loadPlugins();
            document.getElementById('plugin-modal').style.display = 'none';
        } catch (error) {
            alert('Action failed: ' + error.message);
        }
    },
    
    async loadPluginReleases(pluginName) {
        try {
            const data = await window.api.getPluginReleases(pluginName);
            const html = data.releases && data.releases.length > 0 ?
                data.releases.map(r => `
                    <div class="card" style="margin-bottom: 15px;">
                        <h4>${r.name || r.tag} ${r.isLatest ? '(Latest)' : ''}</h4>
                        <p><strong>Tag:</strong> ${r.tag}</p>
                        <p><strong>Published:</strong> ${new Date(r.publishedAt).toLocaleString()}</p>
                        ${r.downloadUrl ? `<button onclick="alert('Download functionality coming soon')">Download</button>` : ''}
                    </div>
                `).join('') :
                '<p>No releases found on GitHub</p>';
            
            document.getElementById('plugin-versions').innerHTML = html;
        } catch (error) {
            document.getElementById('plugin-versions').innerHTML = '<p>Failed to load releases</p>';
        }
    },
    
    async loadPluginConfig(pluginName) {
        try {
            const data = await window.api.getConfigFiles(pluginName);
            const html = data.configFiles && data.configFiles.length > 0 ?
                `<p>Config files:</p><ul>${data.configFiles.map(f => `<li>${f}</li>`).join('')}</ul>` :
                '<p>No config files found</p>';
            
            document.getElementById('plugin-config').innerHTML = html;
        } catch (error) {
            document.getElementById('plugin-config').innerHTML = '<p>Failed to load config files</p>';
        }
    },
    
    async loadPluginMetricsDetail(pluginName) {
        try {
            const data = await window.api.getPluginMetrics(pluginName);
            const html = Object.keys(data.metrics).length > 0 ?
                `<pre>${JSON.stringify(data.metrics, null, 2)}</pre>` :
                '<p>No metrics available for this plugin</p>';
            
            document.getElementById('plugin-metrics-detail').innerHTML = html;
        } catch (error) {
            document.getElementById('plugin-metrics-detail').innerHTML = '<p>Failed to load metrics</p>';
        }
    },
    
    async loadMetrics() {
        try {
            const data = await window.api.getMetricsOverview();
            const html = `
                <div class="card">
                    <h3>Metrics Summary</h3>
                    <p>Plugins with metrics: ${data.summary.totalPluginsWithMetrics}</p>
                </div>
                <div class="card mt-20">
                    <h3>All Metrics</h3>
                    <pre>${JSON.stringify(data.allMetrics, null, 2)}</pre>
                </div>
            `;
            document.getElementById('metrics-content').innerHTML = html;
        } catch (error) {
            document.getElementById('metrics-content').innerHTML = '<p>Failed to load metrics</p>';
        }
    },
    
    handleWebSocketMessage(data) {
        console.log('WebSocket message:', data);
        
        if (data.type === 'plugin_state_change') {
            this.loadPlugins();
        } else if (data.type === 'new_release') {
            // Show notification
            console.log('New release available:', data.payload);
        }
    },
    
    showError(element, message) {
        element.textContent = message;
        element.style.display = 'block';
        setTimeout(() => {
            element.style.display = 'none';
        }, 5000);
    }
};

// Initialize app when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => app.init());
} else {
    app.init();
}

// Make app globally available for inline event handlers
window.app = app;
