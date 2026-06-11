# FitFlow Manager

Sistema de gestão para academias com interface **Desktop (JavaFX)** e **Mobile (SPA via navegador)**.

## Stack

| Tecnologia | Versão |
|-----------|--------|
| Java | 25 |
| JavaFX + FXML | 21.0.1 |
| Hibernate (JPA) | 6.4.4.Final |
| PostgreSQL | 42.7.3 |
| Gson | 2.10.1 |
| Maven | — |

## Funcionalidades

- **Autenticação** de Admin, Instrutor e Aluno com controle de acesso por perfil
- **Gestão de usuários** (cadastro, edição, exclusão, reset de senha)
- **Cadastro de alunos** com avaliação física (peso, altura, IMC, histórico)
- **Catálogo de exercícios** com busca automática de GIFs (GIPHY API)
- **Montagem de treinos** com séries, repetições, cargas e progressão
- **Programação de fichas** para alunos com controle por período
- **Acompanhamento** de treinos realizados com detalhamento por item
- **Dashboard** com métricas, gráficos de evolução e alertas inteligentes
- **Interface mobile** acessível pelo navegador do celular
- **Monitoramento em tempo real** via diagrama de fluxo com SSE

## Como executar

```bash
mvn clean compile exec:java
```

O servidor mobile inicia automaticamente na porta 8081.

## Arquitetura

Organização em pacotes por domínio (DDD leve):

| Pacote | Responsabilidade |
|--------|----------------|
| `core` | Infraestrutura: JPA, servidor HTTP, EventBus, sessão, entry point |
| `admin` | Gestão de administradores e instrutores |
| `aluno` | Gestão de alunos e avaliações físicas |
| `treino` | Gestão de treinos, exercícios, séries e programação |

Cada domínio segue o padrão **model/** → **dao/** → **ui/**.

## Projeto

```
src/main/java/com/mycompany/academia/
├── core/         → config/, session/, ui/, util/
├── admin/        → dao/, model/, ui/
├── aluno/        → dao/, model/, ui/
└── treino/       → dao/, enums/, model/, ui/

src/main/resources/
├── META-INF/persistence.xml
├── fxml/              (12 telas JavaFX)
└── FitFlow app/       (SPA mobile: pages/, js/, css/)
```

## Documentação completa

Veja [DOCUMENTACAO.md](DOCUMENTACAO.md) para documentação técnica detalhada — arquitetura, modelo de domínio, fluxo de dados, banco de dados, EventBus, API mobile e perguntas frequentes.

## Banco de Dados

- PostgreSQL, banco `sistema_academia`
- Schema gerado automaticamente por Hibernate (`hibernate.hbm2ddl.auto=update`)
- 13 entidades JPA com herança JOINED em `Usuario`
- Todas as FKs com `ON DELETE CASCADE`

## Créditos

Sistema criado originalmente em Vibe Coding.
