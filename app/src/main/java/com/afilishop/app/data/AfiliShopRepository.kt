package com.afilishop.app.data

import android.content.Context
import com.afilishop.app.BuildConfig
import com.afilishop.app.model.AuthSession
import com.afilishop.app.model.ChatMessage
import com.afilishop.app.model.ChatMessageInsert
import com.afilishop.app.model.Conversation
import com.afilishop.app.model.LiveActionRequest
import com.afilishop.app.model.LiveActionResponse
import com.afilishop.app.model.LiveGift
import com.afilishop.app.model.LiveRoom
import com.afilishop.app.model.NotificationItem
import com.afilishop.app.model.NotificationPreferences
import com.afilishop.app.model.NotificationPreferencesPayload
import com.afilishop.app.model.NotificationReadUpdate
import com.afilishop.app.model.NotificationUserState
import com.afilishop.app.model.NotificationUserStatePayload
import com.afilishop.app.model.AdminActionRequest
import com.afilishop.app.model.AdminResponse
import com.afilishop.app.model.AuthUserUpdateRequest
import com.afilishop.app.model.FavoriteRow
import com.afilishop.app.model.FollowPayload
import com.afilishop.app.model.FollowRow
import com.afilishop.app.model.FavoriteTogglePayload
import com.afilishop.app.model.PointBalance
import com.afilishop.app.model.PointTransaction
import com.afilishop.app.model.PricePoint
import com.afilishop.app.model.ProductComment
import com.afilishop.app.model.ProductCommentInsert
import com.afilishop.app.model.ProductOffer
import com.afilishop.app.model.RankingEntry
import com.afilishop.app.model.SocialVideoInsert
import com.afilishop.app.model.Store
import com.afilishop.app.model.SubscriptionPlan
import com.afilishop.app.model.PlanFeature
import com.afilishop.app.model.VideoComment
import com.afilishop.app.model.VideoCommentInsert
import com.afilishop.app.model.VideoLikeRow
import com.afilishop.app.model.PlayBillingActivationRequest
import com.afilishop.app.model.ProfileUpdatePayload
import com.afilishop.app.model.RefreshTokenRequest
import com.afilishop.app.model.PushTokenPayload
import com.afilishop.app.model.UserRole
import com.afilishop.app.model.UserSubscription
import com.afilishop.app.model.AuthUser
import com.afilishop.app.model.Banner
import com.afilishop.app.model.Category
import com.afilishop.app.model.HomePayload
import com.afilishop.app.model.Product
import com.afilishop.app.model.Profile
import com.afilishop.app.model.SignInRequest
import com.afilishop.app.model.SignUpRequest
import com.afilishop.app.model.SocialVideo
import com.afilishop.app.notifications.PushTokenStore
import com.google.firebase.messaging.FirebaseMessaging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.tasks.await
import java.time.Instant

