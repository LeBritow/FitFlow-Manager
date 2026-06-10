# FitFlow Manager

Sistema de gestão para academias em **Java (JavaFX + JPA + Hibernate)** com monitor de arquitetura em tempo real via SSE.

### Stack
* **Java 25**
* **JavaFX 21** com **FXML** (UI Desktop)
* **PostgreSQL** + **Hibernate 6** (Persistência JPA)
* **Gson** (JSON)
* **Maven** (Build)
* **Servidor HTTP embutido** (Interface mobile + SSE)

### Funcionalidades
- Gestão de usuários (Admin, Instrutores, Alunos) com autenticação e controle de senhas.
- Cadastro de alunos com avaliação física.
- Montagem de treinos com exercícios, séries, repetições e programação.
- Acompanhamento de treinos realizados com detalhamento por item.
- Interface mobile SPA com navegação inferior (Feed, Treino, Performance, Perfil).
- Monitor de arquitetura em tempo real via SSE (`fluxo.html`).
- Gifs de exercícios via Giphy API.

### Arquitetura do Projeto
Organização em pacotes por domínio (DDD leve), cada um com suas próprias camadas:

| Pacote | Responsabilidade |
|---|---|
| **core** | Infraestrutura: configuração JPA, sessão, servidor mobile, EventBus (SSE), GifSearchService, classes principais e UI de login |
| **admin** | Gestão de administradores e instrutores (DAO, Model, UI) |
| **aluno** | Gestão de alunos e avaliações físicas (DAO, Model, UI) |
| **treino** | Gestão de treinos, exercícios e séries (DAO, Enums, Model, UI) |

### Estrutura de Pastas
```text
src/
├── main/java/com/mycompany/academia/
│   ├── core/
│   │   ├── config/       # JPAUtil, ServidorMobile, EventBus, GifSearchService, SetupBanco
│   │   ├── session/      # SessaoUsuario, SessaoTreino
│   │   ├── ui/           # Login, PainelPrincipal, RecuperarSenha, TrocarSenha
│   │   ├── Academia.java     # Entry point JavaFX
│   │   └── Launcher.java     # Wrapper alternativo
│   ├── admin/
│   │   ├── dao/          # UsuarioDAO
│   │   ├── model/        # Admin, Instrutor, Usuario
│   │   └── ui/           # FormUsuarioController
│   ├── aluno/
│   │   ├── dao/          # AlunoDAO (buscarAvaliacoesPorAluno, contarAlunos)
│   │   ├── model/        # Aluno, AvaliacaoFisica
│   │   └── ui/           # UsuariosController, AnaliseAluno, DetalhesTreino
│   └── treino/
│       ├── dao/          # ExercicioDAO, TreinoDAO (17 métodos, todos com EventBus.emit)
│       ├── enums/        # ObjetivoTreino
│       ├── model/        # Treino, Exercicio, SerieTreino, ItemTreino, ProgramacaoTreino,
│       │                 # ComentarioTreino, ItemRealizado
│       └── ui/           # ExerciciosController, FichasTreino, FormExercicio
└── main/resources/
    ├── fxml/             # Telas JavaFX (Login, PainelPrincipal, etc.)
    ├── META-INF/         # persistence.xml (ignorado pelo git)
    └── FitFlow app/      # Interface mobile SPA
        ├── pages/        # login.html, app.html, fluxo.html
        ├── css/          # style.css
        ├── js/           # app.js
        └── assets/
```

### Endpoints do Servidor Mobile (porta 8081)

| Rota | Método | Descrição |
|---|---|---|
| `/` | GET | Redireciona para `/pages/login.html` |
| `/pages/login.html` | GET | Tela de login mobile |
| `/pages/app.html` | GET | SPA mobile (navegação inferior) |
| `/pages/fluxo.html` | GET | Diagrama de classes em tempo real (SSE) |
| `/api/login` | POST | Autenticação do aluno |
| `/api/ficha` | GET | Ficha de treino ativa |
| `/api/treino/finalizar` | POST | Finaliza treino com itens realizados |
| `/api/exercicios` | GET | Lista todos os exercícios |
| `/api/aluno/dashboard` | GET | Métricas de performance (treinos/mês, streak, etc.) |
| `/api/aluno/historico` | GET | Feed de histórico do aluno |
| `/api/aluno/perfil` | GET/PUT | Dados do perfil e avaliações físicas |
| `/api/sse` | GET | Server-Sent Events para monitor em tempo real |

### Monitor de Arquitetura em Tempo Real

O `fluxo.html` (disponível em `http://localhost:8081/fluxo.html`) é um diagrama de classes animado que mostra em tempo real cada chamada ao banco de dados:

- **Zona Desktop (JavaFX)**: eventos dos controllers da UI desktop
- **Zona Mobile (SPA)**: eventos dos handlers do ServidorMobile
- **DAO Layer**: TreinoDAO, AlunoDAO, ExercicioDAO, UsuarioDAO
- **Infrastructure**: JPAUtil, PostgreSQL, Entities

Todas as chamadas aos DAOs disparam eventos via `EventBus.java` que são transmitidos por SSE para o `fluxo.html`, destacando as caixas de classe e animando as linhas de conexão.

### API Key do Giphy

O `GifSearchService.java` lê a chave da variável de ambiente `GIPHY_API_KEY` ou da propriedade de sistema `giphy.api.key`.

Para configurar:

**Windows (cmd):**
```cmd
setx GIPHY_API_KEY "sua_chave_aqui"
```
**Windows (PowerShell):**
```powershell
[Environment]::SetEnvironmentVariable("GIPHY_API_KEY", "sua_chave_aqui", "User")
```
**Linux/macOS:**
```bash
export GIPHY_API_KEY="sua_chave_aqui"
```

Obtenha uma chave em: https://developers.giphy.com/

### Executando o projeto
```bash
mvn clean compile exec:java
```

O servidor mobile será iniciado automaticamente junto com a interface desktop em `http://localhost:8081`.
