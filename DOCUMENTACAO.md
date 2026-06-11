# FitFlow Manager — Documentação Técnica

> **Versão PDF:** gere com `python gerar_pdf.py` (ou abra [DOCUMENTACAO.pdf](DOCUMENTACAO.pdf) se disponível).

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

## 14. Fundamentação Técnica — Para Saber na Apresentação

### 14.1. Por que usamos esta tecnologia?

#### Por que `@Inheritance(strategy = JOINED)` em vez de `SINGLE_TABLE`?

`SINGLE_TABLE` joga tudo numa tabela só com uma coluna `DTYPE`, mas obriga que colunas de subclasses sejam `NULL` — desperdício e sem integridade. `JOINED` normaliza: cada subclasse tem sua própria tabela só com suas colunas, e o banco garante a consistência via FK compartilhando o `id`.

#### Por que `com.sun.net.httpserver` em vez de Spring Boot ou Tomcat?

O projeto é um trabalho acadêmico focado em **JavaFX puro + JPA**. Um servidor HTTP completo como Spring Boot adicionaria centenas de dependências e复杂idade. O `com.sun.net.httpserver` já vem embutido no JDK — zero dependência extra, suficiente para servir uma SPA mobile e uma API REST simples.

#### Por que `Server-Sent Events` em vez de WebSocket para o monitoramento?

O monitoramento é **unidirecional** (servidor → navegador). SSE é mais simples que WebSocket: usa HTTP comum, não precisa de handshake especial, funciona em qualquer proxy reverso e o JavaScript consome com `new EventSource(url)` sem bibliotecas extras. WebSocket só valeria a pena se houvesse comunicação bidirecional.

#### Por que `Platform.runLater()` em operações de banco?

O JavaFX tem uma **thread única** para renderizar a UI. Operações de banco (JPA) podem travar essa thread, congelando a tela. A solução: rodar JPA em uma thread separada e, quando terminar, usar `Platform.runLater()` para voltar à thread do JavaFX e atualizar a UI com segurança.

#### Por que `ObservableList` + `PropertyValueFactory` nas tabelas?

O JavaFX TableView precisa **notificar a UI** quando os dados mudam. `ObservableList` dispara eventos de inserção/remoção automaticamente. `PropertyValueFactory` usa reflection para ligar cada coluna ao atributo do modelo — sem ele, precisaríamos escrever `setCellValueFactory` manualmente para cada coluna.

#### Por que `CopyOnWriteArrayList` no EventBus?

O EventBus é **thread-safe** — listeners podem se inscrever em qualquer thread, e eventos podem ser emitidos de qualquer thread (inclusive do servidor HTTP com pool de threads). `CopyOnWriteArrayList` é ideal para cenários com **muita leitura e pouca escrita** (nosso caso).

#### Por que `AtomicInteger` nos contadores de GIF? (`ExerciciosController.java:119`)

Várias threads disparam buscas de GIF ao mesmo tempo. Um `int` comum sofreria **race condition** (duas threads incrementando ao mesmo tempo perdem atualizações). `AtomicInteger` garante incremento atômico sem precisar de `synchronized`.

#### Por que `EnumType.STRING` no `objetivo` do Treino?

`EnumType.ORDINAL` salva o número do enum (0, 1, 2...). Se alguém reordenar os valores no código, os dados do banco ficam corrompidos. `STRING` salva o nome (`"HIPERTROFIA"`, `"EMAGRECIMENTO"`) — legível e resistente a reordenação.

---

### 14.2. Exemplos de conceitos Java no código

#### Sobrecarga (overloading)

Dois métodos com **mesmo nome** mas **assinaturas diferentes**:

```java
// AnaliseAlunoController.java:440 e 471
private void abrirModalDetalhesTreino(SessaoTreino sessao) { ... }
private void abrirModalDetalhesTreino(ComentarioTreino comentario) { ... }

// DetalhesTreinoRealizadoController.java:45 e 93
public void carregarDadosReais(Aluno aluno, Treino treino, LocalDateTime data, String texto) { ... }
public void carregarDadosReais(ComentarioTreino comentario) { ... }
```

#### Sobreposição (override)

Métodos que **reescrevem** o comportamento da classe pai com `@Override`:

```java
// Academia.java:11 — sobrescreve o start() do JavaFX
@Override
public void start(Stage palcoPrincipal) throws Exception { ... }

// ServidorMobile.java:128, 166, 234, etc. — 8 handlers sobrescrevendo HttpHandler
static class LoginHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException { ... }
}
```

#### Herança

`Usuario` é classe **abstrata** com `@Inheritance(strategy = InheritanceType.JOINED)`:

```java
// Usuario.java — classe base abstrata
public abstract class Usuario { ... }

// Admin.java, Instrutor.java, Aluno.java — herdam Usuario
public class Aluno extends Usuario { ... }
```

No banco, isso gera uma tabela `usuario` com colunas comuns, e tabelas separadas `admin`, `instrutor`, `aluno` só com colunas específicas, compartilhando o mesmo `id`.

#### Polimorfismo

O método `handle(HttpExchange)` é implementado de **8 formas diferentes** em `ServidorMobile.java`. O servidor HTTP chama o mesmo método sem saber qual handler específico está sendo executado:

```java
// ServidorMobile.java:88-112 — todos registrados como HttpHandler
httpServer.createContext("/api/login", new LoginHandler());
httpServer.createContext("/api/ficha", new BuscarFichaHandler());
httpServer.createContext("/api/treino/finalizar", new FinalizarTreinoHandler());
// + mais 5 handlers...
```

Outro exemplo: `StringConverter` tem implementações diferentes para `Aluno`, `ProgramacaoTreino`, `Treino` e `AvaliacaoFisica`, mas o JavaFX chama sempre o mesmo método `toString()`.

#### Generics

Em todo lugar. Os principais exemplos:

```java
// TableView tipada — DetalhesTreinoRealizadoController.java:19
@FXML private TableView<LinhaExecucao> tabelaExecucao;

// ObservableList tipada — AnaliseAlunoController.java:51
private ObservableList<Aluno> todosAlunos;

// JPA queries com tipo explícito — TreinoDAO.java:104
List<ProgramacaoTreino> r = em.createQuery("SELECT p FROM ProgramacaoTreino p ...", ProgramacaoTreino.class);

// Wildcard generics — TableUtils.java:7
public static void autoFitColumns(TableView<?> tableView) { ... }
```

#### Lambdas e streams

Lambdas são usadas extensivamente em listeners do JavaFX:

```java
// AnaliseAlunoController.java:63
comboBuscaAluno.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
    if (novo != null) {
        alunoSelecionado = novo;
        mostrarDetalhesAluno(novo);
    }
});
```

**Stream** para filtrar exercícios sem GIF:

```java
// ExerciciosController.java:105-107
List<Exercicio> semGif = todos.stream()
    .filter(e -> e.getUrlMidia() == null || e.getUrlMidia().isEmpty())
    .toList();
```

#### Programação concorrente (threads)

```java
// PainelPrincipalController.java:45 — servidor HTTP em background
new Thread(() -> { ServidorMobile.iniciar(); }).start();

// LoginController.java:76 — login assíncrono
new Thread(() -> {
    Usuario u = usuarioDAO.autenticar(login, senha);
    Platform.runLater(() -> { /* atualiza UI */ });  // volta pra thread do JavaFX
}).start();

// ServidorMobile.java:60 — pool de threads para requisições HTTP
servidorAtual.setExecutor(Executors.newCachedThreadPool());

// ExerciciosController.java:119 — AtomicInteger para contadores thread-safe
AtomicInteger sucesso = new AtomicInteger(0);
```

#### Padrões de projeto

**Observer (pub-sub)** — O `EventBus` mantém uma lista de listeners e os notifica quando um evento ocorre:

```java
// EventBus.java — singleton, thread-safe (CopyOnWriteArrayList)
private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

public static void emit(String component, String action, String detail) {
    Event event = new Event(component, action, detail, System.currentTimeMillis());
    for (Listener l : listeners) l.onEvent(event);
}

// Uso: ServidorMobile.java:529 — inscrição no bus
EventBus.Listener listener = event -> { /* envia via SSE */ };
EventBus.subscribe(listener);
```

Outros padrões: **DAO** (`AlunoDAO`, `TreinoDAO`), **Singleton** (`EventBus`, `JPAUtil`), **MVC** (controllers JavaFX, models JPA, DAOs).

#### Classes aninhadas (nested/inner classes)

```java
// ServidorMobile.java — 8 handlers como classes internas estáticas
static class LoginHandler implements HttpHandler { ... }
static class SSEHandler implements HttpHandler { ... }

// EventBus.java — interface Listener e classe Event internas
public static class Event { ... }
public interface Listener { void onEvent(Event e); }

// AnaliseAlunoController.java:592 — DTO interno
public static class ItemHistorico { ... }

// DetalhesTreinoRealizadoController.java:147 — DTO interno
public static class LinhaExecucao { ... }
```

---

### 14.3. Como funciona na prática?

#### Server-Sent Events (SSE)

O servidor HTTP mantém uma **conexão aberta** com o navegador. O `SSEHandler` se inscreve no `EventBus` e, para cada evento recebido, escreve no OutputStream da conexão:

```java
// ServidorMobile.java:512-561
static class SSEHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) {
        ex.getResponseHeaders().add("Content-Type", "text/event-stream");
        ex.sendResponseHeaders(200, 0);
        OutputStream out = ex.getResponseBody();

        EventBus.subscribe(event -> {
            synchronized (out) {
                String msg = "data: " + new Gson().toJson(event) + "\n\n";
                out.write(msg.getBytes());
                out.flush();
            }
        });

        while (true) {
            Thread.sleep(30000);
            synchronized (out) { /* keepalive */ }
        }
    }
}
```

#### CascadeType vs ON DELETE CASCADE

```java
// ItemTreino.java
@OneToMany(mappedBy = "itemTreino", cascade = CascadeType.ALL, orphanRemoval = true)
private List<SerieTreino> seriesTreino = new ArrayList<>();
```

O `CascadeType.ALL` no JPA garante que, ao **salvar** um `ItemTreino` com séries, tudo seja persistido junto. O `ON DELETE CASCADE` no banco é o **plano B** para deleções via `em.remove()` no pai. As demais entidades (ex: `SessaoTreino` → `ItemRealizado`) dependem exclusivamente do CASCADE do banco, pois não têm CascadeType anotado.

#### Diferença entre `ItemTreino` e `ItemRealizado`

`ItemTreino` é o **planejado**: um exercício com séries teóricas dentro de um treino. `ItemRealizado` é o **executado**: registra a carga que o aluno usou, se fez ou pulou, tempos de execução/descanso. Um `ItemTreino` pode gerar zero ou muitos `ItemRealizado` ao longo do tempo.