class AfiliShopRepository(context: Context) {
    private val appContext = context.applicationContext
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val configured = supabaseUrl.isNotBlank() && anonKey.isNotBlank()
    private val sessionStore = SessionStore(appContext)
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false })
        }
        expectSuccess = false
    }

    var session: AuthSession? = null
        private set

    suspend fun restoreExternalSession(accessToken: String, refreshToken: String? = null): AuthSession? {
        if (!configured || accessToken.isBlank()) return null
        return runCatching {
            val response = client.get("$supabaseUrl/auth/v1/user") {
                header("apikey", anonKey)
                bearerAuth(accessToken)
            }
            if (response.status.value !in 200..299) error("Link de recuperação expirado.")
            val user = response.body<AuthUser>()
            AuthSession(accessToken, refreshToken, user = user).also { session = it; sessionStore.write(it) }
        }.getOrNull()
    }

    suspend fun restoreSession(): AuthSession? {
        val stored = sessionStore.read() ?: return null
        session = stored
        return stored
    }

    suspend fun refreshSession(): Result<AuthSession> = runCatching {
        val refreshToken = session?.refreshToken ?: error("Sessão expirada. Entre novamente.")
        val response = client.post("$supabaseUrl/auth/v1/token?grant_type=refresh_token") {
            header("apikey", anonKey)
            header("Content-Type", "application/json")
            setBody(RefreshTokenRequest(refreshToken))
        }
        if (response.status.value !in 200..299) error("Não foi possível renovar a sessão.")
        response.body<AuthSession>().also { session = it; sessionStore.write(it) }
    }

    suspend fun loadHome(): HomePayload {
        if (!configured) return HomePayload()
        val products = runCatching {
            getTable<Product>("products", mapOf(
                "select" to "id,title,price,currency,image_url,old_price,discount_percentage,free_shipping,affiliate_url,original_affiliate_url,priority_tier,category_id",
                "order" to "priority_tier.desc,created_at.desc",
                "limit" to "1000"
            ))
        }.getOrElse { emptyList() }
        val categories = runCatching {
            getTable<Category>("categories", mapOf("select" to "id,name,slug,icon", "order" to "name.asc"))
        }.getOrElse { emptyList() }
        val banners = runCatching {
            getTable<Banner>("banners", mapOf("select" to "id,type,media_url,title,subtitle,link_url", "is_active" to "eq.true", "order" to "sort_order.asc"))
        }.getOrElse { emptyList() }
        return HomePayload(products, categories, banners)
    }

    suspend fun loadProductsByCategory(categoryId: String): List<Product> {
        if (categoryId.isBlank()) return loadHome().products
        if (!configured) return DemoData.home.products.filter { it.categoryId == categoryId }
        return runCatching {
            getTable<Product>("products", mapOf(
                "select" to "id,title,price,currency,image_url,old_price,discount_percentage,free_shipping,affiliate_url,original_affiliate_url,priority_tier,category_id",
                "category_id" to "eq.$categoryId",
                "order" to "priority_tier.desc,created_at.desc",
                "limit" to "1000"
            ))
        }.getOrElse { emptyList() }
    }

    suspend fun searchProducts(query: String): List<Product> {
        if (query.isBlank()) return loadHome().products
        if (!configured) return DemoData.home.products.filter { it.title.contains(query, ignoreCase = true) }
        return runCatching {
            getTable<Product>("products", mapOf(
                "select" to "id,title,price,currency,image_url,old_price,discount_percentage,free_shipping,affiliate_url,original_affiliate_url",
                "title" to "ilike.*${query.trim()}*",
                "order" to "created_at.desc",
                "limit" to "60"
            ))
        }.getOrElse { emptyList() }
    }

    suspend fun getProduct(id: String): Product? {
        if (!configured) return DemoData.home.products.firstOrNull { it.id == id }
        return runCatching {
            getTable<Product>("products", mapOf(
                "select" to "id,title,description,price,currency,image_url,images,affiliate_url,original_affiliate_url,old_price,discount_percentage,free_shipping,category_id",
                "id" to "eq.$id",
                "limit" to "1"
            )).firstOrNull()
        }.getOrNull()
    }

    suspend fun loadVideos(): List<SocialVideo> {
        if (!configured) return emptyList()
        return runCatching {
            getTable<SocialVideo>("social_videos", mapOf(
                "select" to "id,video_url,thumbnail_url,description,product_id,user_id,likes_count,comments_count,created_at",
                "is_published" to "eq.true",
                "order" to "created_at.desc",
                "limit" to "40"
            ))
        }.getOrElse { emptyList() }
    }

    suspend fun signIn(email: String, password: String): Result<AuthSession> = runCatching {
        require(configured) { "Configure SUPABASE_URL e SUPABASE_ANON_KEY para autenticar." }
        val response = client.post("$supabaseUrl/auth/v1/token?grant_type=password") {
            header("apikey", anonKey)
            header("Content-Type", "application/json")
            setBody(SignInRequest(email, password))
        }
        if (response.status != HttpStatusCode.OK) error("Não foi possível entrar (${response.status.value}).")
        response.body<AuthSession>().also { session = it; sessionStore.write(it) }
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<AuthSession?> = runCatching {
        require(configured) { "Configure SUPABASE_URL e SUPABASE_ANON_KEY para cadastrar." }
        val response = client.post("$supabaseUrl/auth/v1/signup") {
            header("apikey", anonKey)
            header("Content-Type", "application/json")
            setBody(SignUpRequest(email, password, mapOf("display_name" to displayName)))
        }
        if (response.status.value !in 200..299) error("Não foi possível criar a conta (${response.status.value}).")
        if (response.bodyAsText().isBlank()) null else response.body<AuthSession>().also { session = it; sessionStore.write(it) }
    }

    suspend fun isFollowing(followerId: String, followingId: String): Boolean {
        if (!configured || followerId.isBlank() || followingId.isBlank() || followerId == followingId) return false
        return runCatching { getTable<FollowRow>("user_follows", mapOf("select" to "follower_id,following_id", "follower_id" to "eq.$followerId", "following_id" to "eq.$followingId", "limit" to "1")).isNotEmpty() }.getOrDefault(false)
    }

    suspend fun toggleFollow(followerId: String, followingId: String): Result<Boolean> = runCatching {
        require(followerId != followingId) { "Você não pode seguir a própria conta." }
        val exists = isFollowing(followerId, followingId)
        if (exists) {
            if (!deleteTable("user_follows", mapOf("follower_id" to "eq.$followerId", "following_id" to "eq.$followingId"))) error("Não foi possível deixar de seguir.")
        } else if (!postTable("user_follows", FollowPayload(followerId, followingId))) error("Não foi possível seguir o perfil.")
        !exists
    }

    suspend fun loadFavoriteIds(userId: String): Set<String> {
        if (!configured || userId.isBlank()) return emptySet()
        return runCatching { getTable<FavoriteRow>("favorites", mapOf("select" to "product_id", "user_id" to "eq.$userId", "limit" to "500")).map { it.productId }.toSet() }.getOrDefault(emptySet())
    }

    suspend fun toggleFavorite(userId: String, productId: String): Result<Boolean> = runCatching {
        val exists = getTable<FavoriteRow>("favorites", mapOf("select" to "product_id", "user_id" to "eq.$userId", "product_id" to "eq.$productId", "limit" to "1")).isNotEmpty()
        if (exists) {
            if (!deleteTable("favorites", mapOf("user_id" to "eq.$userId", "product_id" to "eq.$productId"))) error("Não foi possível remover o favorito.")
        } else if (!postTable("favorites", FavoriteTogglePayload(userId, productId))) error("Não foi possível salvar o favorito.")
        !exists
    }

    suspend fun loadPoints(userId: String): PointBalance? {
        if (!configured || userId.isBlank()) return null
        return runCatching { getTable<PointBalance>("user_points", mapOf("select" to "balance,updated_at", "user_id" to "eq.$userId", "limit" to "1")).firstOrNull() }.getOrNull()
    }

    suspend fun loadPointTransactions(userId: String): List<PointTransaction> {
        if (!configured || userId.isBlank()) return emptyList()
        return runCatching { getTable<PointTransaction>("point_transactions", mapOf("select" to "id,delta,reason,ref_id,created_at", "user_id" to "eq.$userId", "order" to "created_at.desc", "limit" to "100")) }.getOrElse { emptyList() }
    }

    suspend fun loadStore(slug: String): Store? {
        if (!configured || slug.isBlank()) return null
        return runCatching { getTable<Store>("stores", mapOf("select" to "id,name,slug,description,logo_url,banner_url,owner_id,is_verified,rating_avg,rating_count", "slug" to "eq.$slug", "is_active" to "eq.true", "limit" to "1")).firstOrNull() }.getOrNull()
    }

    suspend fun loadStoreProducts(storeId: String): List<Product> {
        if (!configured || storeId.isBlank()) return emptyList()
        return runCatching { getTable<Product>("products", mapOf("select" to "id,title,price,currency,image_url,old_price,discount_percentage,free_shipping,affiliate_url,original_affiliate_url,store_id", "store_id" to "eq.$storeId", "order" to "created_at.desc", "limit" to "100")) }.getOrElse { emptyList() }
    }

    suspend fun loadProductOffers(productId: String): List<ProductOffer> {
        if (!configured || productId.isBlank()) return emptyList()
        return runCatching { getTable<ProductOffer>("product_offers", mapOf("select" to "id,store,url,price,currency", "product_id" to "eq.$productId", "order" to "price.asc")) }.getOrElse { emptyList() }
    }

    suspend fun loadPriceHistory(productId: String): List<PricePoint> {
        if (!configured || productId.isBlank()) return emptyList()
        return runCatching { getTable<PricePoint>("price_history", mapOf("select" to "price,recorded_at", "product_id" to "eq.$productId", "order" to "recorded_at.desc", "limit" to "30")) }.getOrElse { emptyList() }
    }

    suspend fun loadPlans(): List<SubscriptionPlan> {
        if (!configured) return emptyList()
        return runCatching { getTable<SubscriptionPlan>("subscription_plans", mapOf("select" to "id,code,name,price_brl,slots,tier,duration_days,is_active,benefits", "is_active" to "eq.true", "order" to "sort_order.asc")) }.getOrElse { emptyList() }
    }

    suspend fun loadPlanFeatures(): List<PlanFeature> {
        if (!configured) return emptyList()
        return runCatching {
            getTable<PlanFeature>(
                "plan_features",
                mapOf(
                    "select" to "key,name,min_tier,limit_value,limit_period,sort_order,is_active",
                    "is_active" to "eq.true",
                    "order" to "sort_order.asc",
                ),
            )
        }.getOrElse { emptyList() }
    }

    suspend fun listProductComments(productId: String): List<ProductComment> {
        if (!configured || productId.isBlank()) return emptyList()
        return runCatching { getTable<ProductComment>("product_comments", mapOf("select" to "id,product_id,user_id,content,parent_id,likes_count,is_promoted,created_at", "product_id" to "eq.$productId", "order" to "created_at.desc", "limit" to "100")) }.getOrElse { emptyList() }
    }

    suspend fun addProductComment(userId: String, productId: String, content: String, parentId: String? = null): Boolean {
        if (content.isBlank()) return false
        return postTable("product_comments", ProductCommentInsert(productId, userId, content, parentId))
    }

    suspend fun listVideoComments(videoId: String): List<VideoComment> {
        if (!configured || videoId.isBlank()) return emptyList()
        return runCatching { getTable<VideoComment>("video_comments", mapOf("select" to "id,video_id,user_id,content,parent_id,created_at", "video_id" to "eq.$videoId", "order" to "created_at.asc", "limit" to "100")) }.getOrElse { emptyList() }
    }

    suspend fun addVideoComment(userId: String, videoId: String, content: String, parentId: String? = null): Boolean {
        if (content.isBlank()) return false
        return postTable("video_comments", VideoCommentInsert(videoId, userId, content, parentId))
    }

    suspend fun toggleVideoLike(userId: String, videoId: String): Result<Boolean> = runCatching {
        val exists = getTable<VideoLikeRow>("video_likes", mapOf("select" to "video_id", "user_id" to "eq.$userId", "video_id" to "eq.$videoId", "limit" to "1")).isNotEmpty()
        if (exists) deleteTable("video_likes", mapOf("user_id" to "eq.$userId", "video_id" to "eq.$videoId")) else postTable("video_likes", mapOf("user_id" to userId, "video_id" to videoId))
        !exists
    }

    suspend fun publishVideo(request: SocialVideoInsert): Result<Boolean> = runCatching { postTable("social_videos", request) }

    suspend fun recordProductClick(userId: String?, productId: String, source: String = "android"): Boolean {
        if (!configured) return false
        val payload = mapOf("user_id" to userId, "product_id" to productId, "source" to source)
        return postTable("product_clicks", payload)
    }

    suspend fun requestPasswordReset(email: String): Boolean {
        if (!configured || email.isBlank()) return false
        val response = client.post("$supabaseUrl/auth/v1/recover") {
            header("apikey", anonKey)
            header("Content-Type", "application/json")
            setBody(mapOf("email" to email))
        }
        return response.status.value in 200..299
    }

    suspend fun updatePassword(password: String): Boolean {
        val token = session?.accessToken ?: return false
        val response = client.post("$supabaseUrl/auth/v1/user") {
            header("apikey", anonKey)
            bearerAuth(token)
            header("Content-Type", "application/json")
            setBody(AuthUserUpdateRequest(password = password))
        }
        return response.status.value in 200..299
    }

    suspend fun updateProfile(userId: String, profile: ProfileUpdatePayload): Boolean {
        if (!configured || userId.isBlank()) return false
        return patchTable("profiles", profile, mapOf("id" to "eq.$userId"))
    }

    suspend fun loadProfile(userId: String): Profile? {
        if (!configured || userId.isBlank()) return null
        return runCatching { getTable<Profile>("profiles", mapOf("select" to "id,display_name,avatar_url,bio,followers_count,following_count", "id" to "eq.$userId", "limit" to "1")).firstOrNull() }.getOrNull()
    }

    suspend fun loadAdminData(): Result<AdminResponse> = runCatching {
        val token = session?.accessToken ?: error("Sessão administrativa ausente.")
        val response = client.get("${BuildConfig.API_BASE_URL.trimEnd('/')}/api/mobile/admin") { bearerAuth(token) }
        val body = response.body<AdminResponse>()
        if (response.status.value !in 200..299 || !body.ok) error(body.error ?: "Acesso administrativo negado.")
        body
    }

    suspend fun adminAction(request: AdminActionRequest): Result<Boolean> = runCatching {
        val token = session?.accessToken ?: error("Sessão administrativa ausente.")
        val response = client.post("${BuildConfig.API_BASE_URL.trimEnd('/')}/api/mobile/admin") { bearerAuth(token); header("Content-Type", "application/json"); setBody(request) }
        val body = response.body<AdminResponse>()
        if (response.status.value !in 200..299 || !body.ok) error(body.error ?: "Ação administrativa rejeitada.")
        true
    }

    suspend fun isAdmin(userId: String): Boolean {
        if (!configured || userId.isBlank()) return false
        return runCatching { getTable<UserRole>("user_roles", mapOf("select" to "user_id,role", "user_id" to "eq.$userId", "role" to "eq.admin", "limit" to "1")).isNotEmpty() }.getOrDefault(false)
    }

    suspend fun loadNotifications(userId: String): List<NotificationItem> {
        if (!configured || userId.isBlank()) return emptyList()
        return runCatching {
            val preferences = loadNotificationPreferences(userId) ?: NotificationPreferences()
            val notifications = getTable<NotificationItem>(
                "notifications",
                mapOf(
                    "select" to "id,user_id,title,body,type,link,product_id,read_at,created_at,is_system_protected,actor_id,count",
                    "or" to "(user_id.eq.$userId,user_id.is.null)",
                    "order" to "created_at.desc",
                    "limit" to "80",
                ),
            )
            val broadcastIds = notifications.filter { it.isBroadcast }.map { it.id }
            val statesByNotification = if (broadcastIds.isEmpty()) {
                emptyMap()
            } else {
                getTable<NotificationUserState>(
                    "notification_user_states",
                    mapOf(
                        "select" to "notification_id,read_at,dismissed_at",
                        "user_id" to "eq.$userId",
                        "notification_id" to "in.(${broadcastIds.joinToString(",")})",
                    ),
                ).associateBy { it.notificationId }
            }

            notifications
                .asSequence()
                .filter { it.allowedBy(preferences) }
                .filter { notification ->
                    !notification.isBroadcast || statesByNotification[notification.id]?.dismissedAt == null
                }
                .map { notification ->
                    if (!notification.isBroadcast) notification
                    else notification.copy(readAt = statesByNotification[notification.id]?.readAt)
                }
                .toList()
        }.getOrElse { emptyList() }
    }

    suspend fun loadNotificationPreferences(userId: String): NotificationPreferences? {
        if (!configured || userId.isBlank()) return null
        return runCatching { getTable<NotificationPreferences>("notification_preferences", mapOf("select" to "allow_promo,allow_price_alerts", "user_id" to "eq.$userId", "limit" to "1")).firstOrNull() }.getOrNull()
    }

    suspend fun registerPushToken(userId: String, token: String): Boolean {
        if (!configured || userId.isBlank() || token.isBlank()) return false
        val saved = postTable(
            "user_push_tokens",
            PushTokenPayload(userId, token, updatedAt = Instant.now().toString()),
            onConflict = "token",
        )
        if (saved) PushTokenStore.markSynced(appContext, token)
        return saved
    }

    suspend fun updateNotificationPreferences(userId: String, allowPromo: Boolean, allowPriceAlerts: Boolean): Boolean {
        if (!configured || userId.isBlank()) return false
        return upsertTable("notification_preferences", NotificationPreferencesPayload(userId, allowPromo, allowPriceAlerts), onConflict = "user_id")
    }

    suspend fun markNotificationRead(userId: String, notification: NotificationItem): Boolean {
        if (!configured || userId.isBlank() || notification.id.isBlank()) return false
        val now = Instant.now().toString()
        return if (notification.isBroadcast) {
            postTable(
                "notification_user_states",
                NotificationUserStatePayload(notification.id, userId, readAt = now),
                onConflict = "notification_id,user_id",
            )
        } else {
            patchTable(
                "notifications",
                NotificationReadUpdate(now),
                mapOf("id" to "eq.${notification.id}", "user_id" to "eq.$userId"),
            )
        }
    }

    suspend fun markAllNotificationsRead(userId: String, notifications: List<NotificationItem>): Boolean {
        if (!configured || userId.isBlank()) return false
        val now = Instant.now().toString()
        val personalIds = notifications.filter { !it.isBroadcast && !it.isRead }.map { it.id }
        val broadcastIds = notifications.filter { it.isBroadcast && !it.isRead }.map { it.id }
        val personalOk = personalIds.isEmpty() || patchTable(
            "notifications",
            NotificationReadUpdate(now),
            mapOf("id" to "in.(${personalIds.joinToString(",")})", "user_id" to "eq.$userId"),
        )
        val broadcastOk = broadcastIds.isEmpty() || postTable(
            "notification_user_states",
            broadcastIds.map { NotificationUserStatePayload(it, userId, readAt = now) },
            onConflict = "notification_id,user_id",
        )
        return personalOk && broadcastOk
    }

    suspend fun deleteNotification(userId: String, notification: NotificationItem): Boolean {
        if (!configured || userId.isBlank() || notification.id.isBlank() || notification.isSystemProtected) return false
        return if (notification.isBroadcast) {
            val now = Instant.now().toString()
            postTable(
                "notification_user_states",
                NotificationUserStatePayload(
                    notificationId = notification.id,
                    userId = userId,
                    readAt = notification.readAt ?: now,
                    dismissedAt = now,
                ),
                onConflict = "notification_id,user_id",
            )
        } else {
            deleteTable(
                "notifications",
                mapOf(
                    "id" to "eq.${notification.id}",
                    "user_id" to "eq.$userId",
                    "is_system_protected" to "eq.false",
                ),
            )
        }
    }

    suspend fun deletePushToken(userId: String, token: String): Boolean {
        if (!configured || userId.isBlank() || token.isBlank()) return false
        return deleteTable("user_push_tokens", mapOf("user_id" to "eq.$userId", "token" to "eq.$token"))
    }

    suspend fun uploadObject(bucket: String, path: String, bytes: ByteArray, contentType: String): String? {
        if (!configured || bucket.isBlank() || path.isBlank() || bytes.isEmpty()) return null
        val response = client.post("$supabaseUrl/storage/v1/object/$bucket/$path") {
            header("apikey", anonKey)
            session?.accessToken?.let { bearerAuth(it) }
            header("Content-Type", contentType)
            header("x-upsert", "false")
            setBody(bytes)
        }
        return if (response.status.value in 200..299) "$supabaseUrl/storage/v1/object/public/$bucket/$path" else null
    }

    suspend fun listConversations(userId: String): List<Conversation> {
        if (!configured || userId.isBlank()) return emptyList()
        return runCatching {
            val a = getTable<Conversation>("conversations", mapOf("select" to "id,user_a,user_b,status,last_message_preview,last_message_at", "user_a" to "eq.$userId", "order" to "last_message_at.desc", "limit" to "50"))
            val b = getTable<Conversation>("conversations", mapOf("select" to "id,user_a,user_b,status,last_message_preview,last_message_at", "user_b" to "eq.$userId", "order" to "last_message_at.desc", "limit" to "50"))
            (a + b).distinctBy { it.id }.sortedByDescending { it.lastMessageAt.orEmpty() }
        }.getOrElse { emptyList() }
    }

    suspend fun listMessages(conversationId: String): List<ChatMessage> {
        if (!configured || conversationId.isBlank()) return emptyList()
        return runCatching {
            getTable<ChatMessage>("messages", mapOf("select" to "id,conversation_id,sender_id,content,media_url,created_at,is_flagged", "conversation_id" to "eq.$conversationId", "order" to "created_at.asc", "limit" to "200"))
        }.getOrElse { emptyList() }
    }

    suspend fun sendMessage(conversationId: String, senderId: String, content: String): Boolean {
        if (!configured || conversationId.isBlank() || senderId.isBlank() || content.isBlank()) return false
        return postTable("messages", ChatMessageInsert(conversationId, senderId, content))
    }

    suspend fun listLiveRooms(): List<LiveRoom> {
        if (!configured) return emptyList()
        return runCatching { getTable<LiveRoom>("lives", mapOf("select" to "id,title,host_id,is_live,started_at,ended_at,channel,cover_url,viewers_count,peak_viewers", "is_live" to "eq.true", "order" to "started_at.desc", "limit" to "40")) }.getOrElse { emptyList() }
    }

    suspend fun listLiveGifts(): List<LiveGift> {
        if (!configured) return emptyList()
        return runCatching { getTable<LiveGift>("live_gifts", mapOf("select" to "id,name,emoji,image_url,cost_points,is_active", "is_active" to "eq.true", "order" to "sort_order.asc", "limit" to "60")) }.getOrElse { emptyList() }
    }

    private suspend fun liveAction(request: LiveActionRequest): Result<LiveActionResponse> = runCatching {
        require(session?.accessToken != null) { "Entre para usar as lives." }
        val response = client.post("${BuildConfig.API_BASE_URL.trimEnd('/')}/api/mobile/lives") {
            header("Content-Type", "application/json")
            session?.accessToken?.let { bearerAuth(it) }
            setBody(request)
        }
        val body = response.body<LiveActionResponse>()
        if (response.status.value !in 200..299 || !body.ok) error(body.error ?: "Não foi possível executar a ação da live.")
        body
    }

    suspend fun startLive(title: String, coverUrl: String? = null): Result<LiveActionResponse> = liveAction(LiveActionRequest("start", title = title, coverUrl = coverUrl))
    suspend fun joinLive(liveId: String): Result<LiveActionResponse> = liveAction(LiveActionRequest("join", liveId = liveId))
    suspend fun leaveLive(liveId: String): Result<Boolean> = liveAction(LiveActionRequest("leave", liveId = liveId)).map { true }
    suspend fun endLive(liveId: String): Result<Boolean> = liveAction(LiveActionRequest("end", liveId = liveId)).map { true }
    suspend fun sendLiveGift(liveId: String, giftId: String, quantity: Int): Result<Boolean> = liveAction(LiveActionRequest("send-gift", liveId = liveId, giftId = giftId, quantity = quantity)).map { true }

    suspend fun activatePlayBilling(sku: String, purchaseToken: String): Result<String> = runCatching {
        val token = session?.accessToken ?: error("Faça login para ativar o plano VIP.")
        val response = client.post("${BuildConfig.API_BASE_URL.trimEnd('/')}/api/mobile/billing/activate") {
            header("Authorization", "Bearer $token")
            header("Content-Type", "application/json")
            setBody(Json.encodeToString(PlayBillingActivationRequest(sku, purchaseToken)))
        }
        if (response.status.value !in 200..299) error(response.bodyAsText().ifBlank { "Falha ao ativar o plano VIP." })
        response.bodyAsText()
    }

    suspend fun loadSubscription(userId: String): UserSubscription? {
        if (!configured || userId.isBlank()) return null
        return runCatching { getTable<UserSubscription>("user_subscriptions", mapOf("select" to "plan_id,tier,status,expires_at,slots_total,slots_used", "user_id" to "eq.$userId", "limit" to "1")).firstOrNull() }.getOrNull()
    }

    private suspend fun deleteTable(table: String, params: Map<String, String>): Boolean {
        val response = client.delete("$supabaseUrl/rest/v1/$table") {
            header("apikey", anonKey)
            session?.accessToken?.let { bearerAuth(it) }
            header("Prefer", "return=minimal")
            params.forEach { (key, value) -> url.parameters.append(key, value) }
        }
        return response.status.value in 200..299
    }

    private suspend inline fun <reified T> patchTable(table: String, payload: T, params: Map<String, String>): Boolean {
        val response = client.patch("$supabaseUrl/rest/v1/$table") {
            header("apikey", anonKey)
            session?.accessToken?.let { bearerAuth(it) }
            header("Content-Type", "application/json")
            header("Prefer", "return=minimal")
            params.forEach { (key, value) -> url.parameters.append(key, value) }
            setBody(Json.encodeToString(payload))
        }
        return response.status.value in 200..299
    }

    private suspend inline fun <reified T> postTable(table: String, payload: T, onConflict: String? = null): Boolean {
        val response = client.post("$supabaseUrl/rest/v1/$table") {
            header("apikey", anonKey)
            session?.accessToken?.let { bearerAuth(it) }
            header("Content-Type", "application/json")
            header("Prefer", if (onConflict == null) "return=minimal" else "resolution=merge-duplicates,return=minimal")
            onConflict?.let { url.parameters.append("on_conflict", it) }
            setBody(Json.encodeToString(payload))
        }
        return response.status.value in 200..299
    }

    private suspend inline fun <reified T> upsertTable(table: String, payload: T, onConflict: String): Boolean = postTable(table, payload, onConflict)

    suspend fun signOut() {
        val userId = session?.user?.id
        val tokens = listOfNotNull(PushTokenStore.current(appContext), PushTokenStore.pending(appContext)).distinct()
        if (!userId.isNullOrBlank()) {
            tokens.forEach { token -> runCatching { deletePushToken(userId, token) } }
        }
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
        PushTokenStore.clear(appContext)
        session = null
        sessionStore.clear()
    }

    fun close() {
        client.close()
    }

    private suspend inline fun <reified T> getTable(table: String, params: Map<String, String>): List<T> {
        val response = client.get("$supabaseUrl/rest/v1/$table") {
            header("apikey", anonKey)
            session?.accessToken?.let { bearerAuth(it) }
            params.forEach { (key, value) -> url.parameters.append(key, value) }
        }
        if (response.status.value !in 200..299) error("Supabase ${response.status.value}")
        return response.body()
    }
}

