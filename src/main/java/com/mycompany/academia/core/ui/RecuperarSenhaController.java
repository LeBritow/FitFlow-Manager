package com.mycompany.academia.core.ui;

import com.mycompany.academia.admin.dao.UsuarioDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RecuperarSenhaController {

    @FXML private Label labelEmailAlvo, labelCodigoGerado;
    @FXML private VBox caixaPasso1, caixaPasso2, caixaPasso3;
    @FXML private PasswordField campoNovaSenha;
    
    @FXML private TextField txtD1, txtD2, txtD3, txtD4, txtD5, txtD6;

    private String emailUsuario;
    private String codigoReal; // código gerado aleatoriamente

    @FXML
    public void initialize() {
        configurarPuloAutomatico(txtD1, txtD2);
        configurarPuloAutomatico(txtD2, txtD3);
        configurarPuloAutomatico(txtD3, txtD4);
        configurarPuloAutomatico(txtD4, txtD5);
        configurarPuloAutomatico(txtD5, txtD6);
        
        configurarPuloAutomatico(txtD6, null); 
    }

    public void inicializarComEmail(String email) {
        this.emailUsuario = email;
        this.labelEmailAlvo.setText("Código será enviado para: " + email);
    }

    @FXML
    void enviarCodigo(ActionEvent event) {
        codigoReal = String.format("%06d", new java.util.Random().nextInt(999999));
        System.out.println("Código de recuperação para " + emailUsuario + ": " + codigoReal);
        if (labelCodigoGerado != null) {
            labelCodigoGerado.setText("Código gerado: " + codigoReal);
            labelCodigoGerado.setVisible(true);
        }
        caixaPasso1.setVisible(false); caixaPasso1.setManaged(false);
        caixaPasso2.setVisible(true); caixaPasso2.setManaged(true);
    }

    @FXML
    void validarCodigo(ActionEvent event) {
        String codigoDigitado = txtD1.getText() + txtD2.getText() + txtD3.getText() + 
                                txtD4.getText() + txtD5.getText() + txtD6.getText();
        
        if (codigoDigitado.equals(codigoReal)) {
            caixaPasso2.setVisible(false); caixaPasso2.setManaged(false);
            caixaPasso3.setVisible(true); caixaPasso3.setManaged(true);
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Código inválido! Verifique o código enviado.");
        }
    }

    @FXML
    void salvarNovaSenha(ActionEvent event) {
        String novaSenha = campoNovaSenha.getText();
        
        if (novaSenha.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "A senha não pode estar vazia.");
            return;
        }

        UsuarioDAO dao = new UsuarioDAO();
        boolean sucesso = dao.atualizarSenhaPorEmail(emailUsuario, novaSenha);

        if (sucesso) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Senha alterada com sucesso! Você já pode fazer login.");
            
            Stage palco = (Stage) campoNovaSenha.getScene().getWindow();
            palco.close();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível atualizar a senha no banco de dados.");
        }
    }

    private void configurarPuloAutomatico(TextField atual, TextField proximo) {
        atual.textProperty().addListener((obs, valorAntigo, valorNovo) -> {
            if (valorNovo.length() > 1) {
                atual.setText(valorNovo.substring(0, 1));
            } else if (valorNovo.length() == 1 && proximo != null) {
                proximo.requestFocus();
            }
        });
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}