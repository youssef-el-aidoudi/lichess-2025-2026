# README – Installation & Démarrage

Ce document décrit, dans l’ordre, les étapes pour préparer l’environnement, construire les images et lancer l’application.

---

## 1) Prérequis : installer Docker & Docker Compose

Suivre la documentation officielle de votre système pour installer **Docker** et **Docker Compose**.

**Vérifier l’installation :**

```bash
docker --version
docker compose version
```

### Ajouter votre utilisateur au groupe `docker`

Pour exécuter Docker sans `sudo` :

```bash
sudo usermod -aG docker $USER
```

> Déconnectez-vous puis reconnectez-vous (ou redémarrez la machine) pour appliquer le changement.

**Contrôle rapide :**

```bash
docker ps
```

Si la commande s’exécute sans erreur, vous êtes bien dans le groupe `docker`.

---

## 2) Configuration de l’environnement – fichier `.env`

Le fichier `.env` centralise les variables de configuration du projet (ports, URLs, identifiants, secrets, etc.).

1. Copier le modèle fourni :

   ```bash
   cp .env.example .env
   ```
2. Ouvrir `.env` et adapter **les valeurs** à votre contexte local (domaines, ports, mots de passe, clés d’API…).
3. Conserver `.env` **en local** (ne pas le committer si le repository est public) car il contient des secrets.

> **Note :** Plusieurs services (Traefik, MinIO, etc.) lisent leurs paramètres depuis ce fichier. Vérifiez que **toutes** les clés requises sont renseignées avant de démarrer.

---

## 3) Générer les certificats de développement (Traefik)

Se placer dans le répertoire `traefik` :

```bash
cd traefik
```

Générer la clé privée RSA :

```bash
openssl genrsa -out private.key 2048
```

Générer le certificat auto-signé (remplacez `docker.localhost` par votre domaine si nécessaire) :

```bash
openssl req -new -x509 -key private.key -out cert.pem -days 365 -subj "/CN=docker.localhost"
```

> **Remarque :** Les navigateurs signaleront un avertissement (connexion non sécurisée) car le certificat n’est pas signé par une autorité de confiance. Pour un usage **développement**, cela reste suffisant.

---

## 4) Construire les images et lancer les conteneurs

Après avoir rempli le `.env`, construire et lancer :

```bash
docker compose up --build
```

---

## 5) Créer un **GitLab Personal Access Token**

1. Ouvrir **GitLab** → **Preferences** → **Access Tokens**.
2. Cliquer sur **Add new token** et choisir un nom explicite.
3. Cocher les permissions :

   * ✅ `api`
   * ✅ `read_registry`
   * ✅ `write_registry`
4. Cliquer sur **Create personal access token** et **copier immédiatement** le token affiché.

> Conservez ce token en sécurité ; il pourra être référencé via le `.env` si nécessaire.

---

## 6) (À compléter) MinIO : accès, jetons et permissions

> **Section à rédiger** : préciser comment obtenir/configurer les identifiants MinIO (access key / secret key), les politiques/permissions, l’éventuel **access_token**, et comment les exposer via le `.env` pour manipuler MinIO depuis les services/outils du projet.

Points à prévoir :

* Où récupérer les identifiants (UI MinIO, variables par défaut, procédure d’initialisation…).
* Quelles variables ajouter dans `.env` (ex. `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_REGION`, `MINIO_BUCKET_*`, etc.).
* Bonnes pratiques (droits minimaux, rotation des secrets, stockage hors VCS).

---

## 7) Notes & dépannage (rapide)

* Si `docker ps` échoue sans `sudo`, vérifiez votre appartenance au groupe `docker` et reconnectez votre session.
* En cas d’alerte SSL dans le navigateur, c’est attendu en **dev** avec un certificat auto-signé.
* Assurez-vous que toutes les valeurs sensibles de `.env` sont bien définies avant `docker compose up`.
