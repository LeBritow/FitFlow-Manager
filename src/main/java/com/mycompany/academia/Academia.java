package com.mycompany.academia;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Academia extends Application {

    @Override
    public void start(Stage palcoPrincipal) throws Exception {
        // Carrega o arquivo XML da tela de Login
        Parent raiz = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
        
        Scene cena = new Scene(raiz);
        
        palcoPrincipal.setTitle("Sistema de Academias - Login");
        palcoPrincipal.setScene(cena);
        palcoPrincipal.setResizable(false); // Impede de redimensionar a tela
        palcoPrincipal.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}