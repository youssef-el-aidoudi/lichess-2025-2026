# README – Installation & Démarrage

Ce document décrit, dans l’ordre, les étapes pour préparer l’environnement, construire les images et lancer l’application.

---

## 0) Cloner le dépôt

> **Note** : le serveur GitLab a un certificat SSL mal configuré. Deux options :
>
> **Option A – Désactiver globalement (rapide mais attention quand vous allez cloner d'autres repos)**
>
> ```bash
> git config --global http.sslVerify false
> git clone https://gitlabvigan.iem/m1projettutore2025-2026-groupe4/lichess-2025-2026.git
> cd lichess-2025-2026
> ```
>
> **Option B – Désactiver au cas par cas (recommandé mais il faut ajouter le prefix pour chaque push/pull etc.)**
>
> ```bash
> git -c http.sslVerify=false clone https://gitlabvigan.iem/m1projettutore2025-2026-groupe4/lichess-2025-2026.git
> cd lichess-2025-2026
> ```

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

## 6) Configuration de MinIO

### Se connecter à l’interface MinIO

* **URL de développement (par défaut)** : [https://storage.docker.localhost/](https://storage.docker.localhost/)
  *(Adaptez selon votre nom de domaine.)*
* À l’écran de connexion : **Other Authentication Methods** → **Use Credentials**.
* **Utilisateur** : `minio_admin`
* **Mot de passe** : valeur définie lors de l’exécution de `generate_env.py` (ou dans votre `.env`).

Les identifiants root se configurent dans le fichier `.env` via :

```
MINIO_ROOT_USER=minio_admin
MINIO_ROOT_PASSWORD=CHANGEME
```

### Créer une Access Key

1. Ouvrir **Access Keys** dans la barre latérale gauche.
2. Cliquer sur **Create access key +**.
3. Cliquer sur **Create** pour générer une nouvelle paire (Access/Secret).
4. **Copier** les valeurs générées et mettre à jour votre `.env` :

```
# Identifiants d’accès applicatifs MinIO
MINIO_ACCESS_KEY=PASTE_ACCESS_KEY_HERE
MINIO_SECRET_KEY=PASTE_SECRET_KEY_HERE
```

> Astuce : conservez ces clés en lieu sûr et ne les versionnez jamais.

Points à prévoir :

* Où récupérer les identifiants (UI MinIO, variables par défaut, procédure d’initialisation…).
* Quelles variables ajouter dans `.env` (ex. `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_REGION`, `MINIO_BUCKET_*`, etc.).
* Bonnes pratiques (droits minimaux, rotation des secrets, stockage hors VCS).

---

## 7) Notes & dépannage (rapide)

* Si `docker ps` échoue sans `sudo`, vérifiez votre appartenance au groupe `docker` et reconnectez votre session.
* En cas d’alerte SSL dans le navigateur, c’est attendu en **dev** avec un certificat auto-signé.
* Assurez-vous que toutes les valeurs sensibles de `.env` sont bien définies avant `docker compose up`.
