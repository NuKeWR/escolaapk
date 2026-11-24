# ☢ Escola Nuklear

App Android (Kotlin + Jetpack Compose) para a gestão da Escola Nuklear de Artes Marciais:
alunos, presenças, pagamentos, inadimplência e reconhecimento facial. Os dados ficam só no celular.

- Pacote: `com.nuklear.escola2` · Versão atual: ver `versionName` em `app/build.gradle.kts`
- Android 8.0 ou mais novo (minSdk 26)

## Como gerar o APK (pelo GitHub, sem Android Studio)

1. Envie as alterações para a branch `main` (ou use **Actions → Compilar APK → Run workflow**).
2. Abra a aba **Actions**, entre na última execução e, em **Artifacts**, baixe **EscolaNuklear-APK**.
3. Instale o APK no celular. Como a assinatura é sempre a mesma, ele atualiza **por cima** do app instalado.

A assinatura vem de 4 segredos do repositório (**Settings → Secrets and variables → Actions**):
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

> ⚠️ Guarde o arquivo `escola.keystore` e as senhas em pelo menos dois lugares (fora do GitHub).
> Sem a mesma chave, o Android recusa a atualização e é preciso desinstalar o app (perde os dados).
> Nunca envie a chave para este repositório (ele é público; o `.gitignore` já bloqueia `*.keystore`).

## Regras para atualizar sem perder os dados dos alunos

1. **Suba o `versionCode`** em `app/build.gradle.kts` a cada versão nova.
2. **Mantenha o `applicationId`** (`com.nuklear.escola2`) e a mesma chave de assinatura.
3. **Se mudar o banco** (nova coluna ou tabela em `Entities.kt`): suba o `version` do `AppDatabase`
   e crie uma `Migration`. Não use `fallbackToDestructiveMigration()`: ele apaga tudo.
4. Antes de atualizar, faça **Configurações → Exportar Backup JSON**.

## Funcionalidades

| Tela | O que faz |
|---|---|
| Dashboard | Totais, financeiro do mês, status, horários, aniversariantes (cartões levam às telas) |
| Alunos / Cadastrar / Editar | Cadastro completo com foto, graduações, ativar/inativar |
| Pagamento rápido | Busca o aluno, registra o pagamento e abre o recibo |
| Pagamentos / Recibo | Histórico por aluno, edição, recibo em imagem para compartilhar |
| Inadimplentes | Lista por horário e botão **Cobrar no WhatsApp** |
| Presenças e relatórios | Presença por data/horário, ranking e frequência mensal |
| Reconhecimento facial | Presença e cadastro de rostos (câmera), cadastro de rostos em lote |
| Aniversariantes | Aniversariantes do mês |
| Histórico financeiro | Receita por mês e por aluno |
| Configurações | Backup JSON, importações do sistema antigo, **trava por PIN**, **mensagem de cobrança editável** |

A mensagem de cobrança usa os campos `{nome}`, `{aluno}`, `{mes}` e `{valor}`, preenchidos para cada aluno.

## Estrutura

```
app/src/main/java/com/nuklear/escola/
├── MainActivity.kt        navegação, menu e trava por PIN
├── data/                  banco (Room), modelos e mensagem de cobrança
├── face/                  reconhecimento facial (ML Kit + FaceNet)
├── notification/          avisos diários
├── security/PinStore.kt   PIN do app
└── ui/                    telas, componentes e tema
tools/exportar_rostos_para_apk.py   exporta os rostos do sistema antigo (PC) para importar no app
.github/workflows/build.yml         compila e assina o APK no GitHub
```

## Compilar no Android Studio (opcional)

Precisa de JDK 17. Abra a pasta do projeto e use **Build → Build APK(s)**. Sem os segredos do GitHub,
o APK sai assinado com a chave de teste do computador e não atualiza por cima do app instalado pelo GitHub.
