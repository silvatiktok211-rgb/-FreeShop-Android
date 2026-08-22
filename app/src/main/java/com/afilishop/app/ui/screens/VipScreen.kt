package com.afilishop.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afilishop.app.data.PlayBillingManager
import com.afilishop.app.data.PlayBillingSkus
import com.afilishop.app.data.VipStatusRepository
import com.afilishop.app.data.VipStatusSnapshot
import com.afilishop.app.model.SubscriptionPlan
import com.afilishop.app.ui.AfiliShopViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.launch

private const val EXTRA_SEARCH_PRICE_BRL = 3.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onLogin: () -> Unit,
    onAssistant: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var availableSkus by remember { mutableStateOf(emptySet<String>()) }
    var playPrices by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var vipStatus by remember { mutableStateOf(VipStatusSnapshot()) }
    var extraQuantity by remember { mutableIntStateOf(1) }
    var extraRequestMessage by remember { mutableStateOf<String?>(null) }
    var requestingExtra by remember { mutableStateOf(false) }
    val statusRepository = remember { VipStatusRepository(context) }
    val manager = remember {
        PlayBillingManager(context) { sku, token, acknowledge ->
            viewModel.activatePlayBilling(sku, token, acknowledge)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPlans()
        if (state.user != null) viewModel.loadAccountData()
    }
    LaunchedEffect(state.user?.id) {
        vipStatus = if (state.user != null) statusRepository.load() else VipStatusSnapshot()
    }
    LaunchedEffect(manager, state.plans) {
        manager.restorePurchases()
        manager.querySubscriptions(
            state.plans.filter { it.priceBrl > 0 }.mapNotNull { PlayBillingSkus.forPlanCode(it.code) }.ifEmpty { PlayBillingSkus.all },
        ) { details ->
            availableSkus = details.map { it.productId }.toSet()
            playPrices = details.associate { product ->
                val price = product.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.lastOrNull()
                    ?.formattedPrice
                    .orEmpty()
                product.productId to price
            }
        }
    }
    DisposableEffect(manager, statusRepository) {
        onDispose {
            manager.close()
            statusRepository.close()
        }
    }

    val plans = state.plans
    val paidPlans = plans.filter { it.code != "free" && it.priceBrl > 0 }
    val subscription = state.subscription
    val baseLimit = subscription?.slotsTotal ?: plans.firstOrNull { it.tier == (subscription?.tier ?: 0) }?.slots ?: 0
    val normalRemaining = max(baseLimit - vipStatus.usedToday, 0)
    val totalRemaining = normalRemaining + vipStatus.extraCredits
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = { TopAppBar(title = { Text("Área VIP") }) },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFF97316), Color(0xFFEC4899), Color(0xFF9333EA)),
                                ),
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "AFILISHOP VIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text(
                                    "Mais produtos, mais IA, mais alcance",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(top = 7.dp),
                                )
                                Text(
                                    "Escolha o nível que combina com o seu crescimento.",
                                    color = Color.White.copy(alpha = 0.90f),
                                    modifier = Modifier.padding(top = 5.dp),
                                )
                                Button(
                                    onClick = if (state.user != null) onAssistant else onLogin,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    modifier = Modifier.padding(top = 14.dp),
                                ) {
                                    Text(
                                        if (state.user != null) "Usar IA VIP" else "Entrar para conhecer",
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(72.dp),
                            )
                        }
                    }
                }
            }

            if (state.user != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Seu uso de IA hoje",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(start = 10.dp),
                                )
                            }
                            Text("${vipStatus.usedToday} buscas usadas hoje")
                            Text("$normalRemaining buscas do plano disponíveis")
                            Text("${vipStatus.extraCredits} buscas extras em saldo")
                            Text(
                                "$totalRemaining buscas disponíveis agora",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (vipStatus.totalExtraPurchased > 0) {
                                Text(
                                    "Total de buscas extras adquiridas: ${vipStatus.totalExtraPurchased}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Buscas extras", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "R$ 3,00 por busca. O saldo só é usado depois que o limite diário do plano termina.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { extraQuantity = (extraQuantity - 1).coerceAtLeast(1) },
                                        enabled = extraQuantity > 1 && !requestingExtra,
                                    ) { Icon(Icons.Default.Remove, "Diminuir") }
                                    Text(
                                        extraQuantity.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                    )
                                    IconButton(
                                        onClick = { extraQuantity = (extraQuantity + 1).coerceAtMost(100) },
                                        enabled = extraQuantity < 100 && !requestingExtra,
                                    ) { Icon(Icons.Default.Add, "Aumentar") }
                                }
                                Text(
                                    currencyFormatter.format(extraQuantity * EXTRA_SEARCH_PRICE_BRL),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }

                            Button(
                                onClick = {
                                    requestingExtra = true
                                    extraRequestMessage = null
                                    scope.launch {
                                        statusRepository.createExtraSearchRequest(extraQuantity)
                                            .onSuccess {
                                                extraRequestMessage = "Solicitação criada. As buscas serão liberadas após a confirmação do pagamento."
                                            }
                                            .onFailure {
                                                extraRequestMessage = it.message ?: "Não foi possível solicitar as buscas extras."
                                            }
                                        requestingExtra = false
                                    }
                                },
                                enabled = !requestingExtra,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (requestingExtra) "Enviando…" else "Solicitar $extraQuantity busca${if (extraQuantity > 1) "s" else ""} extra${if (extraQuantity > 1) "s" else ""}")
                            }

                            extraRequestMessage?.let {
                                Text(
                                    it,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            item { Text("Benefícios incluídos", style = MaterialTheme.typography.titleLarge) }
            items(
                listOf(
                    "Busca inteligente no Mercado Livre",
                    "Cada slot do plano vale 1 busca de IA por dia",
                    "Filtro de relevância para evitar acessórios fora do pedido",
                    "Produtos, vídeos e métricas para afiliados",
                    "Créditos extras só são consumidos após o limite diário",
                ),
            ) { benefit -> RowBenefit(benefit) }

            if (plans.isEmpty()) {
                item { Text("Nenhum plano ativo foi retornado pelo backend.") }
            }
            item { Text("Escolha seu plano", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
            items(paidPlans, key = { it.id }) { plan ->
                val currentTier = subscription?.tier ?: 0
                val isCurrent = state.user != null && (subscription?.planId == plan.id || (subscription?.planId == null && plan.tier == currentTier))
                PlanCard(
                    plan = plan,
                    loggedIn = state.user != null,
                    isCurrent = isCurrent,
                    activity = activity,
                    available = PlayBillingSkus.forPlanCode(plan.code)?.let(availableSkus::contains) == true,
                    manager = manager,
                    onLogin = onLogin,
                    billingMessage = state.billingMessage,
                    playPrice = PlayBillingSkus.forPlanCode(plan.code)?.let(playPrices::get)?.takeIf { it.isNotBlank() },
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    loggedIn: Boolean,
    isCurrent: Boolean,
    activity: Activity?,
    available: Boolean,
    manager: PlayBillingManager,
    onLogin: () -> Unit,
    billingMessage: String?,
    playPrice: String?,
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(plan.name, style = MaterialTheme.typography.titleLarge)
            Text(
                if (plan.priceBrl <= 0) "Grátis" else playPrice ?: formatter.format(plan.priceBrl),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "${plan.slots} produtos ativos • ${plan.slots} buscas de IA por dia${plan.durationDays?.let { " • $it dias" } ?: ""}",
                style = MaterialTheme.typography.labelMedium,
            )

            plan.benefits.take(3).forEach { benefit ->
                RowBenefit(benefit)
            }

            when {
                isCurrent -> Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) { Text("Plano atual") }

                plan.priceBrl <= 0 -> Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) { Text("Plano gratuito") }

                !loggedIn -> Button(
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) { Text("Entrar para assinar") }

                else -> Button(
                    onClick = {
                        if (activity != null && available) PlayBillingSkus.forPlanCode(plan.code)?.let { manager.launchSubscription(activity, it) }
                    },
                    enabled = available,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) { Text(if (available) "Assinar com Google Play" else "SKU indisponível") }
            }

            billingMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun RowBenefit(text: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        Text(text, modifier = Modifier.padding(start = 10.dp))
    }
}
