import requests
import json

# Configuration
BASE_URL = "http://localhost:8080"
TOKEN = "Caca123!..."  # Token récupéré de votre sélection

# Headers communs pour l'authentification
HEADERS = {
    "Authorization": f"Bearer {TOKEN}",
    "Content-Type": "application/json"
}

def print_section(title):
    print(f"\n{'='*20} {title} {'='*20}")

def log_response(response):
    """Affiche le code de statut et le JSON formaté de la réponse."""
    print(f"Status: {response.status_code}")
    try:
        if response.headers.get('Content-Type', '').startswith('application/json'):
            print(json.dumps(response.json(), indent=2))
        else:
            print(f"Response (Text): {response.text}")
    except Exception as e:
        print(f"Erreur lors du parsing JSON: {e}")

def test_health():
    print_section("TEST: Health Check (No Auth)")
    try:
        # L'endpoint health ne nécessite pas de token selon le README
        response = requests.get(f"{BASE_URL}/api/health")
        log_response(response)
    except requests.exceptions.ConnectionError:
        print("ERREUR: Impossible de se connecter au serveur. Vérifiez qu'il est bien lancé.")

def test_server_info():
    print_section("TEST: Server Info")
    response = requests.get(f"{BASE_URL}/api/server", headers=HEADERS)
    log_response(response)

def test_plugins_list():
    print_section("TEST: Liste des Plugins")
    response = requests.get(f"{BASE_URL}/api/plugins", headers=HEADERS)
    log_response(response)
    
    # Retourne la liste pour l'utiliser dans d'autres tests si besoin
    if response.status_code == 200:
        return response.json()
    return []

def test_metrics_overview():
    print_section("TEST: Metrics Overview")
    response = requests.get(f"{BASE_URL}/api/metrics/overview", headers=HEADERS)
    log_response(response)

def test_single_plugin(plugin_name):
    print_section(f"TEST: Détails du plugin '{plugin_name}'")
    
    # 1. Infos générales
    print(f"--- Info {plugin_name} ---")
    response = requests.get(f"{BASE_URL}/api/plugins/{plugin_name}", headers=HEADERS)
    log_response(response)

    # 2. Métriques spécifiques (MetricsController)
    print(f"\n--- Métriques {plugin_name} ---")
    response_metrics = requests.get(f"{BASE_URL}/api/plugins/{plugin_name}/metrics", headers=HEADERS)
    log_response(response_metrics)

    # 3. Configuration
    print(f"\n--- Config {plugin_name} ---")
    response_config = requests.get(f"{BASE_URL}/api/plugins/{plugin_name}/config", headers=HEADERS)
    log_response(response_config)

def test_action_plugin(plugin_name, action):
    """
    Test une action (enable/disable/reload).
    ATTENTION: Cela affecte le serveur réel.
    """
    print_section(f"TEST: Action '{action}' sur '{plugin_name}'")
    payload = {"action": action}
    response = requests.post(
        f"{BASE_URL}/api/plugins/{plugin_name}/action", 
        headers=HEADERS, 
        json=payload
    )
    log_response(response)

if __name__ == "__main__":
    print(f"Démarrage des tests sur {BASE_URL} avec le token '{TOKEN}'...")
    
    # 1. Tests de base
    test_health()
    test_server_info()
    test_metrics_overview()
    
    # 2. Récupérer les plugins et tester sur 'PluginManagerWeb' par défaut
    plugins = test_plugins_list()
    
    target_plugin = "PluginManagerWeb"
    test_single_plugin(target_plugin)

    # 3. Exemple d'action (Commenté pour la sécurité)
    # test_action_plugin(target_plugin, "reload")
