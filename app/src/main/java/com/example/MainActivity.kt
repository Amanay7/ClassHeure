package com.example

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import com.example.ui.components.GlassmorphicBackground
import com.example.ui.components.GlassmorphicCard
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ExitToApp
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import com.example.util.I18n
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.SettingsEntity
import com.example.ui.AppViewModel
import com.example.ui.AppViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

val LocalActivityContext = staticCompositionLocalOf<Context?> { null }

/**
 * Activité principale "ClassHeure".
 * Initialise l'accès à la base Room, configure le ViewModel, et lance l'architecture de navigation.
 */
class MainActivity : androidx.fragment.app.FragmentActivity() {
  var navigateToRoute by mutableStateOf<String?>(null)
  private var appViewModel: AppViewModel? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    navigateToRoute = intent?.getStringExtra("navigate_to")
    enableEdgeToEdge()

    // Initialisation locale sécurisée de Room & Repository
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = AppRepository(database.appDao())
    val factory = AppViewModelFactory(repository)

    // Initialisation automatique du dossier dédié "ClassHeure"
    com.example.util.AppStorageHelper.ensureAppDirectory(applicationContext)

    setContent {
      val viewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
      appViewModel = viewModel

      LaunchedEffect(Unit) {
        viewModel.seedRegulatoryDocs(applicationContext)
      }

      val settingsState by viewModel.settings.collectAsState()

      if (settingsState == null) {
        // En attente du chargement de la base de données
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator()
        }
      } else {
        val currentSettings = settingsState!!
        val pinCode = currentSettings.pinCode

        val isUnlocked by viewModel.isUnlocked.collectAsState()

        // Auto-déverrouillage si aucun PIN n'est défini
        LaunchedEffect(pinCode) {
            if (pinCode.isNullOrBlank()) {
                viewModel.setUnlocked(true)
            }
        }

        // Direction automatique de l'interface (RTL pour l'Arabe, LTR pour le Français)
        val isArabic = currentSettings.activeLanguage == "ar"
        com.example.util.Tr.forcedLanguage = currentSettings.activeLanguage
        com.example.util.I18n.setLanguage(currentSettings.activeLanguage)
        val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

        val baseContext = LocalContext.current
        val (localizedContext, localizedConfig) = remember(currentSettings.activeLanguage) {
            val locale = java.util.Locale(currentSettings.activeLanguage)
            java.util.Locale.setDefault(locale)
            val config = android.content.res.Configuration(baseContext.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
            Pair(baseContext.createConfigurationContext(config), config)
        }

        LaunchedEffect(currentSettings.activeLanguage) {
            I18n.init(baseContext)
            I18n.setLanguage(currentSettings.activeLanguage)
        }

        val registryOwner = baseContext as ActivityResultRegistryOwner
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection,
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfig,
            LocalActivityResultRegistryOwner provides registryOwner,
            LocalActivityContext provides baseContext
        ) {
          MyApplicationTheme(
            themeName = currentSettings.activeTheme,
            fontFamilyName = currentSettings.activeFont,
            fontSizeScaleName = currentSettings.fontSizeScale,
            isFontBold = currentSettings.isFontBold,
            activeLanguage = currentSettings.activeLanguage
          ) {
            if (!pinCode.isNullOrBlank() && !isUnlocked) {
              PinLockScreen(
                correctPin = pinCode,
                isBiometricEnabled = currentSettings.isBiometricEnabled,
                activity = this@MainActivity,
                onUnlockSuccess = { viewModel.setUnlocked(true) }
              )
            } else {
              LaunchedEffect(currentSettings.hasCompletedOnboarding) {
                if (!currentSettings.hasCompletedOnboarding) {
                  viewModel.updateEducationLevel("SECONDAIRE")
                }
              }

              MainAppLayout(viewModel = viewModel)
            }
          }
        }
      }
    }
  }

  private var backgroundedAtMillis: Long = 0L

  override fun onStart() {
    super.onStart()
    val stopTime = backgroundedAtMillis
    backgroundedAtMillis = 0L
    if (stopTime > 0L && (System.currentTimeMillis() - stopTime) > 120_000L) {
      appViewModel?.setUnlocked(false)
    }
  }

  override fun onResume() {
    super.onResume()
  }

  override fun onStop() {
    super.onStop()
    if (!isChangingConfigurations) {
      backgroundedAtMillis = System.currentTimeMillis()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (!isChangingConfigurations) {
      appViewModel?.setUnlocked(false)
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    navigateToRoute = intent.getStringExtra("navigate_to")
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: AppViewModel) {
  val navController = rememberNavController()
  val statusMessage by viewModel.statusMessage.collectAsState()
  val context = LocalContext.current
  val activityContext = LocalActivityContext.current

  val activity = activityContext as? MainActivity
  val navigateTo = activity?.navigateToRoute
  LaunchedEffect(navigateTo) {
    if (!navigateTo.isNullOrBlank()) {
      navController.navigate(navigateTo) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
      activity.navigateToRoute = null
    }
  }
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: "home"

  // Handle exit dialog state
  var showExitDialog by remember { mutableStateOf(false) }
  var showAboutDialog by remember { mutableStateOf(false) }

  // BackHandler for home screen
  BackHandler(enabled = currentRoute == "home") {
    showExitDialog = true
  }

  // About dialog
  if (showAboutDialog) {
    AboutDialog(
      onDismissRequest = { showAboutDialog = false }
    )
  }

  // Exit dialog
  if (showExitDialog) {
    AlertDialog(
      onDismissRequest = { showExitDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Default.ExitToApp,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(32.dp)
        )
      },
      title = {
        Text(
          text = stringResource(id = R.string.exit_dialog_title),
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleMedium
        )
      },
      text = {
        Text(
          text = stringResource(id = R.string.confirm_exit),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showExitDialog = false
            viewModel.setUnlocked(false)
            (activityContext as? ComponentActivity)?.finishAffinity()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          )
        ) {
          Text(text = stringResource(id = R.string.exit_yes), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showExitDialog = false }
        ) {
          Text(text = stringResource(id = R.string.exit_no))
        }
      }
    )
  }

  val settingsState by viewModel.settings.collectAsState()
  val currentSettings = settingsState ?: com.example.data.SettingsEntity()
  val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
  val isDarkTheme = when (currentSettings.activeTheme) {
    "light" -> false
    "dark" -> true
    else -> systemInDark
  }
  val layoutDirection = if (currentSettings.activeLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

  // Affichage dynamique et réactif des notifications de statut (ex: Absences, Retards, Exclusions enregistrés)
  var activeToast by remember { mutableStateOf<Toast?>(null) }
  LaunchedEffect(statusMessage) {
    statusMessage?.let { rawMsg ->
      val isAr = (currentSettings.activeLanguage == "ar") || com.example.util.Tr.isArabicLang()
      val localizedMsg = com.example.util.Tr.translateNotificationMessage(rawMsg, isAr)
      activeToast?.cancel()
      val newToast = Toast.makeText(context, localizedMsg, Toast.LENGTH_SHORT)
      activeToast = newToast
      newToast.show()
      viewModel.clearStatusMessage()

      // Disparition rapide et fluide après 1.2s pour ne pas bloquer l'écran
      kotlinx.coroutines.delay(1200)
      if (activeToast == newToast) {
        newToast.cancel()
        activeToast = null
      }
    }
  }

  GlassmorphicBackground(
    modifier = Modifier
      .fillMaxSize()
      .then(if (showAboutDialog) Modifier.blur(16.dp) else Modifier)
  ) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      topBar = {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (currentRoute == "home") {
            val isLight = !isDarkTheme
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
              color = MaterialTheme.colorScheme.surface
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(88.dp)
                  .padding(horizontal = 12.dp, vertical = 2.dp)
              ) {
                // Settings button on Start side
                Surface(
                  modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(42.dp)
                    .shadow(
                      elevation = if (isDarkTheme) 4.dp else 2.5.dp,
                      shape = RoundedCornerShape(12.dp),
                      ambientColor = Color(0xFF2563EB).copy(alpha = if (isLight) 0.15f else 0.35f),
                      spotColor = Color(0xFF3B82F6).copy(alpha = if (isLight) 0.25f else 0.45f)
                    )
                    .border(
                      width = 1.1.dp,
                      brush = Brush.linearGradient(
                        listOf(
                          if (isLight) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.35f),
                          Color(0xFF3B82F6).copy(alpha = if (isLight) 0.40f else 0.45f),
                          Color(0xFF2563EB).copy(alpha = if (isLight) 0.15f else 0.20f)
                        )
                      ),
                      shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                      navController.navigate("settings") {
                        launchSingleTop = true
                      }
                    },
                  shape = RoundedCornerShape(12.dp),
                  color = if (isLight) {
                    Color(0xFF2563EB).copy(alpha = 0.08f).compositeOver(Color.White)
                  } else {
                    Color(0xFF2563EB).copy(alpha = 0.16f).compositeOver(Color(0xFF141722))
                  }
                ) {
                  Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                  ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                      val w = size.width
                      drawLine(
                        brush = Brush.horizontalGradient(
                          listOf(
                            Color.Transparent,
                            if (isLight) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.30f),
                            Color.Transparent
                          )
                        ),
                        start = Offset(w * 0.15f, 1.2f),
                        end = Offset(w * 0.85f, 1.2f),
                        strokeWidth = 1.2f
                      )
                    }

                    Icon(
                      imageVector = Icons.Default.Settings,
                      contentDescription = "Settings",
                      tint = if (isLight) Color(0xFF2563EB) else Color(0xFF60A5FA),
                      modifier = Modifier.size(22.dp)
                    )
                  }
                }

                // Centered large, prominent Logo (design identique en Français et en Arabe)
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center,
                  modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 40.dp)
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { showAboutDialog = true }
                  ) {
                    Image(
                      painter = painterResource(id = R.drawable.ic_home_logo),
                      contentDescription = "ClassHeure",
                      modifier = Modifier.height(64.dp),
                      contentScale = ContentScale.Fit
                    )
                  }
                }
              }
            }
          } else {
            CenterAlignedTopAppBar(
              modifier = Modifier.fillMaxWidth(),
              title = {
                val titleText = if (currentSettings.activeLanguage == "ar") {
                  when (currentRoute) {
                    "students" -> "قائمة التلاميذ"
                    "gradebook" -> "التنقيط"
                    "journal" -> "اليومية"
                    "timetables" -> "مواقيت"
                    "settings" -> "الإعدادات"
                    "reminders" -> "التذكيرات"
                    "notes" -> "الملاحظات"
                    "documents" -> "وثائقي"
                    "attendances" -> "دفتر الغيابات"
                    else -> stringResource(id = R.string.app_name)
                  }
                } else {
                  when (currentRoute) {
                    "students" -> stringResource(id = R.string.nav_students)
                    "gradebook" -> stringResource(id = R.string.nav_gradebook)
                    "journal" -> stringResource(id = R.string.nav_journal)
                    "timetables" -> stringResource(id = R.string.nav_timetables)
                    "settings" -> stringResource(id = R.string.nav_settings)
                    "reminders" -> stringResource(id = R.string.nav_reminders)
                    "notes" -> stringResource(id = R.string.nav_notes)
                    "documents" -> stringResource(id = R.string.nav_documents)
                    "attendances" -> stringResource(id = R.string.nav_attendances)
                    else -> stringResource(id = R.string.app_name)
                  }
                }
                Text(
                  text = titleText,
                  fontWeight = FontWeight.Bold
                )
              },
              navigationIcon = {
                if (currentRoute == "settings" || currentRoute == "documents" || currentRoute == "notes" || currentRoute == "reminders" || currentRoute == "attendances") {
                  IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "Back"
                    )
                  }
                }
              },
              actions = {},
              colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
              )
            )
          }
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            thickness = 1.dp
          )
        }
      },
      bottomBar = {
      val navBackStackEntryInner by navController.currentBackStackEntryAsState()
      val currentRouteInner = navBackStackEntryInner?.destination?.route

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding(),
        shape = androidx.compose.ui.graphics.RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDarkTheme) 0.45f else 0.65f),
            thickness = 1.dp
          )
          NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 680.dp)
              .height(62.dp)
          ) {
        val studentsColor = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
        val studentsBg = if (isDarkTheme) Color(0xFF065F46).copy(alpha = 0.45f) else Color(0xFFD1FAE5)
        val isStudentsSelected = currentRouteInner == "students"
        NavigationBarItem(
          icon = {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isStudentsSelected) Color(0xFF059669) else studentsBg),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.People,
                contentDescription = "Students",
                tint = if (isStudentsSelected) Color.White else studentsColor,
                modifier = Modifier.size(17.dp)
              )
            }
          },
          label = {
            Text(
              text = stringResource(id = R.string.nav_students),
              fontWeight = if (isStudentsSelected) FontWeight.Bold else FontWeight.Medium,
              fontSize = 10.sp,
              lineHeight = 12.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          },
          selected = isStudentsSelected,
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedTextColor = studentsColor,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          onClick = {
            if (currentRouteInner != "students") {
              navController.navigate("students") {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            }
          }
        )

        val gradebookColor = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
        val gradebookBg = if (isDarkTheme) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFEF3C7)
        val isGradebookSelected = currentRouteInner == "gradebook"
        NavigationBarItem(
          icon = {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isGradebookSelected) Color(0xFFD97706) else gradebookBg),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.Assignment,
                contentDescription = "Grades",
                tint = if (isGradebookSelected) Color.White else gradebookColor,
                modifier = Modifier.size(17.dp)
              )
            }
          },
          label = {
            Text(
              text = stringResource(id = R.string.nav_gradebook),
              fontWeight = if (isGradebookSelected) FontWeight.Bold else FontWeight.Medium,
              fontSize = 10.sp,
              lineHeight = 12.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          },
          selected = isGradebookSelected,
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedTextColor = gradebookColor,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          onClick = {
            if (currentRouteInner != "gradebook") {
              navController.navigate("gradebook") {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            }
          }
        )

        val isHomeSelected = currentRouteInner == "home"
        NavigationBarItem(
          icon = {
            Box(
              modifier = Modifier
                .size(32.dp)
                .shadow(elevation = if (isHomeSelected) 4.dp else 1.5.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                  if (isHomeSelected) {
                    Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))
                  } else {
                    Brush.verticalGradient(
                      if (isDarkTheme) listOf(Color(0xFF2563EB), Color(0xFF1E3A8A))
                      else listOf(Color(0xFF60A5FA), Color(0xFF2563EB))
                    )
                  }
                )
                .border(
                  width = 1.5.dp,
                  color = if (isHomeSelected) Color.White else (if (isDarkTheme) Color(0xFF93C5FD).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f)),
                  shape = CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.Home,
                contentDescription = "Home",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
          },
          label = {
            Text(
              text = stringResource(id = R.string.nav_home),
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
              lineHeight = 12.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          },
          selected = isHomeSelected,
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedTextColor = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
            unselectedTextColor = if (isDarkTheme) Color(0xFF93C5FD).copy(alpha = 0.85f) else Color(0xFF2563EB)
          ),
          onClick = {
            if (currentRouteInner != "home") {
              navController.navigate("home") {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = false
              }
            }
          }
        )

        val journalColor = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF7C3AED)
        val journalBg = if (isDarkTheme) Color(0xFF4C1D95).copy(alpha = 0.45f) else Color(0xFFF3E8FF)
        val isJournalSelected = currentRouteInner == "journal"
        NavigationBarItem(
          icon = {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isJournalSelected) Color(0xFF7C3AED) else journalBg),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.Book,
                contentDescription = "Journal",
                tint = if (isJournalSelected) Color.White else journalColor,
                modifier = Modifier.size(17.dp)
              )
            }
          },
          label = {
            Text(
              text = stringResource(id = R.string.nav_journal),
              fontWeight = if (isJournalSelected) FontWeight.Bold else FontWeight.Medium,
              fontSize = 10.sp,
              lineHeight = 12.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          },
          selected = isJournalSelected,
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedTextColor = journalColor,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          onClick = {
            if (currentRouteInner != "journal") {
              navController.navigate("journal") {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            }
          }
        )

        val timetablesColor = if (isDarkTheme) Color(0xFFFB7185) else Color(0xFFE11D48)
        val timetablesBg = if (isDarkTheme) Color(0xFF881337).copy(alpha = 0.45f) else Color(0xFFFFE4E6)
        val isTimetablesSelected = currentRouteInner == "timetables"
        NavigationBarItem(
          icon = {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isTimetablesSelected) Color(0xFFE11D48) else timetablesBg),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.CalendarToday,
                contentDescription = "Timetables",
                tint = if (isTimetablesSelected) Color.White else timetablesColor,
                modifier = Modifier.size(17.dp)
              )
            }
          },
          label = {
            Text(
              text = stringResource(id = R.string.nav_timetables),
              fontWeight = if (isTimetablesSelected) FontWeight.Bold else FontWeight.Medium,
              fontSize = 10.sp,
              lineHeight = 12.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          },
          selected = isTimetablesSelected,
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedTextColor = timetablesColor,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          onClick = {
            if (currentRouteInner != "timetables") {
              navController.navigate("timetables") {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            }
          }
        )
          }
        }
      }
    }
  ) { innerPadding ->
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
      NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.padding(innerPadding),
        enterTransition = { fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing)) },
        exitTransition = { fadeOut(animationSpec = tween(110, easing = FastOutLinearInEasing)) },
        popEnterTransition = { fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing)) },
        popExitTransition = { fadeOut(animationSpec = tween(110, easing = FastOutLinearInEasing)) }
      ) {
        composable("home") {
          HomeScreen(viewModel = viewModel) { route ->
            navController.navigate(route) {
              popUpTo(navController.graph.startDestinationId) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          }
        }
        composable("students") {
          StudentsScreen(viewModel = viewModel)
        }
        composable("gradebook") {
          GradebookScreen(viewModel = viewModel)
        }
        composable("journal") {
          JournalScreen(viewModel = viewModel)
        }
        composable("timetables") {
          TimetablesScreen(viewModel = viewModel)
        }
        composable("settings") {
          SettingsScreen(viewModel = viewModel)
        }
        composable("reminders") {
          RemindersScreen(viewModel = viewModel)
        }
        composable("notes") {
          NotesScreen(viewModel = viewModel)
        }
        composable("documents") {
          DocumentsScreen(viewModel = viewModel)
        }
        composable("attendances") {
          AttendanceScreen(viewModel = viewModel)
        }
      }
    }
  }
  }
}

