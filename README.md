# Parl’Anime

**Ta voix anime ton personnage !** Une petite application Android cartoon, simple et amusante. L’analyse audio et le rendu fonctionnent sur ton téléphone, sans remplacer ta voix par une IA.

## Télécharger

[**Télécharger l’APK Android R5**](https://github.com/da23real/ParlAnim/releases/download/parlanim-va01.16-r5/ParlAnime-VA01.16-R5.apk) · [Toutes les versions](https://github.com/da23real/ParlAnim/releases)

Android 8.0 ou plus récent. Ouvre l’APK sur ton téléphone et autorise son installation si Android le demande. Pour une mise à jour, installe par-dessus la version précédente afin de conserver tes réglages.

## Comment ça marche ?

Choisis un personnage, puis enregistre un discours ou importe un audio. Il réagit en quatre états : **attente, chuchote, normal et cris**. Tu peux ajouter un fond et une musique ; « Animateur radio » baisse la musique pendant ta voix.

- **16 personnages** intégrés, dont les animaux imaginaires et le Kawaii synthé en version détaillée. Tu peux aussi importer une planche PNG de personnage (9 colonnes × 4 rangées).
- **5 décors**, une image personnelle ou une couleur de fond.
- **MOV transparent**, **MP4 avec fond**, partage MP4 et **MIDI + audio WAV pour Snkoz**, réunis dans un ZIP.
- Réglages avancés en direct : seuils, lissage, gate micro et puissance de la baisse musicale.

La Communauté n’est pas encore en ligne. La version iPhone n’est pas encore disponible.

## Liens

- [Soutenir le projet sur Ko-fi](https://ko-fi.com/ers115)
- [Ma chaîne YouTube](https://www.youtube.com/@ERS115-23)
- [Snkoz](https://github.com/da23real/Snkoz)

## Sources

Code sous **GPL-3.0-only** — [licence](LICENSE), [provenance des illustrations](ASSETS.md), [confidentialité](PRIVACY.md).

Compilation Windows : JDK 21, Android SDK 35 et Build Tools 35.0.0. Définir `JAVA_HOME`, `ANDROID_HOME`, `PARLANIME_KEYSTORE`, `PARLANIME_KEY_ALIAS`, `PARLANIME_KEY_PASSWORD`, puis lancer `Tools/Build.ps1`. Utilise ta propre clé privée ; une signature différente ne remplace pas l’APK officiel.
