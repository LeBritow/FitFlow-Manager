package com.mycompany.academia.core.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.academia.admin.dao.UsuarioDAO;
import com.mycompany.academia.admin.model.Usuario;
import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.treino.dao.TreinoDAO;
import com.mycompany.academia.treino.model.ItemTreino;
import com.mycompany.academia.treino.model.ProgramacaoTreino;
import com.mycompany.academia.treino.model.SerieTreino;
import com.mycompany.academia.treino.model.Treino;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ServidorMobile {

    public static void iniciar() {
        try {
            InetSocketAddress address = new InetSocketAddress(8081);
            HttpServer server = HttpServer.create(address, 0);
            
            server.createContext("/api/teste", new TesteHandler());
            server.createContext("/api/login", new LoginHandler());
            server.createContext("/api/ficha", new BuscarFichaHandler());
            server.createContext("/api/treino/finalizar", new FinalizarTreinoHandler());
            server.createContext("/api/exercicios", new ListarExerciciosHandler());
            
            server.setExecutor(null); 
            server.start();
            System.out.println("🚀 Servidor Mobile (API) rodando na porta 8081...");

        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
            // Se der erro de porta ocupada, vamos tentar matar o processo que está nela
            System.out.println("Tentando forçar liberação da porta...");
        }
    }

    // ========================================================================
    // ROTA 1: TESTE
    // ========================================================================
    static class TesteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            
            JsonObject json = new JsonObject();
            json.addProperty("status", "sucesso");
            json.addProperty("mensagem", "A API está online na porta 8081!");
            enviarResposta(exchange, 200, json.toString());
        }
    }

    // ========================================================================
    // ROTA 2: LOGIN DO APLICATIVO MOBILE
    // ========================================================================
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    JsonObject corpoRequisicao = JsonParser.parseReader(isr).getAsJsonObject();
                    
                    String loginRecebido = corpoRequisicao.get("login").getAsString();
                    String senhaRecebida = corpoRequisicao.get("senha").getAsString();

                    UsuarioDAO dao = new UsuarioDAO();
                    Usuario usuario = dao.autenticar(loginRecebido, senhaRecebida);
                    JsonObject respostaJson = new JsonObject();

                    if (usuario != null && usuario instanceof Aluno) {
                        respostaJson.addProperty("status", "sucesso");
                        respostaJson.addProperty("id", usuario.getId());
                        respostaJson.addProperty("nome", usuario.getNome());
                        enviarResposta(exchange, 200, respostaJson.toString());
                    } else {
                        respostaJson.addProperty("status", "erro");
                        respostaJson.addProperty("mensagem", "Credenciais inválidas ou usuário não é um Aluno.");
                        enviarResposta(exchange, 401, respostaJson.toString()); 
                    }
                } catch (Exception e) {
                    JsonObject erroJson = new JsonObject();
                    erroJson.addProperty("status", "erro");
                    erroJson.addProperty("mensagem", "Erro ao processar os dados: " + e.getMessage());
                    enviarResposta(exchange, 400, erroJson.toString());
                }
            } else {
                JsonObject erro = new JsonObject();
                erro.addProperty("erro", "Esta rota aceita apenas método POST.");
                enviarResposta(exchange, 405, erro.toString());
            }
        }
    }

    // ========================================================================
    // ROTA 3: BUSCAR A FICHA DE TREINO DO ALUNO (ROTA NOVA)
    // ========================================================================
    static class BuscarFichaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    if (query == null || !query.contains("alunoId=")) {
                        enviarResposta(exchange, 400, "{\"erro\":\"Falta o parametro alunoId.\"}");
                        return;
                    }
                    
                    int alunoId = Integer.parseInt(query.split("=")[1]);
                    
                    TreinoDAO dao = new TreinoDAO();
                    List<ProgramacaoTreino> progs = dao.listarProgramacoesPorAluno(alunoId);
                    
                    if (progs.isEmpty()) {
                        enviarResposta(exchange, 404, "{\"erro\":\"Nenhuma ficha ativa encontrada para este aluno.\"}");
                        return;
                    }
                    
                    ProgramacaoTreino fichaAtual = progs.get(0);
                    Treino treino = fichaAtual.getTreino();
                    List<ItemTreino> itens = dao.listarItensPorTreino(treino.getId());
                    
                    JsonObject respostaJson = new JsonObject();
                    respostaJson.addProperty("idFicha", fichaAtual.getId());
                    respostaJson.addProperty("nomeTreino", treino.getNome());
                    respostaJson.addProperty("objetivo", treino.getObjetivo());
                    
                    JsonArray arrayExercicios = new JsonArray();
                    for (ItemTreino item : itens) {
                        JsonObject objExercicio = new JsonObject();
                        objExercicio.addProperty("idItem", item.getId());
                        objExercicio.addProperty("nomeExercicio", item.getExercicio().getNome());
                        objExercicio.addProperty("descanso", item.getIntervaloDescanso());
                        
                        JsonArray arraySeries = new JsonArray();
                        for (SerieTreino s : item.getSeriesTreino()) {
                            JsonObject objSerie = new JsonObject();
                            objSerie.addProperty("serie", s.getNumeroDaSerie());
                            objSerie.addProperty("reps", s.getRepeticoes());
                            objSerie.addProperty("carga", s.getCarga());
                            arraySeries.add(objSerie);
                        }
                        objExercicio.add("series", arraySeries);
                        arrayExercicios.add(objExercicio);
                    }
                    
                    respostaJson.add("exercicios", arrayExercicios);
                    enviarResposta(exchange, 200, respostaJson.toString());

                } catch (Exception e) {
                    enviarResposta(exchange, 500, "{\"erro\":\"Falha ao processar os dados: " + e.getMessage() + "\"}");
                }
            } else {
                enviarResposta(exchange, 405, "{\"erro\":\"Esta rota aceita apenas o metodo GET.\"}");
            }
        }
    }
    
    // ========================================================================
    // ROTA 4: FINALIZAR TREINO E SALVAR COMENTÁRIO
    // ========================================================================
    static class FinalizarTreinoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    JsonObject corpoRequisicao = JsonParser.parseReader(isr).getAsJsonObject();
                    
                    int alunoId = corpoRequisicao.get("alunoId").getAsInt();
                    int treinoId = corpoRequisicao.get("treinoId").getAsInt();
                    String textoComentario = corpoRequisicao.has("comentario") ? corpoRequisicao.get("comentario").getAsString() : "Treino concluído sem comentários.";

                    // Busca as entidades no banco
                    Aluno aluno = em.find(Aluno.class, alunoId);
                    Treino treino = em.find(Treino.class, treinoId);

                    if (aluno == null || treino == null) {
                        enviarResposta(exchange, 404, "{\"status\":\"erro\",\"mensagem\":\"Aluno ou Treino não encontrados.\"}");
                        return;
                    }

                    // Cria e salva o comentário (que serve como registro de conclusão)
                    com.mycompany.academia.treino.model.ComentarioTreino novoComentario = new com.mycompany.academia.treino.model.ComentarioTreino();
                    novoComentario.setAluno(aluno);
                    novoComentario.setTreino(treino);
                    novoComentario.setTexto(textoComentario);
                    novoComentario.setDataCriacao(java.time.LocalDateTime.now());
                    novoComentario.setLido(false);

                    em.getTransaction().begin();
                    em.persist(novoComentario);
                    em.getTransaction().commit();

                    JsonObject respostaJson = new JsonObject();
                    respostaJson.addProperty("status", "sucesso");
                    respostaJson.addProperty("mensagem", "Treino finalizado e salvo com sucesso!");
                    enviarResposta(exchange, 200, respostaJson.toString());

                } catch (Exception e) {
                    if (em.getTransaction().isActive()) em.getTransaction().rollback();
                    enviarResposta(exchange, 500, "{\"status\":\"erro\",\"mensagem\":\"Erro ao salvar o treino: " + e.getMessage() + "\"}");
                } finally {
                    em.close();
                }
            } else {
                enviarResposta(exchange, 405, "{\"erro\":\"Esta rota aceita apenas POST.\"}");
            }
        }
    }
    
    static class ListarExerciciosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    // Instancia o DAO de exercícios (assumindo que você tem o ExercicioDAO)
                    com.mycompany.academia.treino.dao.ExercicioDAO dao = new com.mycompany.academia.treino.dao.ExercicioDAO();
                    List<com.mycompany.academia.treino.model.Exercicio> lista = dao.listarTodos(); // Certifique-se que este método existe no seu DAO
                    
                    JsonArray jsonArray = new JsonArray();
                    for (com.mycompany.academia.treino.model.Exercicio e : lista) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", e.getId());
                        obj.addProperty("nome", e.getNome());
                        obj.addProperty("grupo", e.getGrupoMuscular());
                        jsonArray.add(obj);
                    }
                    
                    enviarResposta(exchange, 200, jsonArray.toString());
                } catch (Exception e) {
                    enviarResposta(exchange, 500, "{\"erro\":\"Erro ao buscar exercícios.\"}");
                }
            }
        }
    }

    // ========================================================================
    // FUNÇÃO AUXILIAR PARA ENVIAR A RESPOSTA RAPIDAMENTE
    // ========================================================================
    private static void enviarResposta(HttpExchange exchange, int statusCode, String resposta) throws IOException {
        byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}