# Bois Nature Photos — application Android locale

Application native pour tablette Android. Elle ne contient aucun accès Internet et ne recopie pas les photos : elle conserve uniquement les autorisations Android vers les fichiers ou dossiers choisis.

## Fonctions

- Pièces par défaut : Rangement, Salle à manger, Chambre, Salon, Terrasse, Bureau et Salle de bain.
- Ajout de photos ou d'un dossier complet avec lecture récursive.
- Autorisations conservées après fermeture et redémarrage.
- Lecture native des JPG, PNG, WebP et HEIC/HEIF sur Android 9 ou version ultérieure.
- Diaporama toutes les 4 secondes, navigation tactile et suppression logique sans effacer les originaux.
- Fonctionnement totalement hors ligne.

## Obtenir l'APK avec GitHub

1. Décompressez ce projet dans un nouveau dépôt GitHub.
2. Ouvrez l'onglet **Actions** puis **Construire APK Android**.
3. Cliquez sur **Run workflow**.
4. À la fin, téléchargez l'artefact **BoisNaturePhotos-APK**.
5. Extrayez `app-debug.apk`, transférez-le sur la tablette et autorisez l'installation d'applications inconnues pour votre navigateur ou gestionnaire de fichiers.

L'APK de test est signé automatiquement par Android. Pour une diffusion publique, ouvrez le projet dans Android Studio et générez un APK signé avec votre propre clé.
