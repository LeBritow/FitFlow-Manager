package com.mycompany.academia;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class PainelPrincipalController {

    @FXML private Label labelNomeUser;
    @FXML private Label labelTipoUser;
    @FXML private Button btnUsuarios;
    @FXML private StackPane areaConteudo;

    @FXML
    public void initialize() {
        Usuario usuario = SessaoUsuario.getInstancia().getUsuarioLogado();
        
        if (usuario != null) {
            // Pega o primeiro nome para não ficar gigante no menu
            labelNomeUser.setText(usuario.getNome().split(" ")[0]);
            
            // Controle de Acesso inteligente!
            if (usuario instanceof Admin) {
                labelTipoUser.setText("Administrador");
            } else if (usuario instanceof Instrutor) {
                labelTipoUser.setText("Instrutor");
                // Como exemplo, se o Instrutor não puder ver a aba de "Gerenciar Usuários":
                // btnUsuarios.setVisible(false);
                // btnUsuarios.setManaged(false);
            } else if (usuario instanceof Aluno) {
                labelTipoUser.setText("Aluno (Acesso Mobile)");
                btnUsuarios.setVisible(false);
                btnUsuarios.setManaged(false);
            }
        }
    }

    // --- MÁGICA DA NAVEGAÇÃO DINÂMICA ---
    private void carregarTelaCentro(String arquivoFxml) {
        try {
            Parent novaTela = FXMLLoader.load(getClass().getResource("/fxml/" + arquivoFxml));
            areaConteudo.getChildren().clear(); // Limpa o meio
            areaConteudo.getChildren().add(novaTela); // Joga a tela nova lá dentro
        } catch (IOException e) {
            System.err.println("Erro ao tentar carregar a tela: " + arquivoFxml);
            e.printStackTrace();
        }
    }

    @FXML
    void abrirInicio(ActionEvent event) {
        areaConteudo.getChildren().clear();
        Label lbl = new Label("Bem-vindo ao Backoffice da Academia!");
        lbl.setStyle("-fx-font-size: 24px; -fx-text-fill: #797979;");
        areaConteudo.getChildren().add(lbl);
    }

    @FXML
    void abrirUsuarios(ActionEvent event) {
        carregarTelaCentro("Usuarios.fxml"); // Vai carregar a tela que criaremos no Passo 3
    }

    @FXML
    void abrirExercicios(ActionEvent event) {
        carregarTelaCentro("Exercicios.fxml"); 
    }

    @FXML
    void abrirFichas(ActionEvent event) {
        carregarTelaCentro("FichasTreino.fxml");
    }

    @FXML
    void sairSistema(ActionEvent event) {
        // Limpa a memória e volta pra tela de login
        SessaoUsuario.getInstancia().encerrarSessao();
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage palco = new Stage();
            palco.setTitle("Sistema de Academias - Login");
            palco.setScene(new Scene(login));
            palco.setResizable(false);
            palco.show();
            
            // Fecha o Painel Principal atual
            Stage palcoAtual = (Stage) btnUsuarios.getScene().getWindow();
            palcoAtual.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}