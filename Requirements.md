# \# Requirements - English Learning App (MVP)

# 

# > \*\*Status:\*\* Draft v1.0

# > \*\*Última atualização:\*\* 27/06/2026

# > \*\*Owner:\*\* \[Seu Nome]

# 

# \---

# 

# \## 1. Visão do Produto

# 

# \### 1.1 Problema

# Brasileiros iniciantes no inglês (A1) abandonam cursos tradicionais por falta de feedback imediato, personalização e prática contextualizada.

# 

# \### 1.2 Proposta de Valor

# Curso estruturado de gramática com correção inteligente, usando IA apenas onde agrega valor real (feedbacks dinâmicos) e conteúdo hard-coded onde a precisão é crítica (regras gramaticais).

# 

# \### 1.3 Público-Alvo do MVP

# \- Brasileiros adultos (18-45 anos)

# \- Nível A1 (iniciante absoluto)

# \- Objetivo: construir base sólida do verbo TO BE antes de avançar

# 

# \---

# 

# \## 2. Escopo do MVP (Fase 1)

# 

# \### 2.1 Funcionalidades IN (Obrigatórias)

# 

# | ID | Funcionalidade | Prioridade |

# |----|---------------|------------|

# | F-01 | Cadastro/Login (email + senha) | P0 |

# | F-02 | Login social (Google OAuth) | P1 |

# | F-03 | Dashboard com progresso do aluno | P0 |

# | F-04 | Trilha linear do Módulo 1 (Verbo TO BE - 10 lições) | P0 |

# | F-05 | Exercícios de múltipla escolha | P0 |

# | F-06 | Exercícios fill-in-the-blank | P1 |

# | F-07 | Exercícios de tradução PT→EN | P1 |

# | F-08 | Correção automática baseada em gabarito (determinística) | P0 |

# | F-09 | Feedback imediato de erro com explicação contextual | P0 |

# | F-10 | Áudios pré-gerados (MP3) para listening/pronúncia passiva | P0 |

# | F-11 | Progresso por lição (% concluído, melhor nota) | P0 |

# | F-12 | Bloqueio de lições por pré-requisito | P1 |

# | F-13 | Gamificação básica (XP por lição concluída) | P1 |

# 

# \### 2.2 Funcionalidades OUT (Adiadas para Fase 2+)

# 

# > \*\*Por que cortar?\*\* Cada item abaixo adiciona complexidade técnica, custo operacional ou risco pedagógico desproporcional ao valor no MVP.

# 

# \- ❌ \*\*Conversação em tempo real com IA\*\* (latência >1.5s destrói UX; custo proibitivo)

# \- ❌ \*\*Avaliação fonética de pronúncia\*\* (APIs especializadas são caras; STT genérico dá feedback errado - ver Risco R-02)

# \- ❌ \*\*Geração dinâmica de exercícios por IA\*\* (risco de alucinação em conteúdo pedagógico - ver R-03)

# \- ❌ \*\*Teste de nivelamento adaptativo\*\* (público do Módulo 1 é claramente A1; adiciona complexidade sem benefício)

# \- ❌ \*\*Leaderboards globais / batalhas PvP\*\* (precisa massa crítica de usuários para fazer sentido)

# \- ❌ \*\*Revisão espaçada (Anki-like)\*\* (validar primeiro se usuários completam a trilha linear)

# \- ❌ \*\*Reconhecimento de voz para ditar respostas\*\* (depende de STT; adiar até ter budget)

# \- ❌ \*\*App mobile nativo\*\* (começar com PWA/Web responsivo para validar mais rápido)

# \- ❌ \*\*Módulos 2+ (outros temas gramaticais)\*\* (validar retenção no Módulo 1 antes de produzir mais conteúdo)

# 

# \---

# 

# \## 3. Restrições Técnicas Críticas

# 

# > Estas restrições \*\*não são negociáveis\*\*. Qualquer solução proposta deve respeitá-las.

# 

# \### 3.1 Stack Backend

# \- \*\*Java 21 + Kotlin + Spring Boot 3.x\*\* (stack principal do time)

# \- \*\*Python apenas para scripts offline\*\* (geração de MP3s com Edge-TTS, processamento batch)

# \- \*\*NÃO usar Python para o core da API\*\* (ver Decisão D-01)

# 

# \### 3.2 Persistência

# \- \*\*PostgreSQL 16\*\* como banco principal (com uso de JSONB para flexibilidade de exercícios)

# \- \*\*Redis\*\* apenas para cache de conteúdo estático e sessões (não para dados críticos)

# \- \*\*NÃO usar MongoDB, Neo4j ou Elasticsearch no MVP\*\* (over-engineering)

# 

