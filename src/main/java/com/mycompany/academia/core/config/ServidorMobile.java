package com.mycompany.academia.core.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.academia.admin.dao.UsuarioDAO;
import com.mycompany.academia.admin.model.Usuario;
import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.aluno.dao.AlunoDAO;
import com.mycompany.academia.aluno.model.AvaliacaoFisica;
import com.mycompany.academia.core.session.SessaoTreino;
import com.mycompany.academia.treino.dao.ExercicioDAO;
import com.mycompany.academia.treino.dao.TreinoDAO;
import com.mycompany.academia.treino.model.Exercicio;
import com.mycompany.academia.treino.model.ComentarioTreino;
import com.mycompany.academia.treino.model.ItemRealizado;
import com.mycompany.academia.treino.model.ItemTreino;
import com.mycompany.academia.treino.model.ProgramacaoTreino;
import com.mycompany.academia.treino.model.SerieTreino;
import com.mycompany.academia.treino.model.Treino;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public class ServidorMobile {

    private static HttpServer servidorAtual;
    private static final Map<String, Integer> sessoesAtivas = new HashMap<>();

    public static void iniciar() {
        try {
            servidorAtual = HttpServer.create(new InetSocketAddress(8081), 0);

            // API
            servidorAtual.createContext("/api/login", new LoginHandler());
            servidorAtual.createContext("/api/ficha", new BuscarFichaHandler());
            servidorAtual.createContext("/api/treino/finalizar", new FinalizarTreinoHandler());
            servidorAtual.createContext("/api/aluno/dashboard", new DashboardHandler());
            servidorAtual.createContext("/api/aluno/historico", new HistoricoHandler());
            servidorAtual.createContext("/api/aluno/perfil", new PerfilHandler());
            servidorAtual.createContext("/api/sse", new SSEHandler());

            servidorAtual.createContext("/", new StaticFileHandler());

            servidorAtual.setExecutor(Executors.newCachedThreadPool());
            servidorAtual.start();
            System.out.println(" Servidor Mobile rodando em http://localhost:8081");
            System.out.println(" Acesse pelo celular com o IP da máquina: http://" + getLocalIp() + ":8081");

        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor mobile: " + e.getMessage());
        }
    }

    private static String getLocalIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "192.168.x.x";
        }
    }

    public static void parar() {
        if (servidorAtual != null) {
            servidorAtual.stop(0);
            sessoesAtivas.clear();
        }
    }


    private static void cors(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }

    private static boolean options(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static void json(HttpExchange ex, int code, String body) throws IOException {
        cors(ex);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, b.length);
        ex.getResponseBody().write(b);
        ex.getResponseBody().close();
    }

    private static JsonObject bodyJson(HttpExchange ex) throws IOException {
        InputStreamReader r = new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8);
        return JsonParser.parseReader(r).getAsJsonObject();
    }

    private static Map<String, String> queryParams(String q) {
        Map<String, String> m = new HashMap<>();
        if (q == null || q.isBlank()) return m;
        for (String p : q.split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) m.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return m;
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (options(ex)) return;
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                json(ex, 405, "{\"erro\":\"Use POST.\"}"); return;
            }
            try {
                EventBus.emit("ServidorMobile", "LoginHandler", "Recebendo POST /api/login");
                JsonObject body = bodyJson(ex);
                String login = body.get("login").getAsString();
                String senha = body.get("senha").getAsString();
                EventBus.emit("ServidorMobile", "LoginHandler", "Credenciais recebidas, autenticando...");
                EventBus.emit("PostgreSQL", "SELECT FROM usuario WHERE email OR cpf = :login", "find");

                UsuarioDAO dao = new UsuarioDAO();
                Usuario oU = dao.autenticar(login, senha);

                if (oU != null && oU instanceof Aluno) {
                    String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                    sessoesAtivas.put(token, oU.getId());

                    JsonObject j = new JsonObject();
                    j.addProperty("status", "sucesso");
                    j.addProperty("token", token);
                    j.addProperty("id", oU.getId());
                    j.addProperty("nome", oU.getNome());
                    j.addProperty("email", oU.getEmail());
                    json(ex, 200, j.toString());
                } else {
                    json(ex, 401, "{\"status\":\"erro\",\"mensagem\":\"Credenciais inválidas ou perfil não autorizado.\"}");
                }
            } catch (Exception e) {
                json(ex, 400, "{\"status\":\"erro\",\"mensagem\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    static class BuscarFichaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (options(ex)) return;
            cors(ex);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                json(ex, 405, "{\"erro\":\"Use GET.\"}"); return;
            }
            try {
                EventBus.emit("ServidorMobile", "BuscarFichaHandler", "Recebendo GET /api/ficha?alunoId=...");
                Map<String, String> q = queryParams(ex.getRequestURI().getQuery());
                int alunoId = Integer.parseInt(q.getOrDefault("alunoId", "0"));
                if (alunoId == 0) { json(ex, 400, "{\"erro\":\"alunoId obrigatório.\"}"); return; }

                EventBus.emit("PostgreSQL", "SELECT FROM programacao_treino JOIN treino WHERE alunoId=" + alunoId, "BuscarFichaHandler");
                EventBus.emit("Entities", "ProgramacaoTreino+Treino loaded", "alunoId=" + alunoId);
                TreinoDAO dao = new TreinoDAO();
                EventBus.emit("ServidorMobile", "BuscarFichaHandler", "Buscando programação do aluno " + alunoId);
                List<ProgramacaoTreino> progs = dao.listarProgramacoesDoAluno(alunoId);
                if (progs.isEmpty()) { json(ex, 404, "{\"erro\":\"Nenhuma ficha ativa.\"}"); return; }

                ProgramacaoTreino oFicha = progs.get(0);
                Treino oTreino = oFicha.getTreino();
                EventBus.emit("PostgreSQL", "SELECT FROM item_treino JOIN exercicio WHERE treinoId=" + oTreino.getId(), "BuscarFichaHandler");
                EventBus.emit("Entities", "ItemTreino+Exercicio loaded", "treinoId=" + oTreino.getId());
                EventBus.emit("ServidorMobile", "BuscarFichaHandler", "Buscando itens do treino " + oTreino.getId());
                List<ItemTreino> itens = dao.listarItensDoTreino(oTreino.getId());

                JsonObject j = new JsonObject();
                j.addProperty("idProgramacao", oFicha.getId());
                j.addProperty("idFicha", oTreino.getId());
                j.addProperty("nomeTreino", oTreino.getNome());
                String oObjetivoTreino = "";
if (oTreino.getObjetivo() != null) {
    oObjetivoTreino = oTreino.getObjetivo().getNome();
}
j.addProperty("objetivo", oObjetivoTreino);

                JsonArray exs = new JsonArray();
                for (ItemTreino oItem : itens) {
                    JsonObject o = new JsonObject();
                    o.addProperty("idItem", oItem.getId());
                    Exercicio oExercicio = oItem.getExercicio();
                    o.addProperty("nomeExercicio", oExercicio.getNome());
                    o.addProperty("grupoMuscular", oExercicio.getGrupoMuscular());
                    o.addProperty("descanso", oItem.getIntervaloDescanso());
                    o.addProperty("progressaoCarga", oItem.isProgressaoCarga());
                    String urlMidia = oExercicio.getUrlMidia();
                    if (urlMidia == null || urlMidia.isEmpty()) {
                        String gifUrl = new GifSearchService().buscarMelhorGif(oExercicio.getNome(), oExercicio.getGrupoMuscular());
                        if (gifUrl != null) {
                            oExercicio.setUrlMidia(gifUrl);
                            new ExercicioDAO().inserir(oExercicio);
                            urlMidia = gifUrl;
                        }
                    }
                    String oUrlMidiaFinal = "";
if (urlMidia != null && !urlMidia.isEmpty()) {
    oUrlMidiaFinal = urlMidia;
}
o.addProperty("urlMidia", oUrlMidiaFinal);

                    JsonArray ss = new JsonArray();
                    for (SerieTreino oS : oItem.getSeriesTreino()) {
                        JsonObject so = new JsonObject();
                        so.addProperty("serie", oS.getNumeroDaSerie());
                        so.addProperty("reps", oS.getRepeticoes());
                        so.addProperty("carga", oS.getCarga());
                        ss.add(so);
                    }
                    o.add("series", ss);
                    exs.add(o);
                }
                j.add("exercicios", exs);
                EventBus.emit("ServidorMobile", "BuscarFichaHandler", "Retornando " + exs.size() + " exercícios para o aluno " + alunoId);
                json(ex, 200, j.toString());

            } catch (Exception e) {
                json(ex, 500, "{\"erro\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    static class FinalizarTreinoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (options(ex)) return;
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                json(ex, 405, "{\"erro\":\"Use POST.\"}"); return;
            }
            EventBus.emit("ServidorMobile", "POST /api/treino/finalizar", "Recebendo requisição de finalização");
            EntityManager em = JPAUtil.getEntityManager();
            try {
                JsonObject body = bodyJson(ex);
                int alunoId = body.get("alunoId").getAsInt();
                int treinoId = body.get("treinoId").getAsInt();
                String comentario = "Treino concluído.";
if (body.has("comentario")) {
    comentario = body.get("comentario").getAsString();
}

                EventBus.emit("JPA", "EntityManager.find(Aluno)", "alunoId=" + alunoId);
                EventBus.emit("PostgreSQL", "SELECT FROM aluno WHERE id=" + alunoId, "find");
                EventBus.emit("Entities", "Aluno loaded", "alunoId=" + alunoId);
                Aluno oAluno = em.find(Aluno.class, alunoId);
                EventBus.emit("JPA", "EntityManager.find(Treino)", "treinoId=" + treinoId);
                EventBus.emit("PostgreSQL", "SELECT FROM treino WHERE id=" + treinoId, "find");
                EventBus.emit("Entities", "Treino loaded", "treinoId=" + treinoId);
                Treino oTreino = em.find(Treino.class, treinoId);
                if (oAluno == null || oTreino == null) {
                    json(ex, 404, "{\"status\":\"erro\",\"mensagem\":\"Aluno ou Treino não encontrados.\"}");
                    return;
                }

                em.getTransaction().begin();
                EventBus.emit("JPA", "JPQL Query", "Buscando ProgramacaoTreino por alunoId=" + alunoId + ", treinoId=" + treinoId);
                EventBus.emit("PostgreSQL", "SELECT FROM programacao_treino WHERE alunoId=" + alunoId, "find");
                EventBus.emit("Entities", "ProgramacaoTreino loaded", "alunoId=" + alunoId);

                List<ProgramacaoTreino> progs = em.createQuery(
                    "SELECT p FROM ProgramacaoTreino p WHERE p.aluno.id = :a AND p.treino.id = :t", ProgramacaoTreino.class)
                    .setParameter("a", alunoId).setParameter("t", treinoId).getResultList();

                if (!progs.isEmpty()) {
                    ProgramacaoTreino oProg = progs.get(0);
                    EventBus.emit("JPA", "EntityManager.persist(SessaoTreino)", "Criando sessão de treino");
                    EventBus.emit("PostgreSQL", "INSERT INTO sessao_treino (alunoId=" + alunoId + ", treinoId=" + treinoId + ")", "persist");
                    EventBus.emit("Entities", "SessaoTreino created", "alunoId=" + alunoId);
                    SessaoTreino oSessao = new SessaoTreino();
                    oSessao.setProgramacaoTreino(oProg);
                    oSessao.setData(LocalDateTime.now());
                    oSessao.setConcluido(true);
                    em.persist(oSessao);

                    if (body.has("itensRealizados")) {
                        for (var e : body.get("itensRealizados").getAsJsonArray()) {
                            JsonObject o = e.getAsJsonObject();
                            int idItem = o.get("itemTreinoId").getAsInt();
                            float carga = o.get("carga").getAsFloat();
                            boolean feito = o.get("feito").getAsBoolean();
                            int tExec = 0;
                            if (o.has("tempoExecucao")) {
                                tExec = o.get("tempoExecucao").getAsInt();
                            }
                            int tDesc = 0;
                            if (o.has("tempoDescanso")) {
                                tDesc = o.get("tempoDescanso").getAsInt();
                            }
                            String sc = "MANTEVE";
                            if (o.has("statusCarga")) {
                                sc = o.get("statusCarga").getAsString();
                            }

                            EventBus.emit("JPA", "EntityManager.find(ItemTreino)", "itemTreinoId=" + idItem);
                            EventBus.emit("PostgreSQL", "SELECT FROM item_treino WHERE id=" + idItem, "find");
                            EventBus.emit("Entities", "ItemTreino loaded", "itemTreinoId=" + idItem);
                            ItemTreino oIt = em.find(ItemTreino.class, idItem);
                            if (oIt != null) {
                                ItemRealizado oIr = new ItemRealizado();
                                oIr.setSessaoTreino(oSessao);
                                oIr.setItemTreino(oIt);
                                oIr.setCargaUtilizada(carga);
                                oIr.setFeito(feito);
                                oIr.setTempoExecucaoSegundos(tExec);
                                oIr.setTempoDescansoSegundos(tDesc);
                                oIr.setStatusCarga(sc);
                                EventBus.emit("JPA", "EntityManager.persist(ItemRealizado)", "feito=" + feito + ", carga=" + carga);
                                EventBus.emit("PostgreSQL", "INSERT INTO item_realizado (itemTreinoId=" + idItem + ")", "persist");
                                EventBus.emit("Entities", "ItemRealizado created", "feito=" + feito);
                                em.persist(oIr);
                            }
                        }
                    }
                }

                EventBus.emit("JPA", "EntityManager.persist(ComentarioTreino)", "Registrando feedback do treino");
                EventBus.emit("PostgreSQL", "INSERT INTO comentario_treino (alunoId=" + alunoId + ")", "persist");
                EventBus.emit("Entities", "ComentarioTreino created", "alunoId=" + alunoId);
                ComentarioTreino oC = new ComentarioTreino();
                oC.setAluno(oAluno);
                oC.setTreino(oTreino);
                oC.setTexto(comentario);
                oC.setDataCriacao(LocalDateTime.now());
                oC.setLido(false);
                em.persist(oC);

                em.getTransaction().commit();
                EventBus.emit("ServidorMobile", "FinalizarTreinoHandler", "Treino finalizado com sucesso!");
                json(ex, 200, "{\"status\":\"sucesso\",\"mensagem\":\"Treino finalizado!\"}");

            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                json(ex, 500, "{\"status\":\"erro\",\"mensagem\":\"" + e.getMessage() + "\"}");
                e.printStackTrace();
            } finally {
                em.close();
            }
        }
    }

    // ─── DASHBOARD (Performance) ────────────────────────────────────────

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (options(ex)) return;
            cors(ex);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                json(ex, 405, "{\"erro\":\"Use GET.\"}"); return;
            }
            try {
                EventBus.emit("ServidorMobile", "DashboardHandler", "Recebendo GET /api/aluno/dashboard");
                int alunoId = Integer.parseInt(queryParams(ex.getRequestURI().getQuery()).getOrDefault("alunoId", "0"));
                if (alunoId == 0) { json(ex, 400, "{\"erro\":\"alunoId obrigatório.\"}"); return; }

                EventBus.emit("PostgreSQL", "SELECT COUNT FROM comentario_treino WHERE alunoId=" + alunoId, "count");
                TreinoDAO dao = new TreinoDAO();
                EventBus.emit("ServidorMobile", "DashboardHandler", "Consultando métricas do aluno " + alunoId);

                long treinosMes = dao.contarTreinosNoMes(alunoId);
                EventBus.emit("PostgreSQL", "SELECT COUNT FROM comentario_treino WHERE alunoId=" + alunoId, "count");
                long totalTreinos = dao.contarTotalTreinos(alunoId);
                EventBus.emit("PostgreSQL", "SELECT MAX(dataCriacao) FROM comentario_treino WHERE alunoId=" + alunoId, "select");
                String ultimoTreino = dao.buscarDataUltimoTreino(alunoId);
                EventBus.emit("PostgreSQL", "SELECT DISTINCT data FROM sessao_treino WHERE alunoId=" + alunoId, "select");
                long streak = dao.buscarSequenciaAtual(alunoId);

                JsonObject j = new JsonObject();
                j.addProperty("treinosMes", treinosMes);
                j.addProperty("totalTreinos", totalTreinos);
                j.addProperty("ultimoTreino", ultimoTreino);
                j.addProperty("streak", streak);
                j.addProperty("fichaAtiva", dao.buscarNomeFichaAtiva(alunoId));

                java.util.Map<Integer, Long> semanaMap = dao.listarTreinosPorSemana(alunoId, 4);
                JsonArray semanasArr = new JsonArray();
                for (java.util.Map.Entry<Integer, Long> e : semanaMap.entrySet()) {
                    JsonObject wo = new JsonObject();
                    int chave = e.getKey();
                    int ano = chave / 100;
                    int sem = chave % 100;
                    wo.addProperty("semana", "S" + sem + "/" + ano);
                    wo.addProperty("count", e.getValue());
                    semanasArr.add(wo);
                }
                j.add("treinosPorSemana", semanasArr);
                json(ex, 200, j.toString());

            } catch (Exception e) {
                json(ex, 500, "{\"erro\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    static class HistoricoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (options(ex)) return;
            cors(ex);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                json(ex, 405, "{\"erro\":\"Use GET.\"}"); return;
            }
            try {
                EventBus.emit("ServidorMobile", "HistoricoHandler", "Recebendo GET /api/aluno/historico");
                int alunoId = Integer.parseInt(queryParams(ex.getRequestURI().getQuery()).getOrDefault("alunoId", "0"));
                if (alunoId == 0) { json(ex, 400, "{\"erro\":\"alunoId obrigatório.\"}"); return; }

                EventBus.emit("PostgreSQL", "SELECT FROM comentario_treino JOIN treino WHERE alunoId=" + alunoId, "select");
                TreinoDAO dao = new TreinoDAO();
                EventBus.emit("ServidorMobile", "HistoricoHandler", "Buscando histórico do aluno " + alunoId);
                List<ComentarioTreino> comentarios = dao.listarComentariosDoAluno(alunoId);

                JsonArray arr = new JsonArray();
                for (ComentarioTreino oC : comentarios) {
                    JsonObject o = new JsonObject();
                    o.addProperty("data", oC.getDataCriacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    Treino oTreinoC = oC.getTreino();
                    o.addProperty("treinoNome", oTreinoC.getNome());
                    o.addProperty("comentario", oC.getTexto());
                    o.addProperty("lido", oC.isLido());
                    arr.add(o);
                }
                json(ex, 200, arr.toString());

            } catch (Exception e) {
                json(ex, 500, "{\"erro\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ─── PERFIL ─────────────────────────────────────────────────────────

    static class PerfilHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (options(ex)) return;
            cors(ex);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            Map<String, String> q = queryParams(ex.getRequestURI().getQuery());
            int alunoId = Integer.parseInt(q.getOrDefault("alunoId", "0"));

            if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                try {
                    EventBus.emit("ServidorMobile", "PerfilHandler", "Recebendo GET /api/aluno/perfil");
                    if (alunoId == 0) { json(ex, 400, "{\"erro\":\"alunoId obrigatório.\"}"); return; }
                    EntityManager em = JPAUtil.getEntityManager();
                    EventBus.emit("ServidorMobile", "PerfilHandler", "Buscando dados do aluno " + alunoId);
                    EventBus.emit("PostgreSQL", "SELECT FROM aluno WHERE id=" + alunoId, "select");
                    Aluno oA = em.find(Aluno.class, alunoId);
                    em.close();
                    if (oA == null) { json(ex, 404, "{\"erro\":\"Aluno não encontrado.\"}"); return; }

                    JsonObject j = new JsonObject();
                    j.addProperty("id", oA.getId());
                    j.addProperty("nome", oA.getNome());
                    j.addProperty("email", oA.getEmail());
                    j.addProperty("cpf", oA.getCpf());
                    j.addProperty("peso", oA.getPeso());
                    j.addProperty("altura", oA.getAltura());
                    j.addProperty("imc", oA.getImc());

                    AlunoDAO alunoDAO = new AlunoDAO();
                    EventBus.emit("ServidorMobile", "PerfilHandler", "Buscando avaliações físicas do aluno " + alunoId);
                    EventBus.emit("PostgreSQL", "SELECT FROM avaliacao_fisica WHERE alunoId=" + alunoId, "select");
                    List<AvaliacaoFisica> avals = alunoDAO.listarAvaliacoesDoAluno(alunoId);
                    JsonArray avArr = new JsonArray();
                    for (var av : avals) {
                        JsonObject ao = new JsonObject();
                        ao.addProperty("data", av.getDataAvaliacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        ao.addProperty("peso", av.getPeso());
                        ao.addProperty("altura", av.getAltura());
                        ao.addProperty("imc", av.getImc());
                        avArr.add(ao);
                    }
                    j.add("avaliacoes", avArr);
                    json(ex, 200, j.toString());
                } catch (Exception e) {
                    json(ex, 500, "{\"erro\":\"" + e.getMessage() + "\"}");
                }
            } else if ("PUT".equalsIgnoreCase(ex.getRequestMethod())) {
                try {
                    EventBus.emit("ServidorMobile", "PerfilHandler", "Recebendo PUT /api/aluno/perfil - atualizando dados");
                    JsonObject body = bodyJson(ex);
                    alunoId = body.get("alunoId").getAsInt();
                    float peso = body.get("peso").getAsFloat();
                    float altura = body.get("altura").getAsFloat();

                    EntityManager em = JPAUtil.getEntityManager();
                    em.getTransaction().begin();
                    EventBus.emit("ServidorMobile", "PerfilHandler", "Atualizando peso=" + peso + ", altura=" + altura + " do aluno " + alunoId);
                    EventBus.emit("PostgreSQL", "SELECT FROM aluno WHERE id=" + alunoId, "select");
                    Aluno oA = em.find(Aluno.class, alunoId);
                    if (oA != null) {
                        oA.setPeso(peso);
                        oA.setAltura(altura);
                        oA.setImc(peso / (altura * altura));
                        EventBus.emit("PostgreSQL", "UPDATE aluno SET peso=" + peso + " WHERE id=" + alunoId, "merge");
                        em.merge(oA);
                    }
                    em.getTransaction().commit();
                    em.close();
                    json(ex, 200, "{\"status\":\"sucesso\",\"mensagem\":\"Perfil atualizado.\"}");
                } catch (Exception e) {
                    json(ex, 500, "{\"erro\":\"" + e.getMessage() + "\"}");
                }
            } else {
                json(ex, 405, "{\"erro\":\"Use GET ou PUT.\"}");
            }
        }
    }


    static class SSEHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            cors(ex);
            ex.getResponseHeaders().add("Content-Type", "text/event-stream; charset=UTF-8");
            ex.getResponseHeaders().add("Cache-Control", "no-cache");
            ex.getResponseHeaders().add("Connection", "keep-alive");
            ex.sendResponseHeaders(200, 0);

            OutputStream out = ex.getResponseBody();

            JsonObject connMsg = new JsonObject();
            connMsg.addProperty("type", "connected");
            connMsg.addProperty("message", "Conectado ao monitor de eventos!");
            out.write(("data: " + connMsg.toString() + "\n\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();

            EventBus.Listener listener = event -> {
                try {
                    JsonObject j = new JsonObject();
                    j.addProperty("type", "dao_call");
                    j.addProperty("component", event.component);
                    j.addProperty("action", event.action);
                    j.addProperty("detail", event.detail);
                    j.addProperty("timestamp", event.timestamp);
                    synchronized (out) {
                        out.write(("data: " + j.toString() + "\n\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        out.flush();
                    }
                } catch (IOException e) {
                }
            };

            EventBus.subscribe(listener);

            try {
                while (ex.getHttpContext() != null) {
                    Thread.sleep(30000);
                    synchronized (out) {
                        out.write(":keepalive\n\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        out.flush();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                EventBus.unsubscribe(listener);
                try { out.close(); } catch (Exception ignored) {}
            }
        }
    }


    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            cors(ex);
            String path = ex.getRequestURI().getPath();

            if (path == null || path.equals("/") || path.equals("/login.html") || path.equals("/index.html")) {
                path = "/pages/login.html";
            } else if (path.equals("/app.html") || path.equals("/treino.html")) {
                path = "/pages/app.html";
            } else if (path.equals("/fluxo.html")) {
                path = "/pages/fluxo.html";
            } else if (path.equals("/style.css")) {
                path = "/css/style.css";
            } else if (path.equals("/app.js")) {
                path = "/js/app.js";
            }

            String resource = "/FitFlow app" + path;
            InputStream in = getClass().getResourceAsStream(resource);

            if (in == null) {
                String msg = "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>FitFlow</title><style>body{font-family:sans-serif;display:flex;justify-content:center;align-items:center;height:100vh;background:#1a1a2e;color:#fff;text-align:center}div{max-width:400px}h1{color:#e94560}a{color:#0f3460;background:#e94560;padding:10px 20px;border-radius:6px;text-decoration:none;margin-top:20px;display:inline-block}</style></head><body><div><h1>FitFlow Manager</h1><p>Servidor mobile rodando!</p><a href='/pages/login.html'>Acessar Login</a></div></body></html>";
                byte[] b = msg.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                ex.sendResponseHeaders(200, b.length);
                ex.getResponseBody().write(b);
                ex.getResponseBody().close();
                return;
            }

            String mime = "text/html; charset=UTF-8";
            if (path.endsWith(".css")) mime = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) mime = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".png")) mime = "image/png";
            else if (path.endsWith(".svg")) mime = "image/svg+xml";
            else if (path.endsWith(".ico")) mime = "image/x-icon";
            else if (path.endsWith(".json")) mime = "application/json; charset=UTF-8";
            else if (path.endsWith(".gif")) mime = "image/gif";

            ex.getResponseHeaders().add("Content-Type", mime);
            ex.getResponseHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
            ex.getResponseHeaders().add("Pragma", "no-cache");
            ex.getResponseHeaders().add("Expires", "0");
            byte[] data = readAll(in);
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
            ex.getResponseBody().close();
        }

        private byte[] readAll(InputStream in) throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[4096];
            int n;
            while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);
            return buf.toByteArray();
        }
    }
}
