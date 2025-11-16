# PluginManagerWeb

PluginManagerWeb est un plugin Minecraft (Paper 1.20+) optionnel qui centralise le management et la supervision des plugins développés par **MatisseAD**. À la manière du web editor de LuckPerms, il expose un tableau de bord web embarqué permettant :

* la découverte et l’inventaire des plugins installés ;
* la collecte de statistiques (uptime, consommation mémoire, compteurs personnalisables) ;
* l’affichage et l’installation de nouvelles versions disponibles sur GitHub ;
* l’édition en ligne des fichiers de configuration ;
* l’exécution d’actions à chaud comme reload/enable/disable ;
* l’accès via une API REST et WebSocket pour l’intégration avec des outils externes.

## Fonctionnement

Le plugin démarre un serveur HTTP(S) embarqué grâce à **Javalin** lors de l’initialisation du serveur Minecraft. Un tableau de bord accessible via un navigateur permet aux administrateurs de surveiller l’état des différents plugins, de consulter les métriques et d’effectuer des actions. Toutes les actions sensibles sont contrôlées par un système d’authentification à jeton et, si disponible, une intégration avec LuckPerms.

Les dépendances principales sont la Paper API (version 1.20.4 ou supérieure) et Javalin pour le serveur web. Le build est configuré avec Gradle et la tâche `shadowJar` génère un jar autonome comprenant les dépendances nécessaires.

## Installation

1. Compilez le projet avec `./gradlew shadowJar` pour obtenir un jar dans `build/libs/`.
2. Placez le jar `PluginManagerWeb.jar` dans le dossier `plugins` de votre serveur Paper.
3. Démarrez votre serveur Minecraft. Un fichier `config.yml` sera généré si absent ; ajustez les paramètres selon votre environnement (port web, jeton d’authentification, référentiels GitHub).
4. Accédez à l’URL indiquée dans la console (par défaut `http://<adresse IP>:8080/`) et utilisez le jeton d’authentification pour vous connecter.

## Configuration

Le fichier `config.yml` contient toutes les options permettant de personnaliser le comportement du plugin : activation/désactivation du serveur web, port d’écoute, activation du TLS, liste blanche IP, jeton API, référentiels GitHub surveillés, options de base de données, etc. Un exemple complet de configuration se trouve dans `src/main/resources/config.yml`.

## Structure du projet

- `src/main/java/fr/matissead/pluginmanagerweb/PluginManagerWeb.java` : classe principale qui initialise le plugin et démarre le serveur web.
- `src/main/java/fr/matissead/pluginmanagerweb/WebServer.java` : wrapper autour de Javalin pour démarrer, arrêter et configurer le serveur.
- `src/main/resources/plugin.yml` : définition Bukkit/Paper du plugin.
- `src/main/resources/config.yml` : configuration par défaut.
- `build.gradle` : script Gradle pour la compilation et la génération du jar.

## Exemple d’utilisation

Après installation, connectez-vous au tableau de bord et vous verrez la liste des plugins installés avec leur version et leur état. Vous pourrez :

* consulter des statistiques d’utilisation (par exemple, nombre de commandes exécutées pour ReanimateMC) ;
* visualiser les releases disponibles sur GitHub et déclencher une mise à jour ;
* éditer la configuration d’un plugin directement dans le navigateur et sauvegarder vos modifications ;
* exécuter des commandes de maintenance (reload, enable, disable) ;
* recevoir des notifications en temps réel via WebSocket lorsqu’un plugin change d’état ou qu’une nouvelle version est publiée.

## Avertissements

Ce projet est un squelette de départ. De nombreuses fonctionnalités (collecte fine de métriques, gestion avancée des erreurs, protection contre l’exécution arbitraire de code lors des mises à jour, etc.) restent à implémenter. Le choix du framework web (ici Javalin) et de la version cible de Paper (1.20+) sont des recommandations susceptibles d’évoluer.

Pour toute contribution ou suggestion, n’hésitez pas à ouvrir une *issue* ou une *pull request*.
