# Clinica

Clinica est une application mobile Android développée en Kotlin avec Jetpack Compose, destinée à la gestion d’une clinique médicale.

## Description détaillée

L’application propose plusieurs parcours utilisateurs : patient, médecin et administrateur. Elle inclut :

- un écran d’introduction et de connexion
- un parcours d’inscription et d’authentification Firebase
- un tableau de bord patient avec accès à la recherche de médecins, réservation de rendez-vous et conversations
- un tableau de bord médecin avec gestion de disponibilité et profil
- un tableau de bord administrateur avec ajout et édition des médecins
- un module de calendrier pour ajouter des rendez-vous dans l’agenda Android
- une carte interactive pour localiser les médecins via OSMdroid

## Technologies utilisées

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Firebase Authentication
- Firebase Firestore
- Ktor (client Android, content negotiation, JSON)
- OSMdroid (cartographie)
- Coil Compose (chargement d’images)
- AndroidX Lifecycle et ViewModel
- SplashScreen API
- Kotlinx Serialization

## Architecture et modules

- `app/` : module principal Android
- `app/src/main/java/com/example/healthconnect/` : code source de l’application
- `app/src/main/AndroidManifest.xml` : permissions et activité principale
- `app/build.gradle.kts` : configuration du module Android
- `build.gradle.kts` : configuration du projet racine
- `settings.gradle.kts` : inclusion des modules Gradle
- `gradle/` : wrapper Gradle et version management

## Fonctionnalités principales

- Authentification et inscription des utilisateurs avec Firebase
- Gestion des rôles : patient, médecin, administrateur
- Recherche de médecins par spécialité
- Fiche détaillée du médecin avec messagerie et prise de rendez-vous
- Vue carte pour trouver un médecin et accéder à ses détails
- Envoi de messages entre utilisateur et médecin
- Gestion du profil utilisateur
- Ajout de rendez-vous au calendrier Android

## Prérequis

- Android Studio (version recommandée : Arctic Fox ou supérieure)
- JDK 11
- SDK Android 36
- Connexion Internet pour télécharger les dépendances et accéder à Firebase

## Installation et exécution

### Depuis Android Studio

1. Ouvrir Android Studio.
2. Sélectionner `Open an existing project`.
3. Choisir le dossier racine du projet `Clinica`.
4. Laisser Gradle synchroniser le projet.
5. Lancer l’application sur un émulateur ou un appareil connecté.

### Depuis la ligne de commande

Depuis le répertoire racine du projet :

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat installDebug
```

Si vous êtes sous Windows avec Git Bash ou WSL :

```bash
./gradlew clean assembleDebug
./gradlew installDebug
```

## Configuration Firebase

Le projet contient déjà le fichier `app/google-services.json`.

Si vous souhaitez changer de projet Firebase :

1. Remplacer `app/google-services.json` par votre propre fichier Firebase.
2. Vérifier que les produits Firebase Auth et Firestore sont activés.

## Permissions utilisées

- `INTERNET`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_FINE_LOCATION`
- `WRITE_CALENDAR`
- `READ_CALENDAR`

## Informations du projet

- `applicationId` : `com.example.healthconnect`
- `compileSdk` : 36
- `minSdk` : 24
- `targetSdk` : 36
- `versionCode` : 1
- `versionName` : `1.0`

---

Bon développement avec Clinica !
