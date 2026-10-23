# ClassHeure — Audit Architectural & Plan de Reconstruction (Clean Architecture + Jetpack Compose)

Reconstruction complète de **ClassHeure** (carnet de bord pédagogique tout-en-un bilingue Français/Arabe pour enseignants des cycles Primaire, CEM et Lycée) selon les standards modernes **Kotlin, Jetpack Compose (Material 3), et Clean Architecture modulaire**, tout en préservant 100 % des fonctionnalités, des thèmes visuels, des animations et des documents officiels de l'archive existante.

## Choix Confirmés par l'Utilisateur & Décisions Clés

> [!IMPORTANT]
> Les choix suivants ont été validés lors de la phase de clarification et gouvernent l'intégralité de la reconstruction :

- **Persistance des Données Confirmée** : Base de données locale **Room (SQLite + KSP)** couplée au système de **sauvegarde/restauration chiffrée AES-256 (PBKDF2WithHmacSHA256)** et rétrocompatibilité V1/V2.
- **Direction Visuelle Confirmée** : Conservation intégrale de l'identité visuelle actuelle, incluant les **8 palettes de couleurs** (`default`, `emerald`, `indigo`, `purple`, `amber`, `teal`, `rose`, `slate`), les modes Clair / Sombre / Améthyste, la carte profil enseignant animée (`autumn_tree`, `aurora`, `fireflies`, `stardust`), le style Glassmorphisme et les 12 polices locales FR/AR.
- **Périmètre Fonctionnel Confirmé** : Conservation de **100 % des fonctionnalités** (10 modules métiers, calcul des moyennes algériennes CEM/Lycée sur 20 et Primaire sur 10, génération PDF A4 native bilingue RTL/LTR, synthèse hebdomadaire IA via Gemini, et textes réglementaires officiels embarqués).

---

## 1. Résumé Exhaustif des Fonctionnalités Détectées dans l'Archive

L'analyse complète des **49 fichiers sources et ressources** de `projet_propre_108f84e0.zip` confirme la présence des briques fonctionnelles suivantes :

### A. Tableau de Bord & Profil Enseignant (`HomeScreen`, `AutumnTeacherProfileCard`, `OnboardingLevelDialog`)
- **Onboarding initial** : Sélection du cycle d'enseignement (`PRIMAIRE`, `CEM`, `LYCEE` / `SECONDAIRE`), de la langue (`fr` / `ar`), du titre (`M.`, `Mme`, `Mlle`), du nom de l'enseignant, de l'établissement et de la matière enseignée.
- **Carte Profil Enseignant Animée** : Bannière interactive avec photo de profil recadrée, informations de l'établissement, année scolaire active et **4 effets d'animation Canvas temps réel** (Arbre d'automne avec chute de feuilles, Aurore boréale, Lucioles, Poussière d'étoiles) + vagues chromatiques (`ChromaticWavesCanvas`).
- **Suivi des Séances en Temps Réel** : Détection automatique du cours « En cours » et du prochain cours « À venir » selon l'heure actuelle et l'emploi du temps du jour, avec raccourci direct vers l'emploi du temps ou l'appel.
- **Grille d'Accès Rapide Artistique** : Cartes illustrées (`ArtisticToolGridCard`) donnant accès aux modules principaux et secondaires.

### B. Gestion des Classes, Élèves, Groupes & Plan de Classe (`StudentsScreen`, `HorizontalSeatingChartView`)
- **Gestion multi-classes et années scolaires** : Création/modification/suppression de classes avec coefficient par matière, gestion multi-années scolaires et archivage d'années.
- **Dossiers Élèves Complets** : Nom, prénom, date de naissance (saisie assistée par boîtes `BirthDateBoxesInput`), lieu de naissance, sexe (`M`/`F`), redoublements (`1AM`, `2AM`, `1AS`, etc.), groupe (`G1`, `G2`...), observations et note « Bon à savoir ».
- **Division Automatique & Manuelle en Groupes** : Répartition par ordre alphabétique, aléatoire ou équilibrée par genre, ou affectation manuelle.
- **Plan de Classe Interactif (Vertical & Horizontal)** : Attribution de places sur grille de pupitres, permutation par glisser/clic (`swapStudentSeats`), attribution alphabétique automatique, libération de place et vue dédiée en mode paysage (`HorizontalSeatingChartView`).
- **Import / Export CSV & Statistiques Démographiques** : Import/export de listes d'élèves en CSV, pyramide des âges, répartition filles/garçons et taux de redoublement (`ClassStatisticsView`).

