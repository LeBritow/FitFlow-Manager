package com.mycompany.academia.controller;

import com.mycompany.academia.model.Usuario;
import com.mycompany.academia.session.SessaoUsuario;
import com.mycompany.academia.dao.UsuarioDAO;
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

// Imports necessários para salvar as credenciais
import java.util.Base64;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML private TextField campoLogin;
    @FXML private PasswordField campoSenha;
    
    // Novos componentes injetados da tela
    @FXML private Button btnEntrar;
    @FXML private Hyperlink linkEsqueciSenha;
    @FXML private VBox vboxLoading;
    @FXML private ProgressIndicator progressCarregando;
    @FXML private Label labelStatus;
    
    // Caixinha de manter login
    @FXML private CheckBox checkManterLogin;

    // Esse método roda automaticamente quando a tela abre
    @FXML
    public void initialize() {
        // Acessa o "registro" do sistema operacional guardado para esta classe
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        String usuarioSalvo = prefs.get("usuario", null);
        String senhaSalvaBase64 = prefs.get("senha", null);

        // Se encontrou dados salvos, apenas preenche os campos e marca a caixinha
        if (usuarioSalvo != null && senhaSalvaBase64 != null) {
            campoLogin.setText(usuarioSalvo);
            try {
                // Decodifica a senha do Base64
                byte[] decodedBytes = Base64.getDecoder().decode(senhaSalvaBase64);
                campoSenha.setText(new String(decodedBytes));
                
                // Marca a caixinha de "Manter conectado" para o usuário saber que está salvo
                checkManterLogin.setSelected(true);
            } catch (Exception e) {
                // Se der erro ao decodificar, apenas ignora e deixa o usuário digitar
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

        // 1. Esconde os botões e mostra a animação de carregamento
        btnEntrar.setVisible(false);
        btnEntrar.setManaged(false);
        linkEsqueciSenha.setVisible(false);
        linkEsqueciSenha.setManaged(false);
        checkManterLogin.setVisible(false);
        checkManterLogin.setManaged(false);
        
        vboxLoading.setVisible(true);
        vboxLoading.setManaged(true);
        labelStatus.setText("Conectando ao banco de dados...");

        // 2. Abre uma esteira paralela (Thread) para o banco não travar a tela
        new Thread(() -> {
            try {
                Platform.runLater(() -> labelStatus.setText("Verificando credenciais..."));

                UsuarioDAO dao = new UsuarioDAO();
                Usuario usuarioLogado = dao.autenticar(login, senha);

                // 3. Devolve a resposta para a Interface Gráfica
                Platform.runLater(() -> {
                    if (usuarioLogado != null) {
                        
                        // --- LÓGICA DE SALVAR/REMOVER PREFERÊNCIAS ---
                        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
                        if (checkManterLogin.isSelected()) {
                            prefs.put("usuario", login);
                            // Esconde a senha em Base64 antes de salvar no sistema
                            String senhaBase64 = Base64.getEncoder().encodeToString(senha.getBytes());
                            prefs.put("senha", senhaBase64);
                        } else {
                            // Se ele desmarcou a caixa, limpamos os dados salvos
                            prefs.remove("usuario");
                            prefs.remove("senha");
                        }
                        // ---------------------------------------------

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

    // Método extraído para manter o código organizado
    private void abrirTelaPrincipal(Usuario usuarioLogado) {
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

    // Traz o botão e o link de volta se o usuário errar a senha
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