# \### 3.3 Conteúdo Pedagógico

# \- Toda gramática base (tabelas de conjugação, regras) será \*\*hard-coded\*\* e versionada

# \- IA \*\*não pode\*\* ser fonte primária de conteúdo didático (apenas complementar)

# \- Todo exercício deve ter gabarito determinístico armazenado no banco

# 

# \### 3.4 Latência

# \- Tempo de resposta de qualquer endpoint da API: \*\*< 500ms (p95)\*\*

# \- Se integração com IA for usada, o tempo total (STT → LLM → TTS) deve ser \*\*< 1.5s\*\*, caso contrário a feature deve ser convertida para modo assíncrono (turnos)

# 

# \### 3.5 Custo Unitário

# \- Meta de custo de infraestrutura + APIs por usuário ativo mensal: \*\*< R$ 2,00\*\*

# \- Qualquer feature que ultrapasse esse limite deve ter plano de mitigação (cache, modelo menor, batch)

# 

# \---

# 

# \## 4. Casos de Uso Principais

# 

# Baseados no Módulo 1 (Verbo TO BE) dos materiais pedagógicos:

# 

# \### UC-01: Cadastro e Primeiro Acesso

# 1\. Usuário informa email + senha

# 2\. Sistema cria conta e perfil (nível padrão: A1)

# 3\. Usuário é redirecionado ao Dashboard

# 4\. Dashboard mostra Módulo 1, Lição 1.1 desbloqueada

# 

# \### UC-02: Iniciar Lição (ex: 1.1 - TO BE Presente Afirmativo)

# 1\. Usuário clica em "Iniciar Lição 1.1"

# 2\. Sistema carrega: teoria (tabela de conjugação), exemplos, áudios MP3

# 3\. Usuário navega pelo conteúdo no próprio ritmo

# 

# \### UC-03: Responder Exercício de Múltipla Escolha

# 1\. Sistema exibe pergunta (ex: "Qual a maneira correta de afirmar: 'Ele está'?")

# 2\. Usuário seleciona alternativa

# 3\. Sistema registra tentativa com timestamp

# 4\. Sistema compara resposta com gabarito (determinístico)

# 

# \### UC-04: Receber Feedback

# \- \*\*Se acertou:\*\* mensagem positiva + XP + próxima questão

# \- \*\*Se errou:\*\* explicação contextual (ex: "Lembre-se: HE/SHE/IT usa IS no presente") + exemplo correto + opção de tentar novamente

# 

# \### UC-05: Completar Lição

# 1\. Usuário atinge 80% de acertos no quiz final da lição

# 2\. Sistema marca lição como "completa"

# 3\. Sistema desbloqueia próxima lição da trilha

# 4\. Sistema concede XP e atualiza progresso no Dashboard

# 

# \### UC-06: Revisar Erro

# 1\. Usuário acessa seção "Meus Erros" no Dashboard

# 2\. Sistema lista questões erradas com explicação e resposta correta

# 3\. Usuário pode refazer exercícios errados

# 

# \---

# 

# \## 5. Premissas e Riscos Identificados

# 

# | ID | Tipo | Descrição | Mitigação |

# |----|------|-----------|-----------|

# | R-01 | Risco | Latência de APIs de IA inviabiliza conversação em tempo real | MVP não terá conversação; validar latência com PoC na Fase 0 |

# | R-02 | Risco | STT genérico (Whisper) corrige erros de pronúncia do aluno por contexto, dando feedback falso positivo | Não usar STT para avaliação; apenas para transcrição de ditado |

# | R-03 | Risco | LLMs alucinam regras gramaticais em exercícios gerados dinamicamente | Conteúdo pedagógico 100% hard-coded no MVP |

# | R-04 | Risco | Custo de APIs de IA explode com usuários ativos | Uso agressivo de cache + modelos menores (Llama 3) para tarefas simples |

# | R-05 | Premissa | Usuários do MVP são verdadeiramente iniciantes (A1) | Se teste de nivelamento for necessário, adicionar em Fase 2 |

# | R-06 | Premissa | 10 lições do Módulo 1 são suficientes para validar retenção | Monitorar métrica de conclusão; se < 30%, revisar conteúdo antes de expandir |

# 

# \---

# 

# \## 6. Decisões Técnicas Registradas

# 

# \### D-01: Stack Backend - Java/Kotlin em vez de Python

# \*\*Contexto:\*\* Hype de mercado sugere Python para apps de IA.

# \*\*Decisão:\*\* Usar Java 21 + Kotlin + Spring Boot para o core da API.

# \*\*Justificativa:\*\* 

# \- Time já é sênior nessa stack (velocidade de entrega)

# \- Tipagem forte reduz bugs em produção

