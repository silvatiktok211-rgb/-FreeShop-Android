package com.afilishop.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.afilishop.app.ui.components.AfiliBottomBar
import com.afilishop.app.ui.components.AfiliFloatingActions
import com.afilishop.app.ui.components.NativePushRegistrar
import com.afilishop.app.notifications.AfiliShopMessagingService
import com.afilishop.app.notifications.notificationDestination
import com.afilishop.app.ui.screens.AccountScreen
import com.afilishop.app.ui.screens.AdminScreen
import com.afilishop.app.ui.screens.AuthScreen
import com.afilishop.app.ui.screens.CommunityScreen
import com.afilishop.app.ui.screens.ConversationScreen
import com.afilishop.app.ui.screens.ExploreScreen
import com.afilishop.app.ui.screens.FavoritesScreen
import com.afilishop.app.ui.screens.HomeScreen
import com.afilishop.app.ui.screens.LiveScreen
import com.afilishop.app.ui.screens.NotificationsScreen
import com.afilishop.app.ui.screens.PointsScreen
import com.afilishop.app.ui.screens.ProductDetailScreen
import com.afilishop.app.ui.screens.ProfileScreen
import com.afilishop.app.ui.screens.ResetPasswordScreen
import com.afilishop.app.ui.screens.SettingsScreen
import com.afilishop.app.ui.screens.StoreScreen
import com.afilishop.app.ui.screens.TermsScreen
import com.afilishop.app.ui.screens.UploadScreen
import com.afilishop.app.ui.screens.VideosScreen
import com.afilishop.app.ui.screens.VipAssistantScreen
import com.afilishop.app.ui.screens.VipScreen

