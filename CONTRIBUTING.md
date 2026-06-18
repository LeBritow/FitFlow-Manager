### Como Contribuir

Fico muito feliz que você tenha interesse em contribuir com o **FitFlow**! Para que o projeto se mantenha organizado e funcional, siga estas diretrizes.

### Requisitos de Ambiente

- **Java 25** (ou superior)
- **Maven** instalado e configurado 
- **PostgreSQL** instalado e rodando

### Fluxo de Trabalho

1. **Fork o projeto:** Crie sua própria versão do repositório.
2. **Crie uma Branch:** Nunca envie mudanças direto para a `main`. Use nomes descritivos:
   ```bash
   git checkout -b feature/nome-da-feature
   ```
3. **Configure o Banco de Dados:**
   - Crie um banco PostgreSQL chamado `sistema_academia`.
   - Copie `src/main/resources/META-INF/persistence.xml.example` para `src/main/resources/META-INF/persistence.xml`.
   - Edite `persistence.xml` com suas credenciais do PostgreSQL (usuário e senha).
   - *O `persistence.xml` já está no `.gitignore` — seguro.*
4. **(Opcional) Busca automática de GIFs:**
   - A busca de GIFs usa a [API do Giphy](https://developers.giphy.com/).
   - Crie uma conta e gere uma **API Key** gratuita em `https://developers.giphy.com/dashboard/`.
   - Copie `src/main/resources/config.properties.example` para `src/main/resources/config.properties`.
   - Edite `config.properties` com sua chave real.
   - *O `config.properties` já está no `.gitignore` — seguro.*
5. **Codifique:** Siga a organização por pacotes de domínio (`admin/`, `aluno/`, `treino/`, `core/`), cada um com `dao/`, `model/` e `ui/`.
6. **Commit:** Use mensagens claras e objetivas (ex: `feat: adiciona gerador de treino` ou `fix: corrige erro no login`).

### Boas Práticas

- **Código limpo:** Mantenha a indentação e nomeie variáveis de forma clara.
- **Documentação:** Se adicionar funcionalidade nova, atualize o `README.md`.
- **Mobile:** Se alterar o servidor HTTP, atualize os arquivos em `src/main/resources/FitFlow app/`.

### Dúvidas ou Sugestões?

Abra uma **Issue** no GitHub descrevendo o que você pretende fazer.

---

*Obrigado por ajudar a tornar o FitFlow melhor!*
