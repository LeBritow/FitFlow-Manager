# FitFlow Manager — Documentação Técnica

## 1. Visão Geral

Sistema desktop + mobile para gestão de academias. Um instrutor ou administrador utiliza a interface JavaFX para cadastrar alunos, montar fichas de treino, acompanhar avaliações físicas e visualizar dashboards. O aluno acessa um SPA mobile (via navegador) para ver sua ficha atual, registrar treinos e acompanhar seu progresso.

---

## 2. Stack Tecnológica

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | Java | 25 |
| UI Desktop | JavaFX | 21.0.1 |
| XML de Telas | FXML | — |
| ORM | Hibernate (JPA) | 6.4.4.Final |
| Banco | PostgreSQL | 42.7.3 (driver) |
| JSON | Gson | 2.10.1 |
| Build | Maven | — |
| Servidor HTTP | `com.sun.net.httpserver` | embutido no JDK |
| UI Mobile | HTML + CSS + JS (SPA) | — |

---

## 3. Arquitetura

### 3.1. Camadas

```
┌──────────────────────────────────────────────────────────┐
│                    Desktop App (JavaFX)                  │
│           Academia.java → Login.fxml → PainelPrincipal  │
│                          │                               │
│              ┌───────────┼─────────────┐                 │
│              ▼           ▼             ▼                 │
│      Dashboard  Usuarios/Exercicios  AnaliseAluno       │
│      Inicio     FichasTreino                             │
└──────────────────────┬───────────────────────────────────┘
                       │ inicia em thread separada
                       ▼
┌──────────────────────────────────────────────────────────┐
│        Servidor HTTP Embutido (porta 8081)               │
│  ┌─────────┐ ┌────────┐ ┌──────────┐ ┌──────────────┐  │
│  │REST API │ │SSE     │ │Static    │ │EventBus      │  │
│  │/api/*   │ │/api/sse│ │Files (SPA)│ │pub-sub      │  │
│  └────┬────┘ └────────┘ └──────────┘ └──────────────┘  │
└───────┼──────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────┐
│              DAOs (com EventBus.emit)                    │
│  UsuarioDAO · AlunoDAO · ExercicioDAO · TreinoDAO        │
└──────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────┐
│           Hibernate + JPA (EntityManager)                │
│              PostgreSQL (sistema_academia)                │
└──────────────────────────────────────────────────────────┘
```

### 3.2. Organização por Domínio (DDD leve)

Pacotes raiz sob `com.mycompany.academia`:

| Pacote | Responsabilidade | Subpacotes |
|--------|----------------|------------|
| `core` | Infraestrutura comum | `config`, `session`, `ui`, `util` |
| `admin` | Gestão de administradores e instrutores | `dao`, `model`, `ui` |
| `aluno` | Gestão de alunos e avaliações | `dao`, `model`, `ui` |
| `treino` | Gestão de treinos, exercícios e séries | `dao`, `enums`, `model`, `ui` |

