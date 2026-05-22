package com.mycompany.academia.controller;

import com.mycompany.academia.dao.UsuarioDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RecuperarSenhaController {

    @FXML private Label labelEmailAlvo;
    @FXML private VBox caixaPasso1, caixaPasso2, caixaPasso3;
    @FXML private PasswordField campoNovaSenha;
    
    // As 6 caixinhas de código
    @FXML private TextField txtD1, txtD2, txtD3, txtD4, txtD5, txtD6;

    private String emailUsuario;

    @FXML
    public void initialize() {
        // Configura o pulo automático entre as caixinhas
        configurarPuloAutomatico(txtD1, txtD2);
        configurarPuloAutomatico(txtD2, txtD3);
        configurarPuloAutomatico(txtD3, txtD4);
        configurarPuloAutomatico(txtD4, txtD5);
        configurarPuloAutomatico(txtD5, txtD6);
        
        // A última caixa não pula pra lugar nenhum, só limita a 1 caractere
        configurarPuloAutomatico(txtD6, null); 
    }

    public void inicializarComEmail(String email) {
        this.emailUsuario = email;
        this.labelEmailAlvo.setText("Código será enviado para: " + email);
    }

    @FXML
    void enviarCodigo(ActionEvent event) {
        System.out.println("Simulando envio de e-mail para: " + emailUsuario);
        caixaPasso1.setVisible(false); caixaPasso1.setManaged(false);
        caixaPasso2.setVisible(true); caixaPasso2.setManaged(true);
    }

    @FXML
    void validarCodigo(ActionEvent event) {
        // Junta o texto das 6 caixinhas para formar o código completo
        String codigoDigitado = txtD1.getText() + txtD2.getText() + txtD3.getText() + 
                                txtD4.getText() + txtD5.getText() + txtD6.getText();
        
        if (codigoDigitado.equals("123456")) {
            caixaPasso2.setVisible(false); caixaPasso2.setManaged(false);
            caixaPasso3.setVisible(true); caixaPasso3.setManaged(true);
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Código inválido! Tente 123456.");
        }
    }

    @FXML
    void salvarNovaSenha(ActionEvent event) {
        String novaSenha = campoNovaSenha.getText();
        
        if (novaSenha.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "A senha não pode estar vazia.");
            return;
        }

        // Chama o banco de dados para salvar de verdade
        UsuarioDAO dao = new UsuarioDAO();
        boolean sucesso = dao.atualizarSenhaPorEmail(emailUsuario, novaSenha);

        if (sucesso) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Senha alterada com sucesso! Você já pode fazer login.");
            
            // Fecha a janela de recuperação automaticamente
            Stage palco = (Stage) campoNovaSenha.getScene().getWindow();
            palco.close();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível atualizar a senha no banco de dados.");
        }
    }

    // --- MÉTODOS AUXILIARES ---

    // Método inteligente que faz o cursor pular para a próxima caixa ao digitar 1 número
    private void configurarPuloAutomatico(TextField atual, TextField proximo) {
        atual.textProperty().addListener((obs, valorAntigo, valorNovo) -> {
            if (valorNovo.length() > 1) {
                // Impede que digitem mais de 1 número na mesma caixa
                atual.setText(valorNovo.substring(0, 1));
            } else if (valorNovo.length() == 1 && proximo != null) {
                // Se digitou 1 número, pula o foco para a próxima caixa
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