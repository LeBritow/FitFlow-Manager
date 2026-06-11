package com.mycompany.academia.core.ui;

import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.admin.model.Usuario;
import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.session.SessaoUsuario;
import com.mycompany.academia.admin.dao.UsuarioDAO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Base64;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML private TextField campoLogin;
    @FXML private PasswordField campoSenha;
    
    @FXML private Button btnEntrar;
    @FXML private Hyperlink linkEsqueciSenha;
    @FXML private VBox vboxLoading;
    @FXML private ProgressIndicator progressCarregando;
    @FXML private Label labelStatus;
    
    @FXML private CheckBox checkManterLogin;

    @FXML
    public void initialize() {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        String usuarioSalvo = prefs.get("usuario", null);
        String senhaSalvaBase64 = prefs.get("senha", null);

        if (usuarioSalvo != null && senhaSalvaBase64 != null) {
            campoLogin.setText(usuarioSalvo);
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(senhaSalvaBase64);
                campoSenha.setText(new String(decodedBytes));
                
                checkManterLogin.setSelected(true);
            } catch (Exception e) {
            }
        }
    }

    @FXML
    void clicouEntrar(ActionEvent event) {
        String login = campoLogin.getText();
        String senha = campoSenha.getText();

        if (login.isEmpty() || senha.isEmpty()) {
            mostrarAlerta("Aviso", "Preencha todos os campos!");
            return;
        }

        btnEntrar.setVisible(false);
        btnEntrar.setManaged(false);
        linkEsqueciSenha.setVisible(false);
        linkEsqueciSenha.setManaged(false);
        checkManterLogin.setVisible(false);
        checkManterLogin.setManaged(false);
        
        vboxLoading.setVisible(true);
        vboxLoading.setManaged(true);
        labelStatus.setText("Conectando ao banco de dados...");

        new Thread(() -> {
            try {
                Platform.runLater(() -> labelStatus.setText("Verificando credenciais..."));

                EventBus.emit("Desktop", "LoginController.autenticar", "login=" + login);
                UsuarioDAO dao = new UsuarioDAO();
                Usuario usuarioLogado = dao.autenticar(login, senha);

                Platform.runLater(() -> {
                    if (usuarioLogado != null) {
                        if (usuarioLogado instanceof Aluno) {
                            restaurarTelaLogin();
                            mostrarAlerta("Acesso Negado", "Alunos devem acessar pelo aplicativo mobile. O sistema web não está disponível para este perfil.");
                            return;
                        }

                        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
                        if (checkManterLogin.isSelected()) {
                            prefs.put("usuario", login);
                            String senhaBase64 = Base64.getEncoder().encodeToString(senha.getBytes());
                            prefs.put("senha", senhaBase64);
                        } else {
                            prefs.remove("usuario");
                            prefs.remove("senha");
                        }

                        labelStatus.setText("Acesso liberado! Iniciando...");
                        abrirTelaPrincipal(usuarioLogado);
                    } else {
                        restaurarTelaLogin();
                        mostrarAlerta("Erro de Autenticação", "CPF/Email ou senha incorretos.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    restaurarTelaLogin();
                    mostrarAlerta("Erro", "Não foi possível conectar ao servidor.");
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void abrirTelaPrincipal(Usuario usuarioLogado) {
        try {
            SessaoUsuario.getInstancia().setUsuarioLogado(usuarioLogado);

            String telaParaAbrir = "/fxml/PainelPrincipal.fxml";
            String tituloJanela = "Sistema de Academia - Dashboard";

            if (usuarioLogado.getSenha().equals("123456")) {
                telaParaAbrir = "/fxml/TrocarSenhaObrigatoria.fxml";
                tituloJanela = "Troca de Senha Obrigatória";
            }

            javafx.scene.Parent raiz = javafx.fxml.FXMLLoader.load(getClass().getResource(telaParaAbrir));
            
            javafx.stage.Stage novoPalco = new javafx.stage.Stage();
            novoPalco.setTitle(tituloJanela);
            novoPalco.setScene(new javafx.scene.Scene(raiz));
            novoPalco.show();

            javafx.stage.Stage palcoLogin = (javafx.stage.Stage) campoLogin.getScene().getWindow();
            palcoLogin.close();

        } catch (Exception e) {
            restaurarTelaLogin();
            mostrarAlerta("Erro", "Não foi possível carregar o sistema.");
            e.printStackTrace();
        }
    }

    private void restaurarTelaLogin() {
        vboxLoading.setVisible(false);
        vboxLoading.setManaged(false);
        
        btnEntrar.setVisible(true);
        btnEntrar.setManaged(true);
        linkEsqueciSenha.setVisible(true);
        linkEsqueciSenha.setManaged(true);
        checkManterLogin.setVisible(true);
        checkManterLogin.setManaged(true);
    }

    @FXML
    void clicouEsqueciSenha(ActionEvent event) {
        String emailDigitado = campoLogin.getText();
        
        if (emailDigitado.isEmpty()) {
            mostrarAlerta("Aviso", "Por favor, digite o seu Email ou CPF no campo de login antes de clicar em Esqueci a senha.");
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/RecuperarSenha.fxml"));
            javafx.scene.Parent raiz = loader.load();

            RecuperarSenhaController controller = loader.getController();
            controller.inicializarComEmail(emailDigitado);

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

    private void mostrarAlerta(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}