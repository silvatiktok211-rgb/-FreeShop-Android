package com.afilishop.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afilishop.app.data.AfiliShopRepository
import com.afilishop.app.model.Profile
import com.afilishop.app.model.FavoriteTogglePayload
import com.afilishop.app.model.PointBalance
import com.afilishop.app.model.PointTransaction
import com.afilishop.app.model.PricePoint
import com.afilishop.app.model.ProductComment
import com.afilishop.app.model.ProductOffer
import com.afilishop.app.model.NotificationPreferences
import com.afilishop.app.model.ProfileUpdatePayload
import com.afilishop.app.model.SocialVideoInsert
import com.afilishop.app.model.Store
import com.afilishop.app.model.SubscriptionPlan
import com.afilishop.app.model.PlanFeature
import com.afilishop.app.model.VideoComment
import com.afilishop.app.model.AdminActionRequest
import com.afilishop.app.model.AdminResponse
import com.afilishop.app.model.AuthUser
import com.afilishop.app.model.ChatMessage
import com.afilishop.app.model.Conversation
import com.afilishop.app.model.LiveActionResponse
import com.afilishop.app.model.LiveGift
import com.afilishop.app.model.LiveRoom
import com.afilishop.app.model.NotificationItem
import com.afilishop.app.model.UserSubscription
import com.afilishop.app.model.HomePayload
import com.afilishop.app.model.Product
import com.afilishop.app.model.SocialVideo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class AfiliShopUiState(
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val home: HomePayload = HomePayload(),
    val videos: List<SocialVideo> = emptyList(),
    val selectedProduct: Product? = null,
    val query: String = "",
    val user: AuthUser? = null,
    val profile: Profile? = null,
    val publicProfileId: String? = null,
    val isFollowingPublicProfile: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val points: PointBalance? = null,
    val pointTransactions: List<PointTransaction> = emptyList(),
    val store: Store? = null,
    val storeProducts: List<Product> = emptyList(),
    val productOffers: List<ProductOffer> = emptyList(),
    val priceHistory: List<PricePoint> = emptyList(),
    val plans: List<SubscriptionPlan> = emptyList(),
    val planFeatures: List<PlanFeature> = emptyList(),
    val productComments: List<ProductComment> = emptyList(),
    val videoComments: List<VideoComment> = emptyList(),
    val error: String? = null,
    val authMessage: String? = null,
    val notifications: List<NotificationItem> = emptyList(),
    val notificationsLoading: Boolean = false,
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val conversations: List<Conversation> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val liveRooms: List<LiveRoom> = emptyList(),
    val liveGifts: List<LiveGift> = emptyList(),
    val activeLive: LiveActionResponse? = null,
    val liveMessage: String? = null,
    val subscription: UserSubscription? = null,
    val uploadUrl: String? = null,
    val isUploading: Boolean = false,
    val billingMessage: String? = null,
    val isAdmin: Boolean = false,
    val adminData: AdminResponse? = null,
    val accountMessage: String? = null,
    val publishMessage: String? = null
)