@Composable
fun AfiliShopApp(viewModel: AfiliShopViewModel, initialIntent: Intent? = null) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    val bottomRoutes = setOf("home", "videos", "upload", "vip", "account")
    val hideOverlays = currentRoute.startsWith("videos") ||
        currentRoute.startsWith("conversation/") ||
        currentRoute.startsWith("account/") ||
        currentRoute == "upload" ||
        currentRoute == "login" ||
        currentRoute == "reset-password"
    val rawDeepLink = initialIntent?.data?.toString().orEmpty()
    val isRecoveryLink = rawDeepLink.contains("type=recovery", ignoreCase = true) || initialIntent?.data?.host == "reset-password"
    val isOAuthCallback = initialIntent?.data?.scheme.equals("afilishop", ignoreCase = true) &&
        initialIntent?.data?.host.equals("auth-callback", ignoreCase = true)
    val recoveryParams: Map<String, String> = remember(rawDeepLink) {
        buildMap {
            rawDeepLink.substringAfter('#', "").split('&').forEach { part ->
                val pieces = part.split('=', limit = 2)
                if (pieces.size == 2) put(pieces[0], pieces[1])
            }
        }
    }
    val notificationLink = initialIntent?.getStringExtra(AfiliShopMessagingService.EXTRA_NOTIFICATION_LINK)

    LaunchedEffect(initialIntent) {
        val recoveryAccess = recoveryParams["access_token"]
        val recoveryRefresh = recoveryParams["refresh_token"]
        if (isOAuthCallback) {
            viewModel.completeGoogleSignIn(rawDeepLink) {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else if (isRecoveryLink && !recoveryAccess.isNullOrBlank()) {
            viewModel.restoreExternalSession(recoveryAccess, recoveryRefresh)
        }
        if (!isRecoveryLink && !isOAuthCallback) {
            val sharedText = initialIntent?.getStringExtra(Intent.EXTRA_TEXT)
            val deepLink = initialIntent?.data?.toString()
            val destination = notificationDestination(notificationLink)
                ?: deepLink?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                    ?.let(::notificationDestination)
            when {
                destination != null -> navController.navigate(destination) { launchSingleTop = true }
                !notificationLink.isNullOrBlank() &&
                    (notificationLink.startsWith("http://") || notificationLink.startsWith("https://")) -> {
                    val uri = Uri.parse(notificationLink)
                    if (uri.host.equals("afilishop.lovable.app", ignoreCase = true)) {
                        navController.navigate("explore") { launchSingleTop = true }
                    } else {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    }
                }
                !sharedText.isNullOrBlank() -> navController.navigate("explore") { launchSingleTop = true }
            }
        }
    }

    DisposableEffect(context, viewModel) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                viewModel.refreshNotifications()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(AfiliShopMessagingService.ACTION_NOTIFICATION_RECEIVED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    NativePushRegistrar(userId = state.user?.id, viewModel = viewModel)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFFFFDF9),
        bottomBar = {
            if (currentRoute in bottomRoutes) {
                AfiliBottomBar(
                    selectedRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = if (isRecoveryLink) "reset-password" else "home",
                modifier = Modifier.fillMaxSize(),
            ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onProduct = { navController.navigate("product/$it") },
                    onSearch = { navController.navigate("explore") },
                    onNotifications = { navController.navigate("notifications") },
                    onVideos = { navController.navigate("videos") },
                    onSignUp = { navController.navigate("login") },
                )
            }
            composable("explore") {
                ExploreScreen(viewModel, padding, onProduct = { navController.navigate("product/$it") })
            }
            composable("videos") {
                VideosScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onProfile = { userId -> navController.navigate("profile/$userId") },
                    onProduct = { productId -> navController.navigate("product/$productId") },
                )
            }
            composable("vip") {
                VipScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onLogin = { navController.navigate("login") },
                    onAssistant = { navController.navigate("vip-assistant") },
                )
            }
            composable("vip-assistant") {
                VipAssistantScreen(
                    padding = padding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("account") {
                AccountScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onLogin = { navController.navigate("login") },
                    onProfile = { navController.navigate("account/profile") },
                    onNotifications = { navController.navigate("notifications") },
                    onCommunity = { navController.navigate("community") },
                    onLives = { navController.navigate("lives") },
                    onAdmin = { navController.navigate("admin") },
                    onFavorites = { navController.navigate("favorites") },
                    onPoints = { navController.navigate("points") },
                    onSettings = { navController.navigate("account/settings") },
                    onTerms = { navController.navigate("terms") },
                )
            }
            composable("terms") {
                TermsScreen(padding = padding, onBack = { navController.popBackStack() })
            }
            composable("account/settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onBack = { navController.popBackStack() },
                    onProfile = { navController.navigate("account/profile") },
                    onLives = { navController.navigate("lives") },
                    onSignedOut = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onBack = { navController.popBackStack() },
                    onNavigate = { destination ->
                        navController.navigate(destination) { launchSingleTop = true }
                    },
                )
            }
            composable("favorites") {
                FavoritesScreen(
                    viewModel,
                    padding,
                    onBack = { navController.popBackStack() },
                    onProduct = { navController.navigate("product/$it") },
                )
            }
            composable("points") {
                PointsScreen(viewModel, padding, onBack = { navController.popBackStack() })
            }
            composable("community") {
                CommunityScreen(
                    viewModel,
                    padding,
                    onBack = { navController.popBackStack() },
                    onConversation = { navController.navigate("conversation/$it") },
                )
            }
            composable(
                "conversation/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                ConversationScreen(
                    viewModel = viewModel,
                    conversationId = entry.arguments?.getString("id").orEmpty(),
                    padding = padding,
                    onBack = { navController.popBackStack() },
                    onProfile = { userId -> navController.navigate("profile/$userId") },
                    onProduct = { productId -> navController.navigate("product/$productId") },
                )
            }
            composable("lives") {
                LiveScreen(viewModel, padding, onBack = { navController.popBackStack() })
            }
            composable("admin") {
                AdminScreen(viewModel, padding, onBack = { navController.popBackStack() })
            }
            composable(
                "store/{slug}",
                arguments = listOf(navArgument("slug") { type = NavType.StringType }),
            ) { entry ->
                StoreScreen(
                    viewModel,
                    entry.arguments?.getString("slug").orEmpty(),
                    padding,
                    onBack = { navController.popBackStack() },
                    onProduct = { navController.navigate("product/$it") },
                )
            }
            composable("account/profile") {
                ProfileScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onUpload = { navController.navigate("upload") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                "profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { entry ->
                ProfileScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onUpload = {},
                    onBack = { navController.popBackStack() },
                    publicUserId = entry.arguments?.getString("userId"),
                )
            }
            composable("upload") {
                UploadScreen(viewModel, padding, onBack = { navController.popBackStack() })
            }
            composable("login") {
                AuthScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onDone = { navController.popBackStack() },
                    onTerms = { navController.navigate("terms") },
                )
            }
            composable("reset-password") {
                ResetPasswordScreen(viewModel, padding, onDone = { navController.navigate("account") })
            }
            composable(
                route = "product/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                ProductDetailScreen(viewModel, id, padding, onBack = { navController.popBackStack() })
            }
            }

            if (!hideOverlays && currentRoute.isNotBlank()) {
                AfiliFloatingActions(
                    bottomPadding = if (currentRoute in bottomRoutes) 88.dp else 20.dp,
                    showAdmin = state.isAdmin,
                    onAdmin = { navController.navigate("admin") },
                    onPostVideo = { navController.navigate("upload") },
                    onLive = { navController.navigate("lives") },
                    onCommunity = { navController.navigate("community") },
                    onSupport = { navController.navigate(if (state.user == null) "login" else "account") },
                )
            }
        }
    }
}
