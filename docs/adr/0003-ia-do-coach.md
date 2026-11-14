# ADR 0003: IA que escreve o diagnóstico do coach

- **Data:** 13/11/2026
- **Status:** aceita

## Contexto

O Raio-X calcula os números; uma IA transforma esses números num diagnóstico com no máximo 3 focos, exercícios e
metas. Na fase 0 comparamos um modelo pago com um modelo aberto grátis, às cegas: o mesmo Raio-X foi para os
dois, os textos saíram embaralhados como A e B e o jogador de teste deu as notas antes de abrir o gabarito
(comando `--dota2coach.coach.account-id`, ver README).

Foram duas rodadas com a conta de teste (Lenda 2, posição principal meio):

1. **Raio-X v1** (só médias gerais): os dois textos ficaram parecidos e os dois erraram a posição do jogador,
   porque o relatório mostrava a função oficial dos heróis (Nuker, Carry) e não a posição. Conclusão: o problema
   estava nos dados, não no modelo.
2. **Raio-X v2** (posição informada pelo jogador, percentis contra outros jogadores do herói e detalhes das
   partidas com replay analisado): os dois melhoraram, e o texto preferido foi o do Claude.

| Modelo | Tempo | Tokens (entrada / saída) | Custo por relatório |
| --- | ---: | ---: | ---: |
| Claude Opus 5.5 (`claude-opus-5-5`, esforço alto) | 46,8 s | 5.332 / 4.537 | US$ 0,112 |
| gpt-oss-120b na Groq | 6,3 s | 3.582 / 2.645 | grátis |

## Decisão

Usar o **Claude Opus 5.5** pela API da Anthropic para o diagnóstico, com o fallback do servidor ligado (se o
pedido for recusado, a API tenta outro modelo em vez de devolver só a recusa).

O cliente compatível com OpenAI continua no código: serve para novas comparações às cegas quando o prompt ou os
dados mudarem, e como plano B se a API da Anthropic ficar indisponível.

## Consequências

- **Custo:** cerca de US$ 0,11 por diagnóstico. Com o orçamento de R$ 300 por mês, dá algo como 450 diagnósticos
  por mês. O preço do plano pago precisa cobrir isso com folga.
- **Tempo:** cerca de 45 segundos por diagnóstico. A tela precisa mostrar progresso, ou o diagnóstico precisa ser
  gerado em segundo plano.
- **Como baixar o custo depois, nesta ordem:** medir a qualidade com esforço médio; cachear o prompt de sistema;
  gerar o diagnóstico só uma vez por ciclo de treino, não a cada visita.
- **Repetir o comparativo** sempre que o Raio-X ganhar seções novas ou o prompt mudar bastante, para confirmar que
  a escolha continua valendo.
