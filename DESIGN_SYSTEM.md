# Design System Minum - Android

## Fundamentos

- Cores semanticas: `MinumColorTokens` em `presentation/theme/MinumTokens.kt`.
- Espacamento: escala de 4, 8, 12, 16, 24 e 32 dp em `MinumSpacing`.
- Raios: 6 e 8 dp. Superficies operacionais nao usam raios grandes.
- Tipografia: Carbona e a fonte oficial, mas nao foi entregue em formato Android. O app usa `FontFamily.SansSerif` como fallback temporario em `Theme.kt`.

## Componentes

- `MinumLogo`: variantes para superficies claras, escuras e energeticas.
- `MinumLine`: linha institucional de dois segmentos; use como apoio pontual de titulos e estados.
- `AppScaffold`: cabecalho padronizado com retorno, acoes e linha Minum.
- `MinumSectionHeader`, `MinumMetricCard` e `MinumActionRow`: blocos de informacao e acao para telas de campo.
- `EmptyState` e `LoadingState`: estados claros, com mensagem e acao opcional.

## Icones e movimento

Use a familia Material Symbols ja presente no aplicativo em tamanhos de 20 dp para controles, 24 dp para navegacao e 32 dp somente em estados. Transicoes devem ficar entre 120 e 240 ms; respeite a preferencia do sistema quando houver reducao de movimento.

## Acessibilidade

Os controles interativos possuem descricao, contraste baseado em tokens e alvos de toque de pelo menos 48 dp. Status e carregamentos nao dependem apenas de cor.
