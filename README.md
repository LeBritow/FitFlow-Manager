# FitFlow Manager

Sistema de gestão para academias em **Java (JavaFX + JPA)**.

### Stack
* **Java 21**
* **JavaFX** (UI)
* **PostgreSQL** + **Hibernate** (Persistência)
* **Maven** (Build)

### O que faz?
- Gestão de alunos e instrutores.
- Montagem de treinos (Templates vs. Personalizados).
- Gerador automático de fichas (Sorteio inteligente por grupo muscular).

### Rodando o projeto
```bash
# 1. Clone
git clone https://github.com/LeBritow/sistema-academia.git

# 2. Build
mvn clean install

# 3. Start
mvn exec:java -Dexec.mainClass="com.mycompany.academia.Launcher"