class AfiliShopViewModel(private val repository: AfiliShopRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AfiliShopUiState())
    val uiState: StateFlow<AfiliShopUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
        refresh()
    }

    fun restoreExternalSession(accessToken: String, refreshToken: String? = null) {
        viewModelScope.launch {
            val session = repository.restoreExternalSession(accessToken, refreshToken)
            if (session?.user != null) {
                _uiState.update { it.copy(user = session.user, authMessage = "Link de recuperação validado.") }
                loadAccountData()
            } else _uiState.update { it.copy(authMessage = "O link de recuperação expirou.") }
        }
    }

    fun restoreSession() {
        viewModelScope.launch {
            val session = repository.restoreSession()
            if (session?.user != null) {
                _uiState.update { it.copy(user = session.user) }
                loadAccountData()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val home = repository.loadHome()
            val videos = repository.loadVideos()
            _uiState.update { it.copy(isLoading = false, home = home, videos = videos) }
        }
    }

    fun filterCategory(categoryId: String) {
        viewModelScope.launch {
            val products = repository.loadProductsByCategory(categoryId)
            _uiState.update { it.copy(home = it.home.copy(products = products), query = "") }
        }
    }

    fun clearCategoryFilter() {
        refresh()
    }

    fun search(query: String) {
        _uiState.update { it.copy(query = query) }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val results = repository.searchProducts(query)
            _uiState.update { it.copy(isSearching = false, home = it.home.copy(products = results)) }
        }
    }

    fun selectProduct(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val product = repository.getProduct(id)
            _uiState.update { it.copy(isLoading = false, selectedProduct = product) }
        }
    }

    fun clearSelectedProduct() { _uiState.update { it.copy(selectedProduct = null) } }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authMessage = null) }
            repository.signIn(email, password).onSuccess { session ->
                _uiState.update { it.copy(isLoading = false, user = session.user, authMessage = "Login realizado") }
                loadAccountData()
                onSuccess()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, authMessage = error.message) } }
        }
    }

    fun signUp(email: String, password: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authMessage = null) }
            repository.signUp(email, password, name).onSuccess { session ->
                _uiState.update { it.copy(isLoading = false, user = session?.user, authMessage = "Conta criada. Verifique seu e-mail se solicitado.") }
                loadAccountData()
                onSuccess()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, authMessage = error.message) } }
        }
    }

    fun loadFavorites() {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch { _uiState.update { it.copy(favoriteIds = repository.loadFavoriteIds(userId)) } }
    }

    fun toggleFavorite(productId: String) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            repository.toggleFavorite(userId, productId).onSuccess { added ->
                _uiState.update { state -> state.copy(favoriteIds = if (added) state.favoriteIds + productId else state.favoriteIds - productId) }
            }
        }
    }

    fun loadPoints() {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch { _uiState.update { it.copy(points = repository.loadPoints(userId), pointTransactions = repository.loadPointTransactions(userId)) } }
    }

    fun loadStore(slug: String) {
        viewModelScope.launch {
            val store = repository.loadStore(slug)
            _uiState.update { it.copy(store = store, storeProducts = store?.let { repository.loadStoreProducts(it.id) } ?: emptyList()) }
        }
    }

    fun loadProductExtras(productId: String) {
        viewModelScope.launch { _uiState.update { it.copy(productOffers = repository.loadProductOffers(productId), priceHistory = repository.loadPriceHistory(productId), productComments = repository.listProductComments(productId)) } }
    }

    fun recordProductClick(productId: String) {
        val userId = _uiState.value.user?.id
        viewModelScope.launch { repository.recordProductClick(userId, productId) }
    }

    fun addProductComment(productId: String, content: String) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch { if (repository.addProductComment(userId, productId, content)) loadProductExtras(productId) }
    }

    fun toggleVideoLike(videoId: String) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch { repository.toggleVideoLike(userId, videoId) }
    }

    fun loadVideoComments(videoId: String) {
        viewModelScope.launch { _uiState.update { it.copy(videoComments = repository.listVideoComments(videoId)) } }
    }

    fun addVideoComment(videoId: String, content: String) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch { if (repository.addVideoComment(userId, videoId, content)) loadVideoComments(videoId) }
    }

    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            val ok = repository.requestPasswordReset(email)
            _uiState.update { it.copy(authMessage = if (ok) "Confira seu e-mail para redefinir a senha." else "Não foi possível enviar o e-mail de recuperação.") }
        }
    }

    fun updatePassword(password: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.updatePassword(password)
            _uiState.update { it.copy(accountMessage = if (ok) "Senha atualizada." else "Não foi possível atualizar a senha.") }
            if (ok) onDone()
        }
    }

    fun loadPublicProfile(userId: String) {
        val viewerId = _uiState.value.user?.id
        viewModelScope.launch {
            val profile = repository.loadProfile(userId)
            val following = if (viewerId != null) repository.isFollowing(viewerId, userId) else false
            _uiState.update { it.copy(profile = profile, publicProfileId = userId, isFollowingPublicProfile = following) }
        }
    }

    fun toggleFollow(userId: String) {
        val viewerId = _uiState.value.user?.id ?: return
        viewModelScope.launch { repository.toggleFollow(viewerId, userId).onSuccess { following -> _uiState.update { it.copy(isFollowingPublicProfile = following) } } }
    }

    fun updateProfile(displayName: String, bio: String, avatarUrl: String? = null) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            val ok = repository.updateProfile(userId, ProfileUpdatePayload(displayName, bio, avatarUrl))
            val profile = repository.loadProfile(userId)
            _uiState.update { it.copy(profile = profile, accountMessage = if (ok) "Perfil atualizado." else "Não foi possível atualizar o perfil.") }
        }
    }

    fun loadAdminData() {
        if (!_uiState.value.isAdmin) return
        viewModelScope.launch { repository.loadAdminData().onSuccess { response -> _uiState.update { it.copy(adminData = response) } } }
    }

    fun adminDeactivateVideo(videoId: String) {
        viewModelScope.launch { repository.adminAction(AdminActionRequest("deactivate_video", id = videoId)).onSuccess { loadAdminData() } }
    }

    fun loadAccountData() {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            val notifications = repository.loadNotifications(userId)
            val notificationPreferences = repository.loadNotificationPreferences(userId) ?: NotificationPreferences()
            val conversations = repository.listConversations(userId)
            val subscription = repository.loadSubscription(userId)
            val isAdmin = repository.isAdmin(userId)
            val profile = repository.loadProfile(userId)
            val favoriteIds = repository.loadFavoriteIds(userId)
            val points = repository.loadPoints(userId)
            val pointTransactions = repository.loadPointTransactions(userId)
            _uiState.update { it.copy(notifications = notifications, notificationsLoading = false, notificationPreferences = notificationPreferences, conversations = conversations, subscription = subscription, isAdmin = isAdmin, profile = profile, favoriteIds = favoriteIds, points = points, pointTransactions = pointTransactions) }
        }
    }

    fun refreshNotifications() {
        val userId = _uiState.value.user?.id
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(notifications = emptyList(), notificationsLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(notificationsLoading = true) }
            val notifications = repository.loadNotifications(userId)
            _uiState.update { it.copy(notifications = notifications, notificationsLoading = false) }
        }
    }

    fun updateNotificationPreferences(allowPromo: Boolean, allowPriceAlerts: Boolean) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            if (repository.updateNotificationPreferences(userId, allowPromo, allowPriceAlerts)) {
                _uiState.update { it.copy(notificationPreferences = NotificationPreferences(allowPromo, allowPriceAlerts)) }
                refreshNotifications()
            }
        }
    }

    fun registerPushToken(token: String) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch { repository.registerPushToken(userId, token) }
    }

    fun markNotificationRead(notification: NotificationItem) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            if (repository.markNotificationRead(userId, notification)) {
                val now = Instant.now().toString()
                _uiState.update { state ->
                    state.copy(notifications = state.notifications.map {
                        if (it.id == notification.id) it.copy(readAt = now) else it
                    })
                }
            }
        }
    }

    fun markAllNotificationsRead() {
        val userId = _uiState.value.user?.id ?: return
        val notifications = _uiState.value.notifications
        viewModelScope.launch {
            if (repository.markAllNotificationsRead(userId, notifications)) {
                val now = Instant.now().toString()
                _uiState.update { state ->
                    state.copy(notifications = state.notifications.map { it.copy(readAt = it.readAt ?: now) })
                }
            }
        }
    }

    fun deleteNotification(notification: NotificationItem) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            if (repository.deleteNotification(userId, notification)) {
                _uiState.update { state ->
                    state.copy(notifications = state.notifications.filterNot { it.id == notification.id })
                }
            }
        }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch { _uiState.update { it.copy(messages = repository.listMessages(conversationId)) } }
    }

    fun sendMessage(conversationId: String, content: String) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            if (repository.sendMessage(conversationId, userId, content)) loadConversation(conversationId)
        }
    }

    fun startLive(title: String) {
        viewModelScope.launch { repository.startLive(title).onSuccess { response -> _uiState.update { state -> state.copy(activeLive = response, liveMessage = "Live iniciada no canal ${response.channel}.") } }.onFailure { error -> _uiState.update { state -> state.copy(liveMessage = error.message) } } }
    }

    fun joinLive(liveId: String) {
        viewModelScope.launch { repository.joinLive(liveId).onSuccess { response -> _uiState.update { state -> state.copy(activeLive = response, liveMessage = "Entrada autorizada na live.") } }.onFailure { error -> _uiState.update { state -> state.copy(liveMessage = error.message) } } }
    }

    fun leaveLive(liveId: String) {
        viewModelScope.launch { repository.leaveLive(liveId).onSuccess { _uiState.update { state -> state.copy(activeLive = null, liveMessage = "Você saiu da live.") } } }
    }

    fun endLive(liveId: String) {
        viewModelScope.launch { repository.endLive(liveId).onSuccess { _uiState.update { state -> state.copy(activeLive = null, liveMessage = "Live encerrada.") }; loadLives() }.onFailure { error -> _uiState.update { state -> state.copy(liveMessage = error.message) } } }
    }

    fun sendLiveGift(liveId: String, giftId: String) {
        viewModelScope.launch { repository.sendLiveGift(liveId, giftId, 1).onSuccess { _uiState.update { state -> state.copy(liveMessage = "Presente enviado.") } }.onFailure { error -> _uiState.update { state -> state.copy(liveMessage = error.message) } } }
    }

    fun loadLives() {
        viewModelScope.launch {
            val rooms = repository.listLiveRooms()
            val gifts = repository.listLiveGifts()
            _uiState.update { it.copy(liveRooms = rooms, liveGifts = gifts) }
        }
    }

    fun loadPlans() {
        viewModelScope.launch {
            val plans = repository.loadPlans()
            val features = repository.loadPlanFeatures()
            _uiState.update { it.copy(plans = plans, planFeatures = features) }
        }
    }

    fun activatePlayBilling(sku: String, purchaseToken: String, acknowledge: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(billingMessage = "Validando sua compra com segurança…") }
            repository.activatePlayBilling(sku, purchaseToken).onSuccess {
                _uiState.update { it.copy(billingMessage = "Plano ativado com sucesso.") }
                loadAccountData()
                acknowledge()
            }.onFailure { error -> _uiState.update { it.copy(billingMessage = error.message ?: "Não foi possível ativar o plano.") } }
        }
    }

    fun publishVideo(description: String, product: Product? = null) {
        val userId = _uiState.value.user?.id ?: return
        val videoUrl = _uiState.value.uploadUrl ?: return
        viewModelScope.launch {
            val ok = repository.publishVideo(SocialVideoInsert(userId, videoUrl, description.ifBlank { null }, product?.id, product?.affiliateUrl, product?.title, product?.price, product?.imageUrl, null))
            _uiState.update { it.copy(publishMessage = if (ok.getOrDefault(false)) "Publicação criada." else "Não foi possível criar a publicação.") }
            if (ok.getOrDefault(false)) refresh()
        }
    }

    fun uploadMedia(bucket: String, path: String, bytes: ByteArray, contentType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadUrl = null) }
            val url = repository.uploadObject(bucket, path, bytes, contentType)
            _uiState.update { it.copy(isUploading = false, uploadUrl = url) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update { it.copy(user = null, profile = null, publicProfileId = null, isFollowingPublicProfile = false, favoriteIds = emptySet(), points = null, pointTransactions = emptyList(), authMessage = null, accountMessage = null, publishMessage = null, billingMessage = null, liveMessage = null, activeLive = null, adminData = null, isAdmin = false, notifications = emptyList(), conversations = emptyList(), messages = emptyList(), subscription = null) }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(authMessage = null, error = null) } }
}
