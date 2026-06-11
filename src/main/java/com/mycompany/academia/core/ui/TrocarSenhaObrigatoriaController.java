package com.mycompany.academia.core.ui;

import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.session.SessaoUsuario;
import com.mycompany.academia.admin.dao.UsuarioDAO;
import com.mycompany.academia.admin.model.Usuario;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class TrocarSenhaObrigatoriaController {

    @FXML private PasswordField campoNovaSenha;
    @FXML private PasswordField campoConfirmaSenha;

    @FXML
    void salvarEContinuar(ActionEvent event) {
        EventBus.emit("Desktop", "TrocarSenhaController.salvarEContinuar", "Trocando senha obrigatória");
        String novaSenha = campoNovaSenha.getText();
        String confirmaSenha = campoConfirmaSenha.getText();

        if (novaSenha.isEmpty() || confirmaSenha.isEmpty()) {
            mostrarAlerta("Aviso", "Preencha os dois campos.");
            return;
        }

        if (!novaSenha.equals(confirmaSenha)) {
            mostrarAlerta("Erro", "As senhas não coincidem. Digite novamente.");
            return;
        }

        if (novaSenha.equals("123456")) {
            mostrarAlerta("Aviso", "A nova senha não pode ser igual à senha padrão.");
            return;
        }

        Usuario usuarioAtual = SessaoUsuario.getInstancia().getUsuarioLogado();
        
        UsuarioDAO dao = new UsuarioDAO();
        boolean sucesso = dao.atualizarSenhaPorEmail(usuarioAtual.getEmail(), novaSenha);

        if (sucesso) {
            usuarioAtual.setSenha(novaSenha);
            
            try {
                javafx.scene.Parent raiz = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/PainelPrincipal.fxml"));
                Stage novoPalco = new Stage();
                novoPalco.setTitle("Sistema de Academia - Dashboard");
                novoPalco.setScene(new javafx.scene.Scene(raiz));
                novoPalco.show();

                Stage palcoAtual = (Stage) campoNovaSenha.getScene().getWindow();
                palcoAtual.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mostrarAlerta("Erro", "Falha ao salvar a nova senha no banco de dados.");
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