Cada domínio segue o padrão:
- **model/** → entidades JPA
- **dao/** → classes de acesso a dados
- **ui/** → controladores JavaFX (se houver telas desktop)

---

## 4. Modelo de Domínio (Entidades JPA)

### 4.1. Hierarquia `Usuario` (JOINED)

```
usuario (tabela base)
  ├── admin (sem colunas extras)
  ├── instrutor (coluna: cref)
  └── aluno (colunas: peso, altura, imc)
```

`Usuario` é abstrata com `@Inheritance(strategy = InheritanceType.JOINED)`.

### 4.2. Entidades e Relacionamentos

```
aluno ──1:N── avaliacao_fisica
aluno ──1:N── programacao_treino
aluno ──1:N── comentario_treino
treino ──1:N── item_treino
treino ──1:N── programacao_treino
treino ──1:N── comentario_treino
exercicio ──1:N── item_treino
item_treino ──1:N── serie_treino
item_treino ──1:N── item_realizado
programacao_treino ──1:N── sessao_treino
sessao_treino ──1:N── item_realizado
```

### 4.3. Mapeamento completo

| Entidade | Tabela | Chave estrangeira |
|----------|--------|-------------------|
| `Usuario` | `usuario` | — |
| `Admin` | `admin` | `id` → `usuario.id` |
| `Instrutor` | `instrutor` | `id` → `usuario.id` |
| `Aluno` | `aluno` | `id` → `usuario.id` |
| `AvaliacaoFisica` | `avaliacao_fisica` | `aluno_id` → `aluno.id` |
| `Treino` | `treino` | — |
| `Exercicio` | `exercicio` | — |
| `ItemTreino` | `item_treino` | `treino_id` → `treino.id`, `exercicio_id` → `exercicio.id` |
| `SerieTreino` | `serie_treino` | `item_treino_id` → `item_treino.id` |
| `ProgramacaoTreino` | `programacao_treino` | `aluno_id` → `aluno.id`, `treino_id` → `treino.id` |
| `SessaoTreino` | `sessao_treino` | `programacao_treino_id` → `programacao_treino.id` |
| `ItemRealizado` | `item_realizado` | `sessao_treino_id` → `sessao_treino.id`, `item_treino_id` → `item_treino.id` |
| `ComentarioTreino` | `comentario_treino` | `aluno_id` → `aluno.id`, `treino_id` → `treino.id` |

### 4.4. ON DELETE CASCADE

Todas as 11 constraints FK do banco utilizam `ON DELETE CASCADE` (definido no `schema.sql`). Isso permite que o Java use `em.remove()` em entidades pai sem precisar deletar manualmente os filhos. Exemplo: ao excluir uma `ProgramacaoTreino`, o banco remove automaticamente as `SessaoTreino` vinculadas, que por sua vez removem os `ItemRealizado`.

---

## 5. Aplicação Desktop (JavaFX)

### 5.1. Entry Point

`Launcher.main()` → `Academia.main()` → `Application.start()`:

1. Carrega `Login.fxml` como cena inicial
2. Usuário autentica via `UsuarioDAO.autenticar()`
3. Se senha for `"123456"`, força tela `TrocarSenhaObrigatoria.fxml`
4. Senão, abre `PainelPrincipal.fxml`

### 5.2. PainelPrincipal

É o shell principal. Possui:
- Sidebar esquerda com botões de navegação
- `StackPane areaConteudo` onde os FXML filhos são carregados
- Ao inicializar, dispara `ServidorMobile.iniciar()` em uma thread separada

### 5.3. Navegação entre telas

Cada botão na sidebar carrega um FXML diferente dentro de `areaConteudo`:

| Botão | FXML | Controller |
|-------|------|-----------|
| Início | `DashboardInicio.fxml` | `DashboardInicioController` |
| Usuários | `Usuarios.fxml` | `UsuariosController` |
| Exercícios | `Exercicios.fxml` | `ExerciciosController` |
| Fichas de Treino | `FichasTreino.fxml` | `FichasTreinoController` |
| Análise de Aluno | `AnaliseAluno.fxml` | `AnaliseAlunoController` |

### 5.4. Mapa completo FXML × Controller

| FXML | Controller | Localização |
|------|-----------|-------------|
| `Login.fxml` | `LoginController` | `core.ui` |
| `PainelPrincipal.fxml` | `PainelPrincipalController` | `core.ui` |
| `DashboardInicio.fxml` | `DashboardInicioController` | `core.ui` |
| `RecuperarSenha.fxml` | `RecuperarSenhaController` | `core.ui` |
| `TrocarSenhaObrigatoria.fxml` | `TrocarSenhaObrigatoriaController` | `core.ui` |
| `Usuarios.fxml` | `UsuariosController` | `aluno.ui` |
| `FormUsuario.fxml` | `FormUsuarioController` | `admin.ui` |
| `AnaliseAluno.fxml` | `AnaliseAlunoController` | `aluno.ui` |
| `DetalhesTreinoRealizado.fxml` | `DetalhesTreinoRealizadoController` | `aluno.ui` |
| `Exercicios.fxml` | `ExerciciosController` | `treino.ui` |
| `FormExercicio.fxml` | `FormExercicioController` | `treino.ui` |
| `FichasTreino.fxml` | `FichasTreinoController` | `treino.ui` |

### 5.5. Ciclo de vida de uma tela

1. `PainelPrincipalController` chama `FXMLLoader.load(getClass().getResource("/fxml/Tela.fxml"))`
2. O FXML instancia o controller (definido em `fx:controller`)
3. JavaFX injeta campos `@FXML` automaticamente
4. `controller.initialize()` é chamado
5. O controller emite `EventBus.emit("Desktop", "NomeController.acao", "descricao")`

---

## 6. Servidor Mobile (HTTP embutido)

### 6.1. Inicialização

`ServidorMobile.iniciar()` cria um `HttpServer` na porta 8081 usando `com.sun.net.httpserver`. Registra handlers:

| Rota | Handler | Método | Descrição |
|------|---------|--------|-----------|
| `/api/login` | `LoginHandler` | POST | Autentica aluno, retorna token |
| `/api/ficha` | `BuscarFichaHandler` | GET | Retorna ficha ativa do aluno |
| `/api/treino/finalizar` | `FinalizarTreinoHandler` | POST | Finaliza treino, salva itens realizados |
| `/api/aluno/dashboard` | `DashboardHandler` | GET | Métricas do dashboard |
| `/api/aluno/historico` | `HistoricoHandler` | GET | Feed de treinos + feedbacks |
| `/api/aluno/perfil` | `PerfilHandler` | GET/PUT | Dados do perfil |
| `/api/sse` | `SSEHandler` | GET | Server-Sent Events (monitor em tempo real) |
| `/` | `StaticFileHandler` | GET | Arquivos estáticos da SPA mobile |

### 6.2. SPA Mobile

Arquivos em `src/main/resources/FitFlow app/`:

```
FitFlow app/
├── pages/
│   ├── login.html      → tela de login mobile
│   ├── app.html        → SPA principal (dashboard, treino, perfil, feed)
│   └── fluxo.html      → monitor de eventos SSE + diagrama de fluxo
├── js/
│   ├── app.js          → lógica da SPA (navegação, API calls)
│   └── fluxo.js        → diagrama de conexões + consumo SSE
└── css/
    ├── style.css       → estilos da SPA mobile
    └── fluxo.css       → estilos do diagrama de fluxo
```

### 6.3. Fluxo de requisição mobile

```
Navegador (celular)
    │
    ▼
StaticFileHandler → serve app.html + js/css
    │
    ▼
app.js → fetch("/api/login", {method:"POST", body:...})
    │
    ▼
ServidorMobile.LoginHandler → UsuarioDAO.autenticar()
    │                          │
    │                          ▼
    │                    EventBus.emit("PostgreSQL", "SELECT...")
    │
    ▼
Retorna JSON {token, id, nome, email}
    │
    ▼
app.js armazena token no sessionStorage
```

---

## 7. EventBus (Pub-Sub em Memória)

### 7.1. Funcionamento

`EventBus` é um singleton que implementa o padrão publisher-subscriber em memória:

```java
// Emitir evento (qualquer lugar)
EventBus.emit("AlunoDAO", "salvarOuAtualizar", "aluno=João");

// Inscrever-se (ex: SSEHandler)
EventBus.subscribe(event -> {
    // event.component → "AlunoDAO"
    // event.action    → "salvarOuAtualizar"
    // event.detail    → "aluno=João"
    // event.timestamp → System.currentTimeMillis()
});
```

### 7.2. Categorias de eventos

| Componente | Origem | Exemplo |
|-----------|--------|---------|
| `"Desktop"` | Controllers JavaFX | `"UsuariosController.listarUsuarios"` |
| `"AlunoDAO"` | `AlunoDAO` | `"buscarTodos"` |
| `"TreinoDAO"` | `TreinoDAO` | `"salvarProgramacao"` |
| `"ExercicioDAO"` | `ExercicioDAO` | `"listarTodos"` |
| `"UsuarioDAO"` | `UsuarioDAO` | `"autenticar"` |
| `"ServidorMobile"` | Handlers HTTP | `"LoginHandler"` |
| `"PostgreSQL"` | Simula query SQL | `"SELECT FROM usuario..."` |
| `"JPA"` | Operações JPA | `"EntityManager.persist(SessaoTreino)"` |
| `"Entities"` | Entidades carregadas | `"Aluno loaded"` |

### 7.3. Para que serve?

O EventBus alimenta a **página de monitoramento SSE** (`fluxo.html`). Quando o celular faz uma requisição, o servidor emite eventos que são transmitidos em tempo real via SSE para o navegador que está na página `fluxo.html`. Isso permite visualizar o fluxo completo de uma operação: qual controller foi chamado, qual DAO, qual query SQL, qual entidade foi carregada.

---

## 8. Diagrama de Fluxo (fluxo.html + fluxo.js)

A página `fluxo.html` contém um **diagrama visual interativo** que mostra:

- **12 boxes de controller** (azul) com seus respectivos **12 boxes de FXML** (índigo)
- **Boxes de DAO** (verde): TreinoDAO, AlunoDAO, ExercicioDAO, UsuarioDAO
- **Boxes de infraestrutura** (laranja): JPA, PostgreSQL, Entities, DB
- **Setas animadas** conectando os boxes conforme os eventos chegam

### 8.1. Event Queue

Os eventos são enfileirados e processados a cada 1 segundo (`STEP_MS = 1000`). A cada step:

1. O evento é removido da fila
2. O box correspondente acende (classe `.done` removida)
3. A seta entre o box anterior e o atual é desenhada/anima
4. O método sendo chamado aparece dinamicamente nos boxes de DAO/infra (`.cur-method`)
5. O **último controller + seu FXML** permanecem acesos durante eventos de DAO/infra (via `lastControllerId`)
6. Quando um novo evento de controller chega, o anterior apaga

### 8.2. Zoom e Pan

- Botões **+ / -** para zoom
- **Ctrl + scroll do mouse** para zoom
- Botão **⟲** para resetar zoom

---

## 9. Fluxo de Dados (Exemplo Completo)

### 9.1. Aluno faz login no celular

```
1. app.js → fetch POST /api/login {login, senha}
2. ServidorMobile.LoginHandler.processa()
3.   → EventBus.emit("ServidorMobile", "LoginHandler", "Recebendo POST")
4.   → UsuarioDAO.autenticar(login, senha)
5.     → EventBus.emit("UsuarioDAO", "autenticar", "login=" + login)
6.     → EventBus.emit("PostgreSQL", "SELECT FROM usuario WHERE...")
7.     → EventBus.emit("Entities", "Usuario+" + u.getClass().getSimpleName() + " loaded")
8.   → Se OK: gera token, retorna JSON {token, id, nome, email}
```

### 9.2. Admin cria uma ficha de treino (Desktop)

```
1. PainelPrincipalController → carrega FichasTreino.fxml
2. FichasTreinoController.initialize()
3.   → EventBus.emit("Desktop", "FichasTreinoController.inicializar", "...")
4. Usuário preenche nome, objetivo, exercícios, séries
5. Clica "Salvar"
6.   → TreinoDAO.salvarProgramacao(programacao)
7.     → EventBus.emit("TreinoDAO", "salvarProgramacao", "alunoId=...")
8.     → EventBus.emit("PostgreSQL", "INSERT INTO programacao_treino...")
9.     → EventBus.emit("Entities", "ProgramacaoTreino saved")
```

### 9.3. Admin analisa dados de um aluno (Desktop)

```
1. PainelPrincipalController → carrega AnaliseAluno.fxml
2. AnaliseAlunoController.initialize()
3.   → comboBuscaAluno populado via AlunoDAO.buscarTodos()
4. Seleciona um aluno
5.   → AlunoDAO.buscarAvaliacoesPorAluno(alunoId)
6.   → TreinoDAO.buscarNomeFichaAtiva(alunoId)
7.   → TreinoDAO.buscarDataUltimoTreino(alunoId)
8.   → TreinoDAO.buscarQuantidadeTreinosMes(alunoId)
9.   → TreinoDAO.buscarNomesExerciciosPorAluno(alunoId)
10.  → TreinoDAO.buscarComentariosPorAluno(alunoId)
11.  → TreinoDAO.buscarSessoesPorAluno(alunoId)
12. Gráficos de peso/IMC e carga são renderizados
```

---

## 10. Banco de Dados

### 10.1. Configuração

`persistence.xml` → `jdbc:postgresql://localhost:5432/sistema_academia`  
User: `postgres` / Password: `ifsp`  
DDL: execute `schema.sql` no banco, ou deixe `hibernate.hbm2ddl.auto = update` para o Hibernate criar automaticamente

### 10.2. Scripts auxiliares

| Arquivo | Função |
|---------|--------|
| `schema.sql` | Cria o banco completo (13 tabelas) com `ON DELETE CASCADE` em todas as FKs |
| `persistence.xml.example` | Template de configuração do banco (copiar para `persistence.xml` e ajustar credenciais) |
| `config.properties.example` | Template da chave da GIPHY API (copiar para `config.properties` e adicionar a chave) |

---

## 11. Segurança

- **Senhas** armazenadas em texto **puro** no banco (sem hash — melhoria possível)
- `Aluno` **não pode** acessar o desktop (bloqueado no `LoginController`)
- `Admin` pode gerenciar usuários; `Instrutor` tem acesso limitado
- **Primeiro login** com senha `"123456"` força troca de senha (`TrocarSenhaObrigatoriaController`)
- **Recuperação de senha** simulada em 3 etapas (email não é realmente enviado)

---

## 12. Build e Execução

### 12.1. Pré-requisitos

- JDK 25
- Maven 3.8+
- PostgreSQL rodando com banco `sistema_academia` criado

### 12.2. Configuração inicial

Antes de executar, configure os arquivos sensíveis (já ignorados pelo `.gitignore`):

| Arquivo | Como configurar |
|---------|----------------|
| `src/main/resources/META-INF/persistence.xml` | Copie de `persistence.xml.example` e ajuste usuário/senha do PostgreSQL |
| `src/main/resources/config.properties` | Copie de `config.properties.example` e insira sua chave da GIPHY API (opcional) |

### 12.3. Comandos

```bash
# Compilar e executar
mvn clean compile exec:java

# Ou gerar JAR e executar
mvn clean package
java -jar target/academia-1.0-SNAPSHOT.jar
```

O servidor mobile inicia automaticamente na porta 8081 junto com a interface desktop.

---

## 13. Estrutura de Arquivos (após limpeza)

```
src/main/java/com/mycompany/academia/
├── Academia.java                     # Entry point JavaFX
├── Launcher.java                     # Wrapper alternativo
├── admin/
│   ├── dao/UsuarioDAO.java
│   ├── model/{Admin,Instrutor,Usuario}.java
│   └── ui/FormUsuarioController.java
├── aluno/
│   ├── dao/AlunoDAO.java
│   ├── model/{Aluno,AvaliacaoFisica}.java
│   └── ui/{AnaliseAlunoController,DetalhesTreinoRealizadoController,UsuariosController}.java
├── core/
│   ├── config/{EventBus,GifSearchService,JPAUtil,SeedData,ServidorMobile,SetupBanco}.java
│   ├── session/{SessaoTreino,SessaoUsuario}.java
│   ├── ui/{DashboardInicioController,LoginController,PainelPrincipalController,
│            RecuperarSenhaController,TrocarSenhaObrigatoriaController}.java
│   └── util/TableUtils.java
└── treino/
    ├── dao/{ExercicioDAO,TreinoDAO}.java
    ├── enums/ObjetivoTreino.java
    ├── model/{ComentarioTreino,Exercicio,ItemRealizado,ItemTreino,
               ProgramacaoTreino,SerieTreino,Treino}.java
    └── ui/{ExerciciosController,FichasTreinoController,FormExercicioController}.java

src/main/resources/
├── META-INF/persistence.xml
├── fxml/ (12 arquivos .fxml)
└── FitFlow app/
    ├── pages/{app.html,fluxo.html,login.html}
    ├── js/{app.js,fluxo.js}
    └── css/{style.css,fluxo.css}
```

---

## 14. Perguntas Frequentes (para o professor)

### Por que não usar Spring Boot?

O projeto começou como um trabalho acadêmico focado em JavaFX + JPA puro. O servidor HTTP embutido (`com.sun.net.httpserver`) foi adicionado posteriormente para viabilizar o acesso mobile sem necessidade de um servidor de aplicação externo.

### Por que `ON DELETE CASCADE` no banco e não nas entidades JPA?

As entidades JPA não declaravam `CascadeType.REMOVE` nem orphan removal. A deleção em cascata foi implementada diretamente no banco via script SQL. Isso simplificou o código Java, que agora pode chamar `em.remove()` na entidade pai sem se preocupar com os filhos.

### Como funciona o monitoramento em tempo real?

O `EventBus` é um pub-sub em memória. Quando o celular faz uma requisição, cada camada (handler, DAO, JPA) emite eventos. O `SSEHandler` está inscrito no bus e transmite esses eventos via Server-Sent Events para a página `fluxo.html`, que os renderiza em um diagrama animado.

### Por que as senhas não são hasheadas?

É uma limitação conhecida. O sistema usa senha em texto puro. Em produção, seria necessário integrar BCrypt ou similar no `UsuarioDAO.autenticar()`.

### Qual a diferença entre `ItemTreino` e `ItemRealizado`?

`ItemTreino` é o planejado: um exercício com suas séries teóricas dentro de um treino. `ItemRealizado` é o executado: registra quanto o aluno realmente carregou, se fez ou pulou o exercício, tempos de execução/descanso.
