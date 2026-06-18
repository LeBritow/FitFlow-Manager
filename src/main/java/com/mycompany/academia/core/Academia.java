package com.mycompany.academia.core;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Academia extends Application {

  @Override
  public void start(Stage palcoPrincipal) throws Exception {
    Parent raiz = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
    
    Scene cena = new Scene(raiz);
    
    palcoPrincipal.setTitle("Sistema de Academias - Login");
    palcoPrincipal.setScene(cena);
    palcoPrincipal.setResizable(false);
    palcoPrincipal.show();
  }

  @Override
  public void stop() throws Exception {
    System.out.println("Janela principal fechada. Encerrando processos...");
    
    com.mycompany.academia.core.config.ServidorMobile.parar();
    
    System.exit(0);
  }

  public static void main(String[] args) {
    launch(args);
  }
}