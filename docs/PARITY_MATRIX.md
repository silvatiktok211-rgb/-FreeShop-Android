# Matriz de paridade — AfiliShop Web → Android nativo

Esta matriz impede que funcionalidades do site sejam esquecidas durante a reconstrução. O repositório `silvatiktok211-rgb/afilishop` é somente leitura; todas as mudanças Android ficam neste repositório.

| Área | Recursos do site | Android | Estado |
|---|---|---|---|
| Autenticação | Login, cadastro, recuperação e redefinição de senha | Supabase Auth, sessão persistente e deep link de recuperação | Em validação |
| Início | Banners, categorias, produtos, destaques e navegação | Home Compose, cards, banners e categorias | Base implementada |
| Busca | Busca textual, categorias e filtros | Explorar e repositório de busca | Base implementada; filtros avançados pendentes |
| Produto | Galeria, preço, desconto, comentários, loja, favorito e link afiliado | Detalhe nativo e abertura externa segura | Parcial |
| Favoritos | Lista e alternância por usuário | Repositório e tela nativos | Implementado |
| Publicação | Importar produto, link afiliado, foto/vídeo e postagem | Seletor nativo e upload | Parcial |
| Vídeos | Feed vertical, autoplay, curtidas, comentários, compartilhamento e produtos | Media3/ExoPlayer e feed vertical | Parcial; interações completas em revisão |
| Perfil | Avatar, bio, métricas, posts, seguidores e seguindo | Perfil público/próprio e edição | Parcial |
| Loja | Página pública do vendedor e produtos | Rota e tela base | Parcial |
| Comunidade | Solicitações, conversas e presença | Comunidade e polling autenticado | Implementado; Realtime em revisão |
| Mensagens | Conversas e mensagens individuais | Tela e repositório nativos | Base implementada |
| Notificações | Central, preferências e push | FCM, token e central nativa | Base implementada |
| VIP | Planos, assinatura, status e benefícios | BillingClient + validação no backend | Base implementada |
| IA VIP | Busca contextual, limites, filtros, ranking e histórico | Endpoint móvel autenticado | Parcial; paridade de regras em auditoria |
| Pontos e ranking | Saldo, histórico e classificação | Tela de pontos | Parcial |
| Lives | Salas, Agora, presentes e interação | Cliente Agora e telas base | Parcial |
| Administração | Usuários, vendedores, produtos, vídeos, banners, categorias, planos, mensagens, gifts, métricas, integrações ML e configurações | Gate por `user_roles` | Parcial; telas administrativas específicas pendentes |
| Configurações | Conta, privacidade, notificações e segurança | Tela e repositório nativos | Base implementada |
| Mercado Livre | MLB/MLBU, catálogo, OAuth e afiliados | Parser cliente; operações privilegiadas no backend | Base implementada |
| Segurança | RLS, papéis, segredos server-side e logs | Sem service keys no APK | Obrigatório |
| Offline/desempenho | Cache, paginação e estados de erro | Estrutura inicial | Pendente de endurecimento |
| Acessibilidade | Contraste, TalkBack e tamanho de fonte | Material 3 | Auditoria pendente |
| Entrega | APK, AAB, assinatura e Play Console | CI gera APK debug | Em validação |

## Critérios para marcar “concluído”

Um módulo só pode ser concluído quando:

1. compila no CI;
2. possui estado de carregamento, vazio, sucesso e erro;
3. respeita autenticação, RLS e papéis;
4. funciona sem WebView/TWA;
5. não contém segredo privado no APK;
6. tem fluxo equivalente ao site;
7. foi testado em tela pequena e grande;
8. não quebra links, sessões ou dados já existentes.