### C. Carnet de Notes & Calculateur Pédagogique (`GradebookScreen`, `GradeCalculator`)
- **Gestion par Trimestre (T1, T2, T3)** : Saisie fluide avec normalisation automatique des chiffres arabes orientaux (`٠-٩`) et virgules (`،`).
- **Formules Officielles Adaptées au Cycle** :
  - *CEM & Lycée (sur 20)* : Évaluation continue (Participation + Discipline + Assiduité + Cahier = sur 20) + Projet + Moyenne des Devoirs (1 à 3 devoirs) + Composition $\times$ Coefficient de la matière.
  - *Primaire (sur 10)* : Adaptation automatique du barème sur 10 avec omission des sous-notes d'évaluation continue non applicables.
- **Statistiques & Graphiques de Performance** : Moyenne de classe, min/max, taux de réussite ($\ge 10/20$ ou $\ge 5/10$), distribution par tranches de notes et export PDF du relevé.

### D. Carnet d'Absences & Retards (`AttendanceScreen`)
- **Appel par Séance & Créneau Horaire** : Sélection automatique ou manuelle de la classe, de la date et du créneau horaire (avec filtrage des créneaux de réception des parents).
- **Statuts Détaillés** : `PRESENT`, `ABSENT`, `RETARD` (avec saisie des minutes de retard), `EXCLUDED` (exclusion), justification (`isJustified`) et motif (`reason`).
- **Sauvegarde Automatique & Historique** : Enregistrement instantané (`autoSaveAttendances`), historique groupé par séance (`SessionGroup`), bilan individuel par élève et rapport PDF d'assiduité.

### E. Emplois du Temps & Réception des Parents (`TimetablesScreen`)
- **Double Vue (Journalière & Grille Hebdomadaire)** : Emploi du temps consolidé de l'enseignant et emploi du temps par classe (Dimanche à Jeudi, avec suppression automatique du week-end Vendredi/Samedi).
- **Personnalisation Avancée des Créneaux** : Modification globale des horaires (`updateTimeSlotGlobally`), pause déjeuner personnalisable, ajout de créneaux sur mesure, décomposition en heures unitaires, jour libre de l'enseignant (`teacherFreeDay`) et créneau de réception des parents (`ParentReceptionDialog`).
- **Couleurs Intelligentes** : Attribution cohérente de couleurs par classe (`ClassColorHelper`) et par matière (`SubjectColorHelper`).

### F. Cahier Journal & Synthèse IA Gemini (`JournalScreen`, `GeminiSummaryHelper`)
- **Entrées Pédagogiques Structurées** : Date, créneau horaire, classe, matière, activité, titre de la leçon, objectifs pédagogiques, observations, et pièces jointes multimédias (photo et note audio).
- **Organisation par Trimestre** : Filtrage par classe, période ou recherche textuelle.
- **Assistant Pédagogique IA (Gemini REST API)** : Génération automatique d'un bilan hebdomadaire professionnel en Français ou en Arabe à partir des entrées du cahier journal.