private val PROMO_NOTIFICATION_TYPES = setOf(
    "promo", "new_product", "abandoned_cart", "opportunity", "upgrade_suggestion",
)

private val PRICE_NOTIFICATION_TYPES = setOf(
    "price_drop", "price_up", "product_back", "price_reduction", "favorite_discount",
)

private fun NotificationItem.allowedBy(preferences: NotificationPreferences): Boolean = when {
    isSystemProtected || type == "admin" -> true
    type in PROMO_NOTIFICATION_TYPES -> preferences.allowPromo != false
    type in PRICE_NOTIFICATION_TYPES -> preferences.allowPriceAlerts != false
    else -> true
}

private object DemoData {
    private val imageA = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=900&q=80"
    private val imageB = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=900&q=80"
    private val imageC = "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"
    private val imageD = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=900&q=80"

    val home = HomePayload(
        products = listOf(
            Product("demo-watch", "Relógio inteligente com monitoramento", 189.90, imageUrl = imageA, oldPrice = 249.90, discountPercentage = 24.0, freeShipping = true, affiliateUrl = "https://mercadolivre.com.br/"),
            Product("demo-shoe", "Tênis casual urbano edição especial", 219.90, imageUrl = imageB, oldPrice = 299.90, discountPercentage = 27.0, freeShipping = true, affiliateUrl = "https://mercadolivre.com.br/"),
            Product("demo-coffee", "Cafeteira compacta para espresso", 329.00, imageUrl = imageC, freeShipping = true, affiliateUrl = "https://mercadolivre.com.br/"),
            Product("demo-headphone", "Fone Bluetooth com cancelamento de ruído", 159.90, imageUrl = imageD, oldPrice = 199.90, discountPercentage = 20.0, affiliateUrl = "https://mercadolivre.com.br/")
        ),
        categories = listOf(
            Category("1", "Eletrônicos", "eletronicos", "devices"),
            Category("2", "Casa", "casa", "home"),
            Category("3", "Moda", "moda", "checkroom"),
            Category("4", "Beleza", "beleza", "face"),
            Category("5", "Ofertas", "ofertas", "local_offer")
        )
    )

    val videos = listOf(
        SocialVideo("demo-video-1", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", caption = "Achados que valem a pena hoje", likesCount = 128, commentsCount = 14),
        SocialVideo("demo-video-2", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", caption = "Oferta selecionada pelos nossos criadores", likesCount = 92, commentsCount = 8)
    )
}
