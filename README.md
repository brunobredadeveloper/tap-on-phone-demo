# Breda Tap Terminal

Versão 0.3: experiência Android com múltiplos fluxos, autenticação opcional e identidade visual própria. Pagamentos são fictícios.

## Identidade e experiência 0.3

Ícone adaptativo compartilhado **breda_dev** nos quatro apps. Splash nativo Android seguido de abertura Lottie contextual por 3,2 segundos (no lançamento, sem repetir na rotação). Formulários com teclado adequado, mostrar/ocultar senha e navegação login/cadastro/recuperação. Estados de operação ocupam uma tela própria: preparação/processamento, sucesso e erro, com animações grandes e ações adequadas ao resultado. A demo deixa os estados rápidos visíveis; isso não altera o contrato do núcleo de pagamentos.

As esteiras incluem uma jornada instrumentada em emulador: splash, login, cadastro/recuperação, modo local, operação, sucesso/erro, histórico e saída. Também verificam que Lottie renderiza pixels e muda de frame. Capturas e relatórios ficam no artifact `*-ux-evidence`. As capturas usam frame fixo para comparação; as animações permanecem ativas nos APKs normais.

## Fluxos disponíveis

Terminal escuro e âmbar: dashboard, preparação da cobrança, aproximação animada, processamento, resultado, comprovante, histórico com filtros, integração HTTP, diagnóstico NFC e perfil.

## Engenharia demonstrada

Checkout com estados explícitos, ledger persistente por conta e chave pendente recuperada após reinício. Resposta perdida é conciliada por consulta; nova cobrança bloqueada enquanto houver pendência. Firestore opcional registra histórico do usuário autenticado. NFC só diagnostica disponibilidade: não lê cartão nem processa EMV.

| Aplicativo Firebase | Package Android |
|---|---|
| Breda Tap Terminal | `dev.breda.tap` |

## Autenticação Firebase

1. Crie um projeto no Firebase Console e habilite **Authentication → Sign-in method → Email/Password**. Não é necessário habilitar login anônimo.
2. Registre o aplicativo Android usando o package da tabela abaixo. Baixe seu `google-services.json`.
3. No GitHub deste repositório, crie o secret de Actions **FIREBASE_CONFIG_JSON** com o JSON inteiro do aplicativo correspondente. O script CI valida o package e gera um asset de configuração; o arquivo pessoal não é versionado.
4. Execute a esteira novamente para gerar um APK configurado. Para desenvolvimento local: `FIREBASE_CONFIG_JSON="$(cat /caminho/google-services.json)" python3 scripts/configure_firebase.py`.
5. Crie conta no aplicativo, saia, entre novamente, envie recuperação de senha e verificação de e-mail. Firebase restaura a sessão. Senhas não são salvas pelo aplicativo.

Sem configuração, o APK oferece **demonstração local** explicitamente identificada. Ela não autentica no Firebase. Autenticação real, entrega de e-mails e isolamento Firestore exigem validar com seu projeto. Os dados locais são separados pelo hash SHA-256 do UID; isso é organização por conta, não criptografia de dados em repouso. Firebase Auth identifica o usuário, mas não transforma simuladores de negócio em uma API de pagamentos autorizada.

## Experiência e animação

Onboarding, splash, formulários de login/cadastro/recuperação, navegação com transições, feedback de carregamento/sucesso/erro via Lottie e perfil. As animações geométricas em `src/main/assets/motion_*.json` foram criadas para este projeto; não dependem de downloads em tempo de execução. Layout Compose com rolagem, estados vazios e componentes Material 3. Testes visuais e acessibilidade no aparelho ainda são necessários.

## Build e validação

A esteira `.github/workflows/ci.yml` executa testes de domínio e/ou SDK, testes de isolamento de sessão, Android Lint e montagem de APK debug. Em **Actions → Tests and APK → Artifacts**, baixe o ZIP de APK e extraia o `.apk`. A chave debug é preservada por cache quando disponível; se o cache for perdido, poderá ser necessário desinstalar a versão anterior. Esse APK é para demonstração, sem publicação de release.

Veja `docs/advanced-experience.md` para o roteiro manual. Testes automatizados de núcleo não substituem teste de navegação e integração Firebase em um celular.


## Clean Code, MVVM e SOLID

ViewModels recebem ports por construtor e a UI observa estados. Casos de uso, adapters e injeção manual estão descritos em [Arquitetura e decisões](docs/architecture.md), com os contratos e os testes correspondentes.
