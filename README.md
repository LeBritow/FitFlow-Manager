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

### Arquitetura do Projeto
O projeto foi estruturado para ser escalável e de fácil manutenção, seguindo o padrão de separação de responsabilidades:

* **config**: Configurações de infraestrutura (conexão com banco, persistência com JPA).
* **controller**: Lógica de controle das telas e interação com o usuário (JavaFX).
* **dao**: Data Access Objects, gerenciando a persistência com JPA/Hibernate.
* **model**: Contém as entidades do banco de dados (ex: Aluno, Treino).
* **session**: Gerenciamento de estado da sessão do usuário.

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
```
