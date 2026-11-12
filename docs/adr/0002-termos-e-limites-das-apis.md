# ADR 0002: Termos e limites das APIs de dados

- **Data:** 11/11/2026
- **Status:** aceita (dois pontos ainda precisam ser confirmados, ver "Pendências")

## Contexto

O Raio-X e a análise de partidas dependem de dados públicos do Dota 2. O produto será pago, então antes do MVP
precisamos saber o que cada fonte permite em uso comercial, quanto custa e que obrigações cria para nós.

Fontes avaliadas: OpenDota, STRATZ, Steam Web API e Dotabuff.

## Decisão

| Fonte | Uso | Limites conhecidos | Custo |
| --- | --- | --- | --- |
| OpenDota | Fonte principal: partidas, heróis, itens, meta, análise de replays | Grátis, sem chave: 60 chamadas/min e 3.000/dia. Premium: 3.000/min, sem limite diário | Grátis em desenvolvimento; premium em produção a US$ 0,0001 por chamada |
| Steam Web API | Login (OpenID, sem chave) e dados de perfil | 100.000 chamadas/dia por chave | Grátis |
| STRATZ | Fora do MVP. Avaliar depois como fonte complementar | Token obrigatório; 20/s, 250/min, 2.000/h e 10.000/dia no token padrão (dados de 2020 e 2021) | Grátis com cadastro; tiers maiores exigem cota de indicações |
| Dotabuff | Não usar | Sem API pública; os termos proíbem raspagem | — |

### OpenDota

- É a única fonte que entrega, sem cadastro, partidas completas, constantes, meta por faixa e análise de replays.
- O cliente já respeita 50 chamadas/min, tenta de novo em 429 e 5xx e guarda cache. Um Raio-X faz cerca de 15
  chamadas, então o plano grátis aguenta perto de 200 Raio-X por dia: suficiente para a fase 0 e o beta fechado.
- Em produção usamos a chave premium (`OPENDOTA_API_KEY`), que o cliente já envia quando configurada. O preço é
  US$ 0,01 a cada 100 chamadas, cobrado no cartão no começo do mês, e erros 500 não são cobrados. Um Raio-X custa
  cerca de US$ 0,0015; mil Raio-X por mês saem por uns US$ 1,50. O custo entra no orçamento mensal junto com a IA.
- Mantemos o cache e a fila de coleta (Sprint 3) para não repetir chamadas: dados de partida antiga não mudam.

### Steam Web API

Os [termos da Steam Web API](https://steamcommunity.com/dev/apiterms) valem para nós desde já, porque usamos o
login da Steam. Obrigações que viram requisitos do produto:

1. Publicar uma política de privacidade que cubra os dados da Steam e cumpri-la.
2. Dizer ao usuário quais dados da Steam guardamos.
3. Guardar dados da Steam só nos países citados na política de privacidade (decidir a região do banco antes do
   beta).
4. Buscar dados de um usuário só quando ele pedir. Coletas automáticas ficam restritas a quem tem conta ativa.
5. Nunca receber nem guardar a senha da Steam. O OpenID já garante isso.
6. Manter a chave em segredo (só no `.env` e no cofre de produção).
7. Não sugerir parceria com a Valve. O aviso "não afiliado à Valve" fica no rodapé do site.
8. Não criar ferramenta que dê vantagem injusta dentro da partida. Isso reforça a decisão de não fazer sugestão
   de picks durante o draft: toda a análise é fora do jogo.
9. Não usar os dados para marketing não solicitado.
10. A Valve pode encerrar o acesso a qualquer momento. O risco fica registrado: o produto precisa continuar
    funcionando com a OpenDota se a chave da Steam cair.

### STRATZ

- Tem dados ricos (posições, timings, GraphQL), mas exige token e, nos tiers maiores, uma cota mensal de
  indicações (links para o site deles). Não há termos comerciais públicos.
- Fica fora do MVP. Se precisarmos dela depois, falamos com a equipe da STRATZ antes de usar em produto pago.

### Dotabuff

- Não tem API e os termos proíbem raspagem. Usamos o Dotabuff só como referência manual para conferir os números
  do Raio-X (portão da fase 0).

## Pendências

1. **Uso comercial da OpenDota:** a página de planos convida a "levar estatísticas aos seus usuários" e cobra
   por chamada, mas não fala de uso comercial de forma explícita. Antes do lançamento pago, confirmar com a equipe
   da OpenDota (Discord ou e-mail).
2. **Limites atuais da STRATZ:** os números acima vêm de 2020 e 2021. Só confirmar se a STRATZ voltar ao plano.
3. **Região do banco:** decidir o país de hospedagem antes do beta, para constar na política de privacidade.

## Consequências

- O MVP depende de uma fonte principal (OpenDota). O cliente fica isolado em `collector`, então trocar ou somar
  fontes não afeta `analysis` nem `api`.
- Política de privacidade, aviso de não afiliação e tela "seus dados" entram como itens da Sprint 7 (segurança e
  LGPD).
- O custo da chave da OpenDota entra no controle de gastos junto com a IA.

## Fontes

- [Termos da Steam Web API](https://steamcommunity.com/dev/apiterms)
- [Planos da OpenDota](https://www.opendota.com/api-keys), lidos no navegador em 11/11/2026
- [Limites da STRATZ por tipo de token (issue da base de conhecimento)](https://github.com/STRATZ-eSports/knowledge-base/issues/15)
- [Anúncio da STRATZ sobre tokens e cotas](https://stratz.medium.com/stratz-api-major-update-5557335dbdfd)
