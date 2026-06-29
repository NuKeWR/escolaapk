# ☢ Escola Nuklear — Android Studio (Kotlin Nativo)

Projeto **100% Kotlin + Jetpack Compose** pronto para abrir no Android Studio e gerar o APK.

## 🚀 Como Compilar o APK

### Passo 1 — Baixe o Android Studio
👉 https://developer.android.com/studio

### Passo 2 — Abra o projeto
1. Abra o Android Studio
2. **File → Open** → Selecione a pasta `EscolaNuklearAndroid`
3. Aguarde o Gradle sincronizar (~2-5 min na primeira vez)

### Passo 3 — Gerar o APK
- **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- O APK fica em: `app/build/outputs/apk/debug/app-debug.apk`

### Passo 4 — Instalar no celular
- Conecte o celular via USB **ou**
- Copie o APK para o celular e instale manualmente

---

## 📱 Funcionalidades

| Tela | Funcionalidade |
|---|---|
| Dashboard | Totais, financeiro, status, horários, aniversariantes |
| Alunos | CRUD completo, filtro por horário/ativo, busca |
| Cadastrar | 15 campos + foto via câmera/galeria |
| Editar Aluno | Editar dados, graduações, links para pagamento/frequência |
| Presenças | Checkbox por aluno, filtro data/horário, salvar em lote |
| Pagamentos | Registrar, editar, excluir, histórico por aluno |
| Inadimplentes | Lista filtrada, meses de atraso, link para pagamento |
| Relatório Frequência | Ranking com medalhas, frequência mensal |
| Frequência Aluno | Lista de presenças, excluir individual/todas |
| Histórico Financeiro | Receita mensal, total por aluno |

## 🛠 Stack

- **Kotlin** + **Jetpack Compose** (UI nativa moderna)
- **Room** (SQLite — mesmo esquema do projeto original)
- **Navigation Compose** (navegação entre telas)
- **CameraX** (câmera para fotos)
- **Material 3** (design system)
- Tema: **Vermelho (#D32F2F) + Preto (#1A1A1A)**

## 📁 Estrutura

```
app/src/main/java/com/nuklear/escola/
├── MainActivity.kt              ← Navegação + Drawer
├── data/
│   ├── db/AppDatabase.kt       ← Room DAOs + Database
│   └── model/Entities.kt       ← Aluno, Pagamento, Graduação, Presença
└── ui/
    ├── theme/Theme.kt          ← Cores vermelho/preto
    ├── components/Components.kt ← Cards, TopBar, itens reutilizáveis
    └── screens/
        ├── DashboardScreen.kt
        ├── AlunosScreen.kt
        ├── CadastrarAlunoScreen.kt
        ├── EditarAlunoScreen.kt
        ├── PresencasScreen.kt
        ├── PagamentosScreen.kt
        ├── InadimplentesScreen.kt
        ├── RelatorioFrequenciaScreen.kt
        ├── FrequenciaAlunoScreen.kt
        └── HistoricoFinanceiroScreen.kt
```