### G. Gestionnaire de Documents & Textes Réglementaires (`DocumentsScreen`, `RegulatoryDocumentsHelper`)
- **Arborescence de Dossiers & Sous-dossiers** : Catégories racines (« Progressions annuelles », « Fiches de cours », « Textes réglementaires ») et sous-dossiers illimités.
- **Documents Officiels Embarqués (`assets/documents/`)** :
  - Journal Officiel N° 04 de 2025 (`A2025004.pdf` / `Decret_Executif_25-54_et_25-55.pdf` — Statut particulier des fonctionnaires de l'Éducation Nationale).
  - Journal Officiel N° 04 de 2008 (`A2008004.pdf` / `Loi_08-04_Orientation_Education_Nationale.pdf` — Loi d'orientation sur l'Éducation Nationale).
- **Protection & Importation** : Verrouillage contre la suppression des textes officiels, importation de fichiers ou d'arborescences complètes (`importDirectoryTree`) et archivage automatique des PDF générés.

### H. Rappels avec Alarmes, Bloc-Notes, Sécurité & Paramètres (`RemindersScreen`, `NotesScreen`, `PinLockScreen`, `SettingsScreen`, `PdfExporter`, `BackupHelper`)
- **Rappels & Notifications Exactes** : Programmation via `AlarmManager` (`ReminderNotificationReceiver`) avec reprogrammation automatique au redémarrage de l'appareil (`BOOT_COMPLETED`).
- **Prise de Notes Rapide** : Création, édition, recherche et suppression de notes personnelles.
- **Verrouillage PIN & Biométrie** : Écran de verrouillage par code PIN et authentification par empreinte digitale / biométrie (`BiometricHelper`) avec reverrouillage automatique en arrière-plan (`onStop`).
- **Moteur d'Export PDF A4 Natif (`PdfExporter`)** : 9 types de rapports PDF (Dossier complet, Emploi du temps individuel/global, Relevé de notes, Plan de classe, Liste d'élèves, Statistiques de classe, Cahier journal, Rapport d'absences) avec support complet de l'Arabe (RTL) et des polices personnalisées.
- **Sauvegarde Chiffrée AES-256 & Envoi Gmail** : Export/Import intégral de la base de données chiffré par mot de passe (`PBKDF2WithHmacSHA256`) et partage direct par e-mail.

---

## 2. Audit Technique : Erreurs, Points Faibles et Incohérences de l'Ancienne Version

L'inspection du code source révèle plusieurs problèmes structurels majeurs typiques d'une génération monolithique non refactorisée :

### A. Fichiers « God Object » Monolithiques & Formatage Corrompu
1. **Tailles de fichiers hors normes** :
   - `AttendanceScreen.kt` atteint **11 908 lignes** (gonflé par un espacement quadruple de lignes vides et des milliers de lignes de logique métier mêlée à l'UI).
   - `TimetablesScreen.kt` (**4 794 lignes**), `SettingsScreen.kt` (**4 716 lignes**), `StudentsScreen.kt` (**4 509 lignes**), `PdfExporter.kt` (**3 860 lignes**), `GradebookScreen.kt` (**3 419 lignes**) et `AppViewModel.kt` (**3 075 lignes**).
2. **Conséquence directe** : Recompositions Compose massives, lenteurs d'édition/compilation, impossibilité de maintenir ou tester unitairement les sous-composants.

### B. Violations de l'Architecture (Couplage Fort UI ↔ Data ↔ Context)
1. **Un seul `AppViewModel` et un seul `AppDao` pour 11 tables** : Toutes les fonctionnalités (notes, absences, horaires, documents, paramètres, PDF, CSV, alarmes) sont concentrées dans une seule classe `AppViewModel` qui reçoit directement des `Context` Android dans ses méthodes (`addReminder(context, ...)`, `exportSingleTimetablePdf(context, ...)`).
2. **Logique de persistance et d'export dans les Composables** : Par exemple, `AttendanceScreen.kt` construit lui-même des fichiers PDF et des requêtes de filtrage complexes, et `DocumentsScreen.kt` effectue des copies de fichiers I/O (`copyUriToInternalStorage`) directement depuis le fichier d'interface.
3. **Référence statique dangereuse dans `MainActivity`** : `appViewModel` est conservé dans une propriété mutable de l'Activity et manipulé dans `onStop`/`onDestroy`, tandis qu'un `CompositionLocal` bricolé (`LocalActivityContext`) contourne le cycle de vie standard de Compose.

### C. Incohérences dans l'Internationalisation (Triple Système Concurrent)
1. **Trois mécanismes de traduction redondants et contradictoires** :
   - `res/values/strings.xml` et `res/values-ar/strings.xml` (Ressources Android standard).
   - `assets/i18n/fr.json` et `assets/i18n/ar.json` lus par `I18n.kt` avec un état global mutable.
   - `Tr.kt` (776 lignes) contenant un dictionnaire codé en dur et des expressions régulières (`Regex("""Classe '(.+)' ajoutée...""")`) pour traduire à la volée des chaînes françaises écrites en dur dans le ViewModel !
2. **API dépréciée de changement de langue** : `MainActivity.kt` utilise `resources.updateConfiguration(config, displayMetrics)` (déprécié depuis API 25) à l'intérieur même d'un bloc `remember` pendant la composition, provoquant des effets de bord imprévisibles lors du basculement RTL/LTR.

### D. Bugs & Erreurs d'Implémentation Spécifiques Relevés
1. **Bug d'Onboarding dans `MainActivity.kt` (lignes 194-198)** :
   ```kotlin
   LaunchedEffect(currentSettings.hasCompletedOnboarding) {
     if (!currentSettings.hasCompletedOnboarding) {
       viewModel.updateEducationLevel("SECONDAIRE")
     }
   }
   ```
   Alors que `SettingsEntity` utilise `"CEM_LYCEE"` ou `"PRIMAIRE"`, ce `LaunchedEffect` écrase silencieusement la valeur par `"SECONDAIRE"` à chaque démarrage tant que l'onboarding n'est pas complété.
2. **Incohérence du Modèle Gemini & Permissions Réseau** :
   - Le commentaire de `GeminiSummaryHelper.kt` annonce `gemini-3.5-flash`, mais l'URL codée en dur appelle `gemini-2.5-flash` avec `JSONObject` manuel au lieu d'un client typé `kotlinx.serialization` + `Retrofit` sur le modèle standard `gemini-3.5-flash`.
   - **Erreur critique dans `AndroidManifest.xml`** : La permission `<uses-permission android:name="android.permission.INTERNET" />` est **totalement absente** du Manifest de l'archive ! Tout appel à l'API Gemini échoue donc systématiquement avec une `SecurityException` / erreur réseau sur un appareil réel.
3. **Permissions de Stockage Obsolètes et Non-Conformes** :
   - `AndroidManifest.xml` déclare `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `USE_FINGERPRINT` et `requestLegacyExternalStorage="true"`, qui sont obsolètes sur Android 13–16 (API 33–36).
4. **Duplication d'Assets volumineux** :
   - Les fichiers PDF officiels sont dupliqués sous deux noms différents dans `assets/documents/` (`A2008004.pdf` = `Loi_08-04_Orientation_Education_Nationale.pdf`, et `A2025004.pdf` = `Decret_Executif_25-54_et_25-55.pdf`), et les logos PNG (`ic_classheure_logo.png`, `ic_home_logo.png`, `ic_maclasse_logo.png`) sont triplés à l'identique, alourdissant inutilement l'APK de plusieurs mégaoctets.

---

## 3. Architecture Cible & Organisation Modulaire (Clean Architecture)

Pour éliminer les fichiers monolithiques et garantir une maintenabilité exemplaire (chaque fichier source étant strictement maintenu sous 400–500 lignes), le projet sera structuré en **3 couches claires (Data, Domain, Presentation/UI)** accompagnées d'un module **Core** transversal :

```
┌───────────────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER (Jetpack Compose)                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │  Home &      │  │ Classes,     │  │ Gradebook &  │  │ Attendance &  │  │
│  │  Onboarding  │  │ Groups, Seat │  │ Statistics   │  │ Timetables    │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └───────┬───────┘  │
│  ┌──────┴───────┐  ┌──────┴───────┐  ┌──────┴───────┐  ┌───────┴───────┐  │
│  │ Journal & AI │  │ Documents &  │  │ Reminders &  │  │ Settings, PIN │  │
│  │ Summary      │  │ Official PDF │  │ Quick Notes  │  │ & Backup UI   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └───────┬───────┘  │
│         └─────────────────┴────────┬────────┴──────────────────┘          │
│                                    ▼                                      │
│               Feature ViewModels + Immutable UI StateFlows                │
└────────────────────────────────────┬──────────────────────────────────────┘
                                     ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                        DOMAIN LAYER (Logique Métier)                      │
│  ┌────────────────────────┐ ┌──────────────────────┐ ┌─────────────────┐  │
│  │ GradeCalculatorUseCase │ │ Seating & Grouping   │ │ Timetable &     │  │
│  │ (CEM/Lycée/Primaire)   │ │ Algorithms UseCase   │ │ Slot Resolver   │  │
│  └────────────────────────┘ └──────────────────────┘ └─────────────────┘  │
│  ┌────────────────────────┐ ┌──────────────────────┐ ┌─────────────────┐  │
│  │ PdfReportGenerator     │ │ EncryptedBackup      │ │ GeminiSummary   │  │
│  │ (9 Rapports A4 FR/AR)  │ │ Manager (AES-256)    │ │ UseCase         │  │
│  └────────────────────────┘ └──────────────────────┘ └─────────────────┘  │
└────────────────────────────────────┬──────────────────────────────────────┘
                                     ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                         DATA LAYER (Room + Réseau)                        │
│  ┌─────────────────────────────────────────────────┐ ┌─────────────────┐  │
│  │ Room Database (11 Entités, DAOs Spécialisés)    │ │ Retrofit Client │  │
│  │ SchoolDao | GradeDao | ScheduleDao | UtilityDao │ │ (gemini-3.5-    │  │
│  └─────────────────────────────────────────────────┘ │  flash REST)    │  │
│  ┌─────────────────────────────────────────────────┐ └─────────────────┘  │
│  │ AssetSeeder (Textes Réglementaires 2008 & 2025) │                      │
│  └─────────────────────────────────────────────────┘                      │
└───────────────────────────────────────────────────────────────────────────┘
```

### Organisation des Packages du Nouveau Projet (`com.example`)
- **`core/`**
  - `di/` : Conteneur d'injection de dépendances léger (`AppContainer`) fournissant les Repositories, UseCases et ViewModelFactories sans fuite de `Context`.
  - `i18n/` : Gestionnaire de localisation unifié Bilingue Français / Arabe (RTL/LTR automatique, pluriels arabes à 6 formes, normalisation des chiffres `٠-٩`).
  - `security/` : `CryptoManager` (PBKDF2 + AES-256 V1/V2) et `BiometricAuthManager`.
  - `notifications/` : `ReminderScheduler` et `ReminderNotificationReceiver` (AlarmManager + Boot Receiver).
  - `pdf/` : Moteur PDF modulaire découpé par type de document (`PdfThemeUtils`, `TimetablePdfRenderer`, `GradebookPdfRenderer`, `AttendancePdfRenderer`, `JournalPdfRenderer`, `StudentPdfRenderer`, `DossierPdfRenderer`).
- **`data/`**
  - `local/entity/` : Les 11 entités Room annotées et indexées (`SettingsEntity`, `ClassEntity`, `StudentEntity`, `GradeEntity`, `AttendanceEntity`, `TimetableSlotEntity`, `JournalEntryEntity`, `DocumentCategoryEntity`, `DocumentEntity`, `ReminderEntity`, `NoteEntity`).
  - `local/dao/` : Découpage propre des requêtes réactives `Flow<T>` et `suspend` avec les migrations Room 9 à 20 préservées.
  - `remote/` : Service Retrofit + `kotlinx.serialization` pour l'API Gemini (`gemini-3.5-flash`).
  - `repository/` : Implémentations propres des repositories (`SchoolRepository`, `PedagogyRepository`, `ProductivityRepository`, `SettingsRepository`).
- **`domain/`**
  - `model/` : Modèles métiers enrichis (`StudentWithGrade`, `SessionAttendanceSummary`, `ClassStatistics`, `LessonSlotStatus`).
  - `usecase/` : Calculateur de moyennes algérien (`GradeCalculator`), répartiteur de groupes et plan de classe, gestionnaire d'import/export CSV et sauvegarde chiffrée.
- **`ui/`**
  - `theme/` : Les 8 thèmes de couleurs, modes Clair/Sombre/Améthyste, et les 12 polices locales (`Cairo`, `Tajawal`, `Almarai`, `Amiri`, `Lora`, `Times New Roman`, `Dancing Script`, `Great Vibes`, `Aref Ruqaa`, `Diwani`, `Decotype Thuluth`, `Noto Nastaliq Urdu`).
  - `components/` : Composants réutilisables (`AutumnTeacherProfileCard` avec ses 4 effets Canvas, `GlassmorphicCard`, `FineSearchBar`, `AdaptiveScreenContainer`, `OnboardingLevelDialog`, `PinLockScreen`).
  - `navigation/` : Navigation Compose typée avec barre de navigation adaptative et gestion propre du bouton Retour (`BackHandler`).
  - `screens/` : Sous-packages dédiés et modulaires pour chaque écran (`home/`, `students/`, `gradebook/`, `attendance/`, `timetables/`, `journal/`, `documents/`, `reminders/`, `notes/`, `settings/`).

---

## 4. Plan de Reconstruction Étape par Étape

Voici la feuille de route séquentielle que nous allons exécuter dès votre validation :

### Étape 1 : Configuration Gradle, Manifest, Polices, Icône & Ressources Officiels
1. Configurer `metadata.json`, `settings.gradle.kts` (`ClassHeure`), et `app/build.gradle.kts` (`applicationId` unique, activation de Room KSP, Retrofit, Kotlinx Serialization, Biometric, DocumentFile et Secrets Gradle Plugin avec `GEMINI_API_KEY` dans `.env.example`).
2. Mettre à jour `AndroidManifest.xml` avec la permission `INTERNET` (requise pour Gemini), `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_BIOMETRIC`, `RECEIVE_BOOT_COMPLETED`, le `FileProvider` et la suppression des permissions de stockage obsolètes.
3. Installer les polices locales dans `res/font/`, les logos et l'icône adaptative personnalisée de `ClassHeure`, les traductions FR/AR (`strings.xml`, `ar.json`, `fr.json`) et les PDF réglementaires officiels (`A2008004.pdf` et `A2025004.pdf`) sans doublons inutiles.

### Étape 2 : Couche Data (Room Database v20, DAOs, Gemini API & Sauvegarde AES-256)
1. Créer les 11 entités Room et `AppDatabase` (incluant toutes les migrations pour garantir la compatibilité totale avec vos sauvegardes existantes).
2. Implémenter `AppRepository` avec des `Flow` réactifs et des transactions sécurisées.
3. Implémenter `BackupHelper` / `CryptoHelper` (chiffrement/déchiffrement AES-256 V2 + rétrocompatibilité V1) et `RegulatoryDocumentsHelper` (pré-chargement automatique des décrets 2025 et loi 2008).
4. Implémenter le service IA `GeminiSummaryHelper` proprement avec `gemini-3.5-flash` en arrière-plan (`Dispatchers.IO`) et gestion claire des erreurs.

### Étape 3 : Couche Domain & Utilitaires Métier (Calculs, I18n, Couleurs & Moteur PDF)
1. Construire le système bilingue unifié (`I18n` + `Tr` + support des 6 formes de pluriel arabe et bascule RTL/LTR instantanée sans API dépréciée).
2. Implémenter `GradeCalculator` (formules exactes CEM/Lycée sur 20 et Primaire sur 10, normalisation des saisies arabes).
3. Implémenter `ClassColorHelper`, `SubjectColorHelper`, `FormatHelper`, `BiometricHelper` et `WindowAdaptiveHelper`.
4. Reconstruire et modulariser le moteur `PdfExporter` pour générer les 9 rapports PDF A4 bilingues (avec en-têtes officiels, tableaux paginés et polices arabes/latines intégrées).

### Étape 4 : Système de Design, Composants Artistiques & État Global
1. Mettre en place `Theme.kt`, `Color.kt` et `Type.kt` avec les 8 palettes de couleurs, le mode sombre/améthyste et l'échelle typographique dynamique (`small`, `medium`, `large`, `bold`).
2. Recréer les composants signatures : `AutumnTeacherProfileCard` (avec les 4 animations Canvas temps réel : Arbre d'automne, Aurore, Lucioles, Poussière d'étoiles), `GlassmorphicCard`, `FineSearchBar`, `OnboardingLevelDialog`, `PinLockScreen` et `AboutDialog`.
3. Structurer le `AppViewModel` (et ses délégués métiers) avec `collectAsStateWithLifecycle`.

### Étape 5 : Reconstruction Modulaire des 10 Écrans Métiers
1. **Accueil (`HomeScreen`)** : Carte profil animée, détection du cours en direct/à venir, vagues chromatiques et grille d'outils.
2. **Classes & Élèves (`StudentsScreen` + `HorizontalSeatingChartView`)** : Liste numérotée, fiches élèves, création de groupes, plan de classe interactif portrait/paysage, statistiques démographiques et import/export CSV.
3. **Carnet de Notes (`GradebookScreen`)** : Saisie par trimestre T1/T2/T3, calcul temps réel, filtres de groupe et statistiques de réussite.
4. **Carnet d'Absences (`AttendanceScreen`)** : Appel rapide par créneau, gestion des retards/justifications/motifs, historique par séance et bilan par élève (proprement découpé en sous-composants lisibles).
5. **Emplois du Temps (`TimetablesScreen`)** : Vue quotidienne et grille hebdomadaire, édition globale des créneaux, pause déjeuner, jour libre et réception des parents.
6. **Cahier Journal (`JournalScreen`)** : Entrées par trimestre, pièces jointes photo/audio et génération du résumé hebdomadaire via Gemini.
7. **Documents (`DocumentsScreen`)**, **Rappels (`RemindersScreen` + `ReminderNotificationReceiver`)**, **Notes (`NotesScreen`)** et **Paramètres (`SettingsScreen`)** (Personnalisation, années scolaires, sécurité PIN/Biométrie, sauvegarde AES-256 et partage Gmail).

### Étape 6 : Intégration Finale, Tests Unitaires/Robolectric & Compilation
1. Assembler `MainActivity.kt` avec `enableEdgeToEdge()`, gestion propre des `WindowInsets` et navigation fluide.
2. Vérifier les tests (`I18nArabicTest`, `ExampleRobolectricTest`) et valider la compilation complète via `compile_applet` avec zéro erreur.
