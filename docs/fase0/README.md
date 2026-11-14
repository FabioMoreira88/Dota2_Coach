# Fase 0: validação

Relatórios gerados para validar os números e a análise antes do MVP.

- `raio-x-<account id>.md`: Raio-X do perfil gerado pelo comando descrito no README da raiz. Conferir os números
  com o Dotabuff usando a lista no fim de cada relatório.
- `comparativo-ia-<account id>.md` e `gabarito-ia-<account id>.md`: diagnóstico escrito por cada modelo, às cegas,
  e o gabarito com modelo, tempo, tokens e custo. A escolha da IA está no [ADR 0003](../adr/0003-ia-do-coach.md).

## Gate da fase 0

- [x] Números do Raio-X conferem com o Dotabuff (diferença menor que 0,5 ponto percentual; ver o print na conversa
  de 11/11/2026: total 8824 x 8800, Pudge 1116 x 1114, Sniper e Ogre Magi idênticos).
- [x] IA escolhida: Claude Opus 5.5 ([ADR 0003](../adr/0003-ia-do-coach.md)).