@Composable
fun QuillWithDripIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Define a rich, glowing gold-amber-bronze gradient for the feather body
        val featherGradientLeft = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFEF3C7), // Bright glowing cream/gold highlight
                Color(0xFFFBBF24), // Vibrant Amber
                Color(0xFFD97706), // Rich warm Bronze
                Color(0xFF92400E), // Auburn/chestnut brown
                Color(0xFF451A03)  // Deep mahogany shadow
            ),
            start = Offset(width * 0.85f, height * 0.1f),
            end = Offset(width * 0.28f, height * 0.82f)
        )

        val featherGradientRight = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFDE68A), // Warm gold highlight
                Color(0xFFD97706), // Rich Bronze
                Color(0xFFB45309), // Amber-brown
                Color(0xFF78350F), // Dark bronze-brown
                Color(0xFF271302)  // Deep charcoal/shadow
            ),
            start = Offset(width * 0.85f, height * 0.1f),
            end = Offset(width * 0.28f, height * 0.82f)
        )

        // 1. Draw Left Vane with realistic organic split contours inspired by the photo
        val vaneLeft = Path().apply {
            moveTo(width * 0.85f, height * 0.1f)
            // Left outer edge curves out with beautiful splits
            cubicTo(
                width * 0.40f, height * 0.01f,
                width * 0.04f, height * 0.25f,
                width * 0.15f, height * 0.52f
            )
            // Organic split 1
            lineTo(width * 0.24f, height * 0.46f)
            lineTo(width * 0.14f, height * 0.56f)
            // Left lower edge curves
            cubicTo(
                width * 0.10f, height * 0.64f,
                width * 0.18f, height * 0.72f,
                width * 0.32f, height * 0.72f
            )
            // Inner curve back to tip along the curved stem
            cubicTo(
                width * 0.32f, height * 0.72f,
                width * 0.58f, height * 0.42f,
                width * 0.85f, height * 0.1f
            )
            close()
        }

        // 2. Draw Right Vane with corresponding elegant contours
        val vaneRight = Path().apply {
            moveTo(width * 0.85f, height * 0.1f)
            // Right outer edge curves with elegant sweeping splits
            cubicTo(
                width * 0.99f, height * 0.25f,
                width * 0.86f, height * 0.54f,
                width * 0.52f, height * 0.68f
            )
            // Organic split 2
            lineTo(width * 0.58f, height * 0.60f)
            lineTo(width * 0.44f, height * 0.70f)
            // Right lower curve back to base
            cubicTo(
                width * 0.36f, height * 0.74f,
                width * 0.30f, height * 0.74f,
                width * 0.32f, height * 0.72f
            )
            // Inner curve back along the stem
            cubicTo(
                width * 0.32f, height * 0.72f,
                width * 0.58f, height * 0.42f,
                width * 0.85f, height * 0.1f
            )
            close()
        }

        // Draw Left & Right Vanes
        drawPath(path = vaneLeft, brush = featherGradientLeft)
        drawPath(path = vaneRight, brush = featherGradientRight, alpha = 0.9f)

        // 3. Draw Fine, Elegant Texture Lines (Barbs) branching from the curved stem to outer edges
        // Interpolating along the cubic bezier curve of the stem
        for (i in 1..15) {
            val ratio = i / 16f
            val t = ratio
            val mt = 1f - t
            // Cubic Bezier interpolation: P0=(0.85, 0.1), P1=(0.65, 0.35), P2=(0.40, 0.60), P3=(0.28, 0.82)
            val stemX = mt*mt*mt * (width * 0.85f) + 3*mt*mt*t * (width * 0.65f) + 3*mt*t*t * (width * 0.40f) + t*t*t * (width * 0.28f)
            val stemY = mt*mt*mt * (height * 0.1f) + 3*mt*mt*t * (height * 0.35f) + 3*mt*t*t * (height * 0.60f) + t*t*t * (height * 0.82f)

            // Left barb (pointing slightly upwards-outwards with sweeping curves)
            val leftBarbLength = width * 0.24f * (1.1f - ratio)
            val leftBarb = Path().apply {
                moveTo(stemX, stemY)
                quadraticTo(
                    stemX - leftBarbLength * 0.7f, stemY - leftBarbLength * 0.5f,
                    stemX - leftBarbLength * 1.3f, stemY - leftBarbLength * 0.2f
                )
            }
            drawPath(
                path = leftBarb,
                color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f)
            )

            // Right barb (pointing slightly downwards-outwards)
            val rightBarbLength = width * 0.18f * (1.1f - ratio)
            val rightBarb = Path().apply {
                moveTo(stemX, stemY)
                quadraticTo(
                    stemX + rightBarbLength * 0.8f, stemY + rightBarbLength * 0.3f,
                    stemX + rightBarbLength * 1.2f, stemY + rightBarbLength * 0.5f
                )
            }
            drawPath(
                path = rightBarb,
                color = Color(0xFFD97706).copy(alpha = 0.3f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )
        }

        // 4. Draw the central sweeping gold-bronze shaft (quill stem)
        val stemStroke = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFBEB), // Glowing base
                Color(0xFFFBBF24), // Gold
                Color(0xFFB45309), // Amber-brown
                Color(0xFF451A03)  // Rich shadow
            ),
            start = Offset(width * 0.85f, height * 0.1f),
            end = Offset(width * 0.28f, height * 0.82f)
        )
        drawPath(
            path = Path().apply {
                moveTo(width * 0.85f, height * 0.1f)
                cubicTo(
                    width * 0.65f, height * 0.35f,
                    width * 0.40f, height * 0.60f,
                    width * 0.28f, height * 0.82f
                )
            },
            brush = stemStroke,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 5.5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // 5. Draw metallic gold nib tip at (0.28, 0.82)
        drawCircle(
            color = Color(0xFFD4AF37),
            radius = 6.5f,
            center = Offset(width * 0.28f, height * 0.82f)
        )

        // 6. Draw dripping golden ink drop suspended below the tip
        val dropPath = Path().apply {
            moveTo(width * 0.28f, height * 0.84f)
            // Left curve of the teardrop
            cubicTo(
                width * 0.20f, height * 0.90f,
                width * 0.18f, height * 0.98f,
                width * 0.28f, height * 0.98f
            )
            // Right curve of the teardrop
            cubicTo(
                width * 0.38f, height * 0.98f,
                width * 0.36f, height * 0.90f,
                width * 0.28f, height * 0.84f
            )
            close()
        }

        // Paint the ink drop with a glowing multi-stop gold-bronze-amber radial gradient
        drawPath(
            path = dropPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFDF5), // Intense bright white-gold highlight
                    Color(0xFFFBBF24), // Warm golden amber
                    Color(0xFFD4AF37), // Metallic gold
                    Color(0xFF92400E)  // Warm shadow bronze
                ),
                center = Offset(width * 0.28f, height * 0.93f),
                radius = width * 0.16f
            )
        )
    }
}
