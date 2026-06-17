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

        Usuario oUsuarioAtual = SessaoUsuario.getInstancia().getUsuarioLogado();
        
        UsuarioDAO oDao = new UsuarioDAO();
        boolean sucesso = oDao.atualizarSenhaPorEmail(oUsuarioAtual.getEmail(), novaSenha);

        if (sucesso) {
            oUsuarioAtual.setSenha(novaSenha);
            
            try {
                javafx.scene.Parent oRaiz = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/PainelPrincipal.fxml"));
                Stage oNovoPalco = new Stage();
                oNovoPalco.setTitle("Sistema de Academia - Dashboard");
                oNovoPalco.setScene(new javafx.scene.Scene(oRaiz));
                oNovoPalco.show();

                Stage oPalcoAtual = (Stage) campoNovaSenha.getScene().getWindow();
                oPalcoAtual.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mostrarAlerta("Erro", "Falha ao salvar a nova senha no banco de dados.");
        }
    }

    private void mostrarAlerta(String titulo, String mensagem) {
        Alert oAlert = new Alert(Alert.AlertType.INFORMATION);
        oAlert.setTitle(titulo);
        oAlert.setHeaderText(null);
        oAlert.setContentText(mensagem);
        oAlert.showAndWait();
    }
}