package com.mycompany.academia.core.ui;

import com.mycompany.academia.admin.model.Admin;
import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.admin.model.Instrutor;
import com.mycompany.academia.admin.model.Usuario;
import com.mycompany.academia.core.config.EventBus;
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
    EventBus.emit("Desktop", "PainelPrincipalController.carregarDash", "Painel principal carregado");
    Usuario oUsuario = SessaoUsuario.getInstancia().getUsuarioLogado();
    
    if (oUsuario != null) {
      labelNomeUser.setText(oUsuario.getNome().split(" ")[0]);
      
      if (oUsuario instanceof Admin) {
        labelTipoUser.setText("Administrador");
      } else if (oUsuario instanceof Instrutor) {
        labelTipoUser.setText("Instrutor");
      } else if (oUsuario instanceof Aluno) {
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
      Parent oNovaTela = FXMLLoader.load(getClass().getResource("/fxml/" + arquivoFxml));
      areaConteudo.getChildren().clear();
      areaConteudo.getChildren().add(oNovaTela);
    } catch (IOException e) {
      System.err.println("Erro ao tentar carregar a tela: " + arquivoFxml);
      e.printStackTrace();
    }
  }

  @FXML
  void abrirInicio(ActionEvent event) {
    try {
      FXMLLoader oLoader = new FXMLLoader(getClass().getResource("/fxml/DashboardInicio.fxml"));
      Parent oDashboard = oLoader.load();
      DashboardInicioController oController = oLoader.getController();
      oController.setOnNavegar((String destino) -> {
        switch (destino) {
          case "usuarios" -> abrirUsuarios(event);
          case "exercicios" -> abrirExercicios(event);
          case "fichas" -> abrirFichas(event);
          case "analise" -> abrirAnaliseAluno(event);
        }
      });
      areaConteudo.getChildren().clear();
      areaConteudo.getChildren().add(oDashboard);
    } catch (IOException e) {
      System.err.println("Erro ao carregar dashboard: " + e.getMessage());
    }
  }

  @FXML
  void abrirUsuarios(ActionEvent event) {
    carregarTelaCentro("Usuarios.fxml");
  }

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
      Parent oLogin = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
      Stage oPalco = new Stage();
      oPalco.setTitle("Sistema de Academias - Login");
      oPalco.setScene(new Scene(oLogin));
      oPalco.setResizable(false);
      oPalco.show();
      
      Stage oPalcoAtual = (Stage) btnUsuarios.getScene().getWindow();
      oPalcoAtual.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}