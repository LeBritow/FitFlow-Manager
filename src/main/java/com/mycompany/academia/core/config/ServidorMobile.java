package com.mycompany.academia.core.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.academia.admin.dao.UsuarioDAO;
import com.mycompany.academia.admin.model.Usuario;
import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.treino.dao.ExercicioDAO;
import com.mycompany.academia.treino.dao.TreinoDAO;
import com.mycompany.academia.treino.model.ComentarioTreino;
import com.mycompany.academia.treino.model.Exercicio;
import com.mycompany.academia.treino.model.ItemTreino;
import com.mycompany.academia.treino.model.ProgramacaoTreino;
import com.mycompany.academia.treino.model.SerieTreino;
import com.mycompany.academia.treino.model.Treino;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

public class ServidorMobile {

    // A variável que guarda o nosso servidor para podermos "matá-lo" quando o JavaFX fechar
    private static HttpServer servidorAtual;

    public static void iniciar() {
        try {
            servidorAtual = HttpServer.create(new InetSocketAddress(8081), 0);
            
            // Registrando as 5 rotas da nossa API
            servidorAtual.createContext("/api/teste", new TesteHandler());
            servidorAtual.createContext("/api/login", new LoginHandler());
            servidorAtual.createContext("/api/ficha", new BuscarFichaHandler());
            servidorAtual.createContext("/api/treino/finalizar", new FinalizarTreinoHandler());
            servidorAtual.createContext("/api/exercicios", new ListarExerciciosHandler());
            
            servidorAtual.setExecutor(null); 
            servidorAtual.start();
            System.out.println("🚀 Servidor Mobile (API) rodando na porta 8081...");

        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor mobile: " + e.getMessage());
        }
    }

    // ========================================================================
    // MÉTODO DE DESLIGAMENTO (Chamado lá no seu App.java)
    // ========================================================================
    public static void parar() {
        if (servidorAtual != null) {
            System.out.println("🛑 Forçando a parada do Servidor Mobile e liberando a porta 8081...");
            servidorAtual.stop(0); // O zero significa desligamento imediato
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
    // ROTA 3: BUSCAR A FICHA DE TREINO DO ALUNO
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
                    respostaJson.addProperty("idFicha", treino.getId()); // Envia o ID real do Treino
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
    // ROTA 4: FINALIZAR TREINO (SALVANDO SESSÃO, CARGAS E COMENTÁRIO)
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
                EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    JsonObject corpoRequisicao = JsonParser.parseReader(isr).getAsJsonObject();
                    
                    int alunoId = corpoRequisicao.get("alunoId").getAsInt();
                    int treinoId = corpoRequisicao.get("treinoId").getAsInt();
                    String textoComentario = corpoRequisicao.has("comentario") ? corpoRequisicao.get("comentario").getAsString() : "Treino concluído sem comentários.";

                    Aluno aluno = em.find(Aluno.class, alunoId);
                    Treino treino = em.find(Treino.class, treinoId);

                    if (aluno == null || treino == null) {
                        enviarResposta(exchange, 404, "{\"status\":\"erro\",\"mensagem\":\"Aluno ou Treino não encontrados.\"}");
                        return;
                    }

                    em.getTransaction().begin();

                    // 1. Encontrar a ProgramacaoTreino atual do aluno
                    String jpqlProg = "SELECT p FROM ProgramacaoTreino p WHERE p.aluno.id = :aId AND p.treino.id = :tId";
                    List<ProgramacaoTreino> progs = em.createQuery(jpqlProg, ProgramacaoTreino.class)
                                                      .setParameter("aId", alunoId)
                                                      .setParameter("tId", treinoId)
                                                      .getResultList();

                    if (!progs.isEmpty()) {
                        ProgramacaoTreino prog = progs.get(0);
                        
                        // 2. Salvar a Sessão de Treino
                        com.mycompany.academia.core.session.SessaoTreino sessao = new com.mycompany.academia.core.session.SessaoTreino();
                        sessao.setProgramacaoTreino(prog);
                        sessao.setData(LocalDateTime.now());
                        sessao.setConcluido(true);
                        em.persist(sessao);

                        // 3. Varrer o JSON para salvar os Itens Realizados (Cargas)
                        if (corpoRequisicao.has("itensRealizados")) {
                            JsonArray itensArray = corpoRequisicao.get("itensRealizados").getAsJsonArray();
                            for (int i = 0; i < itensArray.size(); i++) {
                                JsonObject objItem = itensArray.get(i).getAsJsonObject();
                                int idItemTreino = objItem.get("itemTreinoId").getAsInt();
                                float cargaReal = objItem.get("carga").getAsFloat();
                                boolean feito = objItem.get("feito").getAsBoolean();
                                
                                int tempoExec = objItem.has("tempoExecucao") ? objItem.get("tempoExecucao").getAsInt() : 0;
                                int tempoDesc = objItem.has("tempoDescanso") ? objItem.get("tempoDescanso").getAsInt() : 0;
                                String statusCg = objItem.has("statusCarga") ? objItem.get("statusCarga").getAsString() : "MANTEVE";

                                ItemTreino itemTreinoDB = em.find(ItemTreino.class, idItemTreino);
                                if (itemTreinoDB != null) {
                                    com.mycompany.academia.treino.model.ItemRealizado itemRealizado = new com.mycompany.academia.treino.model.ItemRealizado();
                                    itemRealizado.setSessaoTreino(sessao);
                                    itemRealizado.setItemTreino(itemTreinoDB);
                                    itemRealizado.setCargaUtilizada(cargaReal);
                                    itemRealizado.setFeito(feito);

                                    itemRealizado.setTempoExecucaoSegundos(tempoExec);
                                    itemRealizado.setTempoDescansoSegundos(tempoDesc);
                                    itemRealizado.setStatusCarga(statusCg);

                                    em.persist(itemRealizado);
                                }
                            }
                        }
                    }

                    // 4. Salvar o Feedback/Comentário
                    ComentarioTreino novoComentario = new ComentarioTreino();
                    novoComentario.setAluno(aluno);
                    novoComentario.setTreino(treino);
                    novoComentario.setTexto(textoComentario);
                    novoComentario.setDataCriacao(LocalDateTime.now());
                    novoComentario.setLido(false); 
                    em.persist(novoComentario);

                    em.getTransaction().commit();

                    JsonObject respostaJson = new JsonObject();
                    respostaJson.addProperty("status", "sucesso");
                    respostaJson.addProperty("mensagem", "Treino finalizado com histórico de cargas salvo!");
                    enviarResposta(exchange, 200, respostaJson.toString());

                } catch (Exception e) {
                    if (em.getTransaction().isActive()) em.getTransaction().rollback();
                    enviarResposta(exchange, 500, "{\"status\":\"erro\",\"mensagem\":\"Erro ao salvar o treino: " + e.getMessage() + "\"}");
                    e.printStackTrace(); // Bom manter aqui no log do NetBeans se der erro
                } finally {
                    em.close();
                }
            } else {
                enviarResposta(exchange, 405, "{\"erro\":\"Esta rota aceita apenas POST.\"}");
            }
        }
    }

    // ========================================================================
    // ROTA 5: LISTAR TODOS OS EXERCÍCIOS
    // ========================================================================
    static class ListarExerciciosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    ExercicioDAO dao = new ExercicioDAO();
                    List<Exercicio> lista = dao.listarTodos(); 
                    
                    JsonArray jsonArray = new JsonArray();
                    for (Exercicio e : lista) {
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
            } else {
                enviarResposta(exchange, 405, "{\"erro\":\"Esta rota aceita apenas GET.\"}");
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