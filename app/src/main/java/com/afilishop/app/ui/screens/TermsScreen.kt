package com.afilishop.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(padding: PaddingValues, onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = { TopAppBar(title = { Text("Termos de Uso", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } }) },
    ) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item { Term(Icons.Default.Shield, "1. Aceitação dos Termos", "Ao acessar e usar a AfiliShop, você concorda em cumprir estes termos. Se não concordar, não utilize o serviço. O aplicativo atua como agregador de ofertas de terceiros.") }
            item { Term(Icons.Default.Person, "2. Responsabilidade do Usuário", "Você é responsável por manter a confidencialidade da conta e senha. É proibido usar o aplicativo em atividades ilegais ou fraudulentas. Comentários e conteúdos devem ser respeitosos e livres de discurso de ódio.") }
            item { Term(Icons.Default.Warning, "3. Links de Terceiros e Afiliados", "A AfiliShop contém links para lojas externas. Ao abrir uma oferta, você estará sujeito aos termos da loja de destino. Podemos receber comissão pelas compras realizadas através desses links.") }
            item { Term(Icons.Default.Lock, "4. Privacidade de Dados", "Coletamos dados básicos para personalizar a experiência. Seus dados não são vendidos. Você pode solicitar a exclusão da conta nas configurações.") }
            item { Term(Icons.Default.Gavel, "5. Limitação de Responsabilidade", "Os preços dependem de fontes externas e podem mudar. A AfiliShop não se responsabiliza por perdas decorrentes de compras realizadas em sites de terceiros.") }
            item { Text("Última atualização: 9 de maio de 2026", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp)) }
        }
    }
}

@Composable private fun Term(icon: ImageVector, title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
