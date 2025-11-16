/**
 * API client for PluginManagerWeb backend
 */
class PluginManagerAPI {
    constructor() {
        this.baseURL = window.location.origin;
        this.token = null;
        this.ws = null;
    }

    setToken(token) {
        this.token = token;
        localStorage.setItem('pmw_token', token);
    }

    getToken() {
        if (!this.token) {
            this.token = localStorage.getItem('pmw_token');
        }
        return this.token;
    }

    clearToken() {
        this.token = null;
        localStorage.removeItem('pmw_token');
    }

    async request(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (this.token && !options.skipAuth) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        const response = await fetch(url, {
            ...options,
            headers
        });

        if (response.status === 401) {
            this.clearToken();
            throw new Error('Unauthorized - please login again');
        }

        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Unknown error' }));
            throw new Error(error.message || `HTTP ${response.status}`);
        }

        return response.json();
    }

    // Health & Server
    async getHealth() {
        return this.request('/api/health', { skipAuth: true });
    }

    async getServerInfo() {
        return this.request('/api/server');
    }

    // Plugins
    async getPlugins() {
        return this.request('/api/plugins');
    }

    async getPlugin(name) {
        return this.request(`/api/plugins/${encodeURIComponent(name)}`);
    }

    async performPluginAction(name, action) {
        return this.request(`/api/plugins/${encodeURIComponent(name)}/action`, {
            method: 'POST',
            body: JSON.stringify({ action })
        });
    }

    async getPluginReleases(name) {
        return this.request(`/api/plugins/${encodeURIComponent(name)}/releases`);
    }

    // Configuration
    async getConfigFiles(pluginName) {
        return this.request(`/api/plugins/${encodeURIComponent(pluginName)}/config`);
    }

    async getConfigFile(pluginName, path) {
        return this.request(`/api/plugins/${encodeURIComponent(pluginName)}/config/file?path=${encodeURIComponent(path)}`);
    }

    async saveConfigFile(pluginName, path, content, reloadPlugin = false) {
        return this.request(`/api/plugins/${encodeURIComponent(pluginName)}/config/file`, {
            method: 'POST',
            body: JSON.stringify({ path, content, reloadPlugin })
        });
    }

    async getConfigBackups(pluginName) {
        return this.request(`/api/plugins/${encodeURIComponent(pluginName)}/config/backups`);
    }

    async rollbackConfig(pluginName, backupId) {
        return this.request(`/api/plugins/${encodeURIComponent(pluginName)}/config/rollback`, {
            method: 'POST',
            body: JSON.stringify({ backupId })
        });
    }

    // Metrics
    async getPluginMetrics(name) {
        return this.request(`/api/plugins/${encodeURIComponent(name)}/metrics`);
    }

    async getMetricsOverview() {
        return this.request('/api/metrics/overview');
    }

    // WebSocket
    connectWebSocket(handlers = {}) {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsURL = `${protocol}//${window.location.host}/ws/events`;
        
        this.ws = new WebSocket(wsURL);
        
        this.ws.onopen = () => {
            console.log('WebSocket connected');
            if (handlers.onOpen) handlers.onOpen();
        };
        
        this.ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                if (handlers.onMessage) handlers.onMessage(data);
            } catch (e) {
                console.error('Failed to parse WebSocket message', e);
            }
        };
        
        this.ws.onerror = (error) => {
            console.error('WebSocket error', error);
            if (handlers.onError) handlers.onError(error);
        };
        
        this.ws.onclose = () => {
            console.log('WebSocket closed');
            if (handlers.onClose) handlers.onClose();
            
            // Auto-reconnect after 5 seconds
            setTimeout(() => this.connectWebSocket(handlers), 5000);
        };
    }

    disconnectWebSocket() {
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
    }
}

// Global API instance
window.api = new PluginManagerAPI();