# \- Consumo de APIs de IA (OpenAI, Groq) é trivial via HTTP client

# \- Python restrito a scripts batch (geração de MP3)

# 

# \### D-02: Banco de Dados - PostgreSQL único + Redis

# \*\*Contexto:\*\* Tentação de usar "banco certo para cada caso" (Mongo, Neo4j, InfluxDB).

# \*\*Decisão:\*\* PostgreSQL 16 (com JSONB) + Redis apenas para cache.

# \*\*Justificativa:\*\*

# \- JSONB oferece flexibilidade de documento sem abrir mão de ACID

# \- Particionamento nativo resolve problema de séries temporais (exercise\_attempts)

# \- Redis apenas para dados não-críticos e de alta leitura

# 

# \### D-03: Áudio - MP3 pré-gerados em vez de TTS em tempo real

# \*\*Contexto:\*\* TTS dinâmico (ElevenLabs, OpenAI) tem custo por caractere.

# \*\*Decisão:\*\* Gerar MP3s offline com Edge-TTS e servir via CDN/storage.

# \*\*Justificativa:\*\*

# \- Custo de execução zero em produção

# \- Latência zero (arquivo estático)

# \- Qualidade suficiente para conteúdo estruturado

# \- TTS dinâmico adiado para Fase 2 (apenas para features dinâmicas)

# 

# \### D-04: Arquitetura - Monolítico Modular

# \*\*Contexto:\*\* Tendência de começar com microsserviços.

# \*\*Decisão:\*\* Monolítico modular em Spring Boot, organizado por features (modules/lessons/exercises/auth).

# \*\*Justificativa:\*\*

# \- Deploy simples (1 artifact)

# \- Debugging trivial

# \- Extração para microsserviço só quando houver justificativa clara (ex: worker de IA pesado)

# 

# \---

# 

# \## 7. Decisões Pendentes (A Resolver Antes do Design)

# 

# > Estas decisões \*\*bloqueiam\*\* o `design.md` e devem ser resolvidas na próxima reunião.

# 

# \### PD-01: Frontend do MVP

# \- \[ ] Opção A: PWA (Progressive Web App) com React + Vite

# \- \[ ] Opção B: Mobile com React Native / Expo

# \- \[ ] Opção C: Web tradicional responsiva (sem PWA)

# \- \*\*Recomendação:\*\* Opção A (PWA) - desenvolvimento mais rápido, funciona em qualquer dispositivo, instalável

# 

# \### PD-02: Modelo de Monetização

# \- \[ ] Opção A: Freemium (Módulo 1 grátis, Módulo 2+ pago)

# \- \[ ] Opção B: Assinatura mensal desde o dia 1

# \- \[ ] Opção C: 100% gratuito na Fase 1 (foco em aquisição)

# \- \*\*Recomendação:\*\* Opção C - validar retenção antes de introduzir barreira de pagamento

# 

# \### PD-03: Hospedagem

# \- \[ ] Opção A: Railway / Fly.io (PaaS, simples, custo previsível)

# \- \[ ] Opção B: AWS Lightsail (VPS gerenciada)

# \- \[ ] Opção C: Hetzner Cloud (mais barato, mais manual)

# \- \*\*Recomendação:\*\* Opção A - foco no produto, não em DevOps

# 

# \---

# 

# \## 8. Métricas de Sucesso do MVP

# 

# O MVP será considerado \*\*validado\*\* se, após 30 dias com 50 beta testers:

# 

# | Métrica | Meta |

# |---------|------|

# | Retenção D1 (% que volta no dia seguinte) | ≥ 40% |

# | Retenção D7 (% que volta após 7 dias) | ≥ 20% |

# | Taxa de conclusão do Módulo 1 | ≥ 30% |

# | NPS (Net Promoter Score) | ≥ 30 |

# | Tempo médio por lição | 10-15 min |

# 

# Se \*\*3 ou mais métricas\*\* não forem atingidas, \*\*não avançar para Fase 2\*\*. Revisar conteúdo, UX ou proposta de valor antes de investir mais.

# 

# \---

# 

# \## 9. Fora do Escopo (Anti-Goals)

# 

# Explicitamente \*\*NÃO\*\* são objetivos do MVP:

# \- Ser "o Duolingo killer"

# \- Ter cobertura completa do idioma inglês

# \- Ter IA conversacional em tempo real

# \- Ter monetização otimizada

# \- Ter escala para milhões de usuários

# \- Ter app nativo nas stores (iOS/Android)

# 

# \---

# 

# \*\*Próximo passo:\*\* Revisar este documento e decidir as 3 questões pendentes (PD-01, PD-02, PD-03). Após aprovação, partir para o `design.md`.

