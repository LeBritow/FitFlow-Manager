# FitFlow Manager

Sistema de gestão para academias em **Java (JavaFX + JPA + Hibernate)**.

### Stack
* **Java 25**
* **JavaFX 21** com **FXML** (UI Desktop)
* **PostgreSQL** + **Hibernate 6** (Persistência JPA)
* **Gson** (JSON)
* **Maven** (Build)
* **Servidor HTTP embutido** (Interface mobile)

### Funcionalidades
- Gestão de usuários (Admin, Instrutores, Alunos) com autenticação e controle de senhas.
- Cadastro de alunos com avaliação física.
- Montagem de treinos com exercícios, séries, repetições e programação.
- Acompanhamento de treinos realizados com detalhamento por item.
- Interface mobile acessível via servidor HTTP embutido.

### Arquitetura do Projeto
Organização em pacotes por domínio (DDD leve), cada um com suas próprias camadas:

| Pacote | Responsabilidade |
|---|---|
| **core** | Infraestrutura: configuração JPA, sessão, servidor mobile, classes principais e UI de login |
| **admin** | Gestão de administradores e instrutores (DAO, Model, UI) |
| **aluno** | Gestão de alunos e avaliações físicas (DAO, Model, UI) |
| **treino** | Gestão de treinos, exercícios e séries (DAO, Enums, Model, UI) |

### Estrutura de Pastas
```text
src/
├── main/java/com/mycompany/academia/
│   ├── core/
│   │   ├── config/     # JPAUtil, ServidorMobile, SetupBanco
│   │   ├── session/    # SessaoUsuario, SessaoTreino
│   │   ├── ui/         # Login, PainelPrincipal, RecuperarSenha, TrocarSenha
│   │   ├── Academia.java   # Entry point JavaFX
│   │   └── Launcher.java   # Wrapper alternativo
│   ├── admin/
│   │   ├── dao/        # UsuarioDAO
│   │   ├── model/      # Admin, Instrutor, Usuario
│   │   └── ui/         # FormUsuarioController
│   ├── aluno/
│   │   ├── dao/        # AlunoDAO
│   │   ├── model/      # Aluno, AvaliacaoFisica
│   │   └── ui/         # UsuariosController, AnaliseAluno, DetalhesTreino
│   └── treino/
│       ├── dao/        # ExercicioDAO, TreinoDAO
│       ├── enums/      # ObjetivoTreino
│       ├── model/      # Treino, Exercicio, SerieTreino, ItemTreino, etc.
│       └── ui/         # ExerciciosController, FichasTreino, FormExercicio
└── main/resources/
    ├── fxml/           # 11 telas JavaFX (Login, PainelPrincipal, etc.)
    ├── META-INF/       # persistence.xml
    └── FitFlow app/    # Interface mobile (HTML)
```

### Executando o projeto
```bash
mvn clean compile exec:java
```

O servidor mobile será iniciado automaticamente junto com a interface desktop.

Sistema criado 100% em Vibe Coding, devido a proporção que tomou.
