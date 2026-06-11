package com.mycompany.academia.core.ui;

import com.mycompany.academia.admin.model.Admin;
import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.admin.model.Instrutor;
import com.mycompany.academia.admin.model.Usuario;
import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.session.SessaoUsuario;
import java.io.IOException;
import java.util.function.Consumer;
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
        EventBus.emit("Desktop", "PainelPrincipalController.carregarDash", "Painel principal carregado");
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
        new Thread(() -> {
            com.mycompany.academia.core.config.ServidorMobile.iniciar();
        }).start();
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DashboardInicio.fxml"));
            Parent dashboard = loader.load();
            DashboardInicioController controller = loader.getController();
            controller.setOnNavegar((String destino) -> {
                switch (destino) {
                    case "usuarios" -> abrirUsuarios(event);
                    case "exercicios" -> abrirExercicios(event);
                    case "fichas" -> abrirFichas(event);
                    case "analise" -> abrirAnaliseAluno(event);
                }
            });
            areaConteudo.getChildren().clear();
            areaConteudo.getChildren().add(dashboard);
        } catch (IOException e) {
            System.err.println("Erro ao carregar dashboard: " + e.getMessage());
        }
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