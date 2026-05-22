# FitFlow Manager

Sistema de gestão para academias em **Java (JavaFX + JPA)**.

### Stack
* **Java 21**
* **JavaFX** (UI)
* **PostgreSQL** + **Hibernate** (Persistência)
* **Maven** (Build)

### O que faz?
- Gestão de alunos e instrutores.
- Montagem de treinos (Templates e Personalizados).
- Gerador automático de fichas (Sorteio inteligente por grupo muscular).

### Estrutura de Pastas
```text
FitFlow-Manager/
├── src/main/java/com/mycompany/academia/
│   ├── config/      # Configurações do JPA
│   ├── controller/  # Controladores JavaFX
│   ├── dao/         # Data Access Objects
│   ├── model/       # Entidades @Entity
│   └── session/     # Gerenciamento de sessão
└── src/main/resources/fxml/ # Telas da interface (FXML)

### Rodando o projeto
```bash
# 1. Clone
git clone https://github.com/LeBritow/sistema-academia.git

# 2. Build
mvn clean install

# 3. Start
mvn exec:java -Dexec.mainClass="com.mycompany.academia.Launcher"
