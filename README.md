# Parl’Anime

**Ta voix anime ton personnage !** Une petite application Android cartoon, simple et amusante. L’analyse audio et le rendu fonctionnent sur ton téléphone, sans remplacer ta voix par une IA.

## Télécharger

[**Télécharger l’APK Android R8**](https://github.com/da23real/ParlAnim/releases/download/android-VA01.16-R8/ParlAnime-VA01.16-R8.apk) · [Toutes les versions](https://github.com/da23real/ParlAnim/releases)

Android 8.0 ou plus récent. Ouvre l’APK sur ton téléphone et autorise son installation si Android le demande. Pour une mise à jour, installe par-dessus la version précédente afin de conserver tes réglages.

[**Voir la démonstration vidéo**](https://youtube.com/shorts/3oMdc-9sEQU)

## Comment ça marche ?

Choisis un personnage, puis enregistre un discours ou importe un audio. Il réagit en quatre états : **attente, chuchote, normal et cris**. Tu peux ajouter un fond et une musique ; « Animateur radio » baisse la musique pendant ta voix.

- **16 personnages** intégrés, dont les animaux imaginaires. Importe aussi un ZIP de quatre GIF de neuf images ou quatre planches PNG 3 × 3, ou sélectionne les fichiers séparément. Le bouton « Importer une animation… » est en haut du choix de personnage. Les anciennes planches PNG 9 × 4 restent acceptées.
- **5 décors**, une image personnelle ou une couleur de fond.
- **MOV transparent**, **MP4 avec fond**, partage MP4 et **MIDI + audio WAV pour Snkoz**, réunis dans un ZIP.
- Réglages avancés en direct : seuils, lissage, gate micro et puissance de la baisse musicale.

La Communauté n’est pas encore en ligne. La version iPhone n’est pas encore disponible.

## Liens

- [Soutenir le projet sur Ko-fi](https://ko-fi.com/ers115)
- [Ma chaîne YouTube](https://www.youtube.com/@ERS115-23)
- [Snkoz](https://github.com/da23real/Snkoz)

## Sources

L’APK publié est en R8. Le dépôt et ses archives automatiques contiennent encore les sources R5 ; ils ne correspondent pas au binaire R8.


Code sous **GPL-3.0-only** — [licence](LICENSE), [provenance des illustrations](ASSETS.md), [confidentialité](PRIVACY.md).

Compilation Windows : JDK 21, Android SDK 35 et Build Tools 35.0.0. Définir `JAVA_HOME`, `ANDROID_HOME`, `PARLANIME_KEYSTORE`, `PARLANIME_KEY_ALIAS`, `PARLANIME_KEY_PASSWORD`, puis lancer `Tools/Build.ps1`. Utilise ta propre clé privée ; une signature différente ne remplace pas l’APK officiel.
