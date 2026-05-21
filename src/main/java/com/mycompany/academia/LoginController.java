package com.mycompany.academia;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

public class LoginController {

    // A anotação @FXML liga a variável do Java ao componente da tela visual
    @FXML
    private TextField campoLogin;

    @FXML
    private PasswordField campoSenha;

    // Método disparado quando o botão "Entrar" é clicado
    @FXML
    void clicouEntrar(ActionEvent event) {
        String login = campoLogin.getText();
        String senha = campoSenha.getText();

        if (login.isEmpty() || senha.isEmpty()) {
            mostrarAlerta("Aviso", "Preencha todos os campos!");
            return;
        }

        UsuarioDAO dao = new UsuarioDAO();
        Usuario usuarioLogado = dao.autenticar(login, senha);

        if (usuarioLogado != null) {
            try {
                // Guarda o usuário autenticado na sessão global
                SessaoUsuario.getInstancia().setUsuarioLogado(usuarioLogado);

                // --- INÍCIO DA INTERCEPTAÇÃO ---
                String telaParaAbrir = "/fxml/PainelPrincipal.fxml";
                String tituloJanela = "Sistema de Academia - Dashboard";

                // Se a senha for a padrão, desviamos a rota!
                if (usuarioLogado.getSenha().equals("123456")) {
                    telaParaAbrir = "/fxml/TrocarSenhaObrigatoria.fxml";
                    tituloJanela = "Troca de Senha Obrigatória";
                }
                // --- FIM DA INTERCEPTAÇÃO ---

                // Carrega o FXML (vai ser o Painel ou o Pedágio, dependendo do IF acima)
                javafx.scene.Parent raiz = javafx.fxml.FXMLLoader.load(getClass().getResource(telaParaAbrir));
                
                javafx.stage.Stage novoPalco = new javafx.stage.Stage();
                novoPalco.setTitle(tituloJanela);
                novoPalco.setScene(new javafx.scene.Scene(raiz));
                novoPalco.show();

                javafx.stage.Stage palcoLogin = (javafx.stage.Stage) campoLogin.getScene().getWindow();
                palcoLogin.close();

            } catch (Exception e) {
                mostrarAlerta("Erro", "Não foi possível carregar o sistema.");
                e.printStackTrace();
            }
        } else {
            mostrarAlerta("Erro de Autenticação", "CPF/Email ou senha incorretos.");
        }
    }
    
    @FXML
    void clicouEsqueciSenha(ActionEvent event) {
        String emailDigitado = campoLogin.getText();
        
        // Verifica se o usuário digitou algo antes de clicar
        if (emailDigitado.isEmpty()) {
            mostrarAlerta("Aviso", "Por favor, digite o seu Email ou CPF no campo de login antes de clicar em Esqueci a senha.");
            return;
        }

        try {
            // Carrega o FXML da nova tela
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/RecuperarSenha.fxml"));
            javafx.scene.Parent raiz = loader.load();

            // Pega o Controller da nova tela e passa o email para ele
            RecuperarSenhaController controller = loader.getController();
            controller.inicializarComEmail(emailDigitado);

            // Abre a nova janela
            javafx.stage.Stage palcoRecuperacao = new javafx.stage.Stage();
            palcoRecuperacao.setTitle("Recuperação de Senha");
            palcoRecuperacao.setScene(new javafx.scene.Scene(raiz));
            palcoRecuperacao.setResizable(false);
            palcoRecuperacao.show();

        } catch (Exception e) {
            mostrarAlerta("Erro", "Não foi possível abrir a tela de recuperação.");
            e.printStackTrace();
        }
    }

    // Método auxiliar para subir um Pop-up na tela
    private void mostrarAlerta(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}