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

    // ========================================================================
    // GATILHO DE FECHAMENTO COMPLETO
    // Acionado no exato milissegundo em que o usuário clica no "X" da janela
    // ========================================================================
    @Override
    public void stop() throws Exception {
        System.out.println("👋 Janela principal fechada. Encerrando processos...");
        
        // 1. Desliga o servidor HTTP nativo
        com.mycompany.academia.core.config.ServidorMobile.parar();
        
        // 2. Garante o encerramento do processo java.exe no Windows/Linux
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}