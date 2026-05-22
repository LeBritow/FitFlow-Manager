Como Contribuir

Fico muito feliz que você tenha interesse em contribuir com o
**FitFlow Manager**! Para que o projeto se mantenha organizado e
funcional, siga estas diretrizes simples.

### ️ Requisitos de Ambiente
Para rodar este projeto localmente, você precisará de:
* **Java 21** (ou superior).
* **Maven** instalado e configurado.
* **PostgreSQL** instalado.
* 
###  Fluxo de Trabalho
1. **Fork o projeto:** Crie sua própria versão do repositório.
2. **Crie uma Branch:** Nunca envie mudanças direto para a `main`.
Use nomes descritivos para suas tarefas:
```bash
git checkout -b feature/nome-da-feature
```

3. **Configure o Banco de Dados:**
* Localize o arquivo
`src/main/resources/META-INF/persistence.example`.
* Renomeie-o para `persistence.xml`.
* Insira suas credenciais do PostgreSQL (usuário e senha).
* *Atenção: Nunca suba o arquivo `persistence.xml` real para o
GitHub.*
4. **Codifique:** Siga o padrão de nomenclatura existente nas
classes (`DAO`, `Controller`, etc).
5. **Commit:** Faça commits com mensagens claras e objetivas (ex:
`feat: adiciona gerador de treino` ou `fix: corrige erro no login`).

###  Boas Práticas
* **Segurança em primeiro lugar:** Jamais inclua senhas ou chaves de
acesso no seu código. O arquivo `persistence.xml` é ignorado pelo
Git propositalmente.
* **Código limpo:** Mantenha o padrão de identação e nomeie
variáveis de forma clara para facilitar a leitura.

* **Documentação:** Se adicionar uma funcionalidade nova, atualize o
`README.md` para incluir instruções de uso.

###  Dúvidas ou Sugestões?
Se tiver qualquer problema ao rodar o projeto ou quiser propor uma

mudança grande, abra uma **Issue** aqui no GitHub descrevendo o que
você pretende fazer.
---
*Obrigado por ajudar a tornar o FitFlow melhor!*
