package com.mycompany.academia.core.config;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class ServidorMobile {

    public static void iniciar() throws IOException {
        // Cria um servidor na porta 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Exemplo de rota: quando o celular mandar algo para /treino/finalizar
        server.createContext("/treino/finalizar", new FinalizarTreinoHandler());
        
        server.setExecutor(null); // usa o executor padrão
        server.start();
        System.out.println("Servidor Mobile rodando na porta 8080...");
    }

    static class FinalizarTreinoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String resposta = "Treino finalizado com sucesso!";
            t.sendResponseHeaders(200, resposta.length());
            OutputStream os = t.getResponseBody();
            os.write(resposta.getBytes());
            os.close();
        }
    }
}