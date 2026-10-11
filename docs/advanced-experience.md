# Roteiro da experiência avançada

Faça uma aprovação e uma recusa no simulador. Inicie outra cobrança após aprovação. Conecte o Merchant Console e teste lost_response, reinicie o app e consulte a chave pendente antes de cobrar novamente.

## Autenticação e estados

- Inicie sem configuração e confirme identificação de demo local.
- Com Firebase configurado: cadastre conta, saia, teste senha incorreta, entre, recupere senha e envie verificação.
- Reinicie para verificar restauração da sessão.
- Teste sem rede e confira mensagem de erro e liberação do formulário.
- Teste voltar, rotação, texto grande, teclado aberto, carregamento e navegação pelas quatro abas.
- Troque de usuário e confirme ausência dos dados do anterior.

## Limites

Checkout com estados explícitos, ledger persistente por conta e chave pendente recuperada após reinício. Resposta perdida é conciliada por consulta; nova cobrança bloqueada enquanto houver pendência. Firestore opcional registra histórico do usuário autenticado. NFC só diagnostica disponibilidade: não lê cartão nem processa EMV.

Firebase e interação visual aguardam teste manual com configuração pessoal e celular. Não existe certificação para aceitação de cartões, pagamento real ou armazenamento de dados bancários.

Firestore: publique `firestore.rules`; o histórico usa `users/{uid}/demoPayments/{id}`. O Merchant Console HTTP é um backend de laboratório sem validação de tokens Firebase e deve ser usado em ambiente local controlado. O histórico Firebase é auxiliar, escrito pelo cliente, não fonte financeira autoritativa.
