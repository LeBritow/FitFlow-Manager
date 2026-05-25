package com.mycompany.academia.core.ui;

import com.mycompany.academia.admin.model.Admin;
import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.admin.model.Instrutor;
import com.mycompany.academia.admin.model.Usuario;
import com.mycompany.academia.core.session.SessaoUsuario;
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
            labelNomeUser.setText(usuario.getNome().split(" ")[0]);
            
            if (usuario instanceof Admin) {
                labelTipoUser.setText("Administrador");
            } else if (usuario instanceof Instrutor) {
                labelTipoUser.setText("Instrutor");
            } else if (usuario instanceof Aluno) {
                labelTipoUser.setText("Aluno (Acesso Mobile)");
                btnUsuarios.setVisible(false);
                btnUsuarios.setManaged(false);
            }
        }
    }

    private void carregarTelaCentro(String arquivoFxml) {
        try {
            Parent novaTela = FXMLLoader.load(getClass().getResource("/fxml/" + arquivoFxml));
            areaConteudo.getChildren().clear();
            areaConteudo.getChildren().add(novaTela);
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
        carregarTelaCentro("Usuarios.fxml");
    }

    // NOVO MÉTODO ADICIONADO AQUI SEGUINDO O SEU PADRÃO
    @FXML
    void abrirAnaliseAluno(ActionEvent event) {
        carregarTelaCentro("AnaliseAluno.fxml");
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
        SessaoUsuario.getInstancia().encerrarSessao();
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage palco = new Stage();
            palco.setTitle("Sistema de Academias - Login");
            palco.setScene(new Scene(login));
            palco.setResizable(false);
            palco.show();
            
            Stage palcoAtual = (Stage) btnUsuarios.getScene().getWindow();
            palcoAtual.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}