# Arquitetura e decisões — v0.4.0

## MVVM e fluxo de dependências

Compose observa o estado e envia eventos. ViewModels coordenam os casos de uso e publicam o estado da apresentação. As regras e os protocolos de negócio dependem de ports; adapters implementam armazenamento, autenticação e gateways. `ViewModelInjection.kt` é a composition root: monta os adapters e injeta os construtores por `InjectedViewModelFactory`. Não há Service Locator dentro dos ViewModels.

`CheckoutRecovery` persiste antes da autorização, grava o comprovante antes de remover a pendência e mantém resultados desconhecidos conciliáveis. `CheckoutStorage`, `SaleHistory` e `CheckoutGatewayFactory` isolam preferências, Firebase e transporte.

`AccountSession` recebe `Authentication` e `SessionStore`. `FirebaseAuthentication` traduz modelos e erros do Firebase; `PreferenceSessionStore` seleciona o namespace de dados da conta antes de expor a sessão. O modo local é uma escolha explícita e nunca autentica implicitamente no Firebase. Senhas não são armazenadas. `AmountInput` rejeita centavos fracionários, expoentes, negativos e overflow antes do acesso a dados.

## SOLID aplicado

- SRP: telas apresentam; ViewModels coordenam; casos de uso validam; adapters executam IO.
- OCP: novos gateways ou autenticação podem implementar os ports existentes, sem reescrever a apresentação.
- LSP: contratos de resultado, cancelamento e persistência são preservados pelas implementações e fakes.
- ISP: autenticação, sessão, histórico, armazenamento e sincronização têm interfaces pequenas, com responsabilidade explícita.
- DIP: ViewModels dependem de abstrações recebidas por construtor; classes Android/Firebase/Room são montadas externamente.

## Patterns usados

Repository organiza os dados e seus invariantes; Adapter encapsula Firebase/armazenamento/WorkManager; Strategy seleciona gateways; Factory monta ViewModels; State organiza as transições de pagamento; Observer/StateFlow conecta estado e UI. A fila de pedidos usa outbox e controle otimista de versões. Não foram adicionados patterns que não resolvam uma necessidade concreta.

## Clean Code e limites

Código Kotlin formatado com ktfmt; nomes descrevem responsabilidades; regras monetárias não dependem de strings de mensagens da tela; cancelamento é propagado; namespaces separam contas. Os quatro apps conservam identidades visuais próprias e uma marca compartilhada. Não há processamento financeiro real. A extração usa injeção manual para manter a montagem explícita; não depende de um framework de DI.

A infraestrutura dos apps permanece no módulo Android e os cores existentes continuam independentes. Esta é uma arquitetura pragmática por responsabilidades, não uma promessa de certificação ou de operação em produção. Configuração de Firebase e verificação de login real exigem o projeto do proprietário.

## Validação

`ArchitectureContractTest` usa fakes de autenticação e sessão, sem Android ou Firebase. Testa modo local, logout, login e entradas monetárias inválidas. Casos de uso possuem testes específicos onde há invariantes de saldo ou recuperação. A esteira executa unit tests, lint, assembleDebug e testes instrumentados: splash, onboarding, login/cadastro/recuperação, navegação, loading, sucesso, erro, histórico e logout; também valida frames não vazios e distintos das quatro animações Lottie.
