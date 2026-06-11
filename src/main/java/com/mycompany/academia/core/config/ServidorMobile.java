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
import com.mycompany.academia.treino.model.ComentarioTreino;
import com.mycompany.academia.treino.model.Exercicio;
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
            servidorAtual.createContext("/api/teste", new TesteHandler());
            servidorAtual.createContext("/api/login", new LoginHandler());
            servidorAtual.createContext("/api/ficha", new BuscarFichaHandler());
            servidorAtual.createContext("/api/treino/finalizar", new FinalizarTreinoHandler());
            servidorAtual.createContext("/api/exercicios", new ListarExerciciosHandler());
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

    static class TesteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            JsonObject j = new JsonObject();
            j.addProperty("status", "sucesso");
            j.addProperty("mensagem", "API FitFlow online!");
            json(ex, 200, j.toString());
        }
    }

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
                Usuario u = dao.autenticar(login, senha);

                if (u != null && u instanceof Aluno) {
                    String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                    sessoesAtivas.put(token, u.getId());

                    JsonObject j = new JsonObject();
                    j.addProperty("status", "sucesso");
                    j.addProperty("token", token);
                    j.addProperty("id", u.getId());
                    j.addProperty("nome", u.getNome());
                    j.addProperty("email", u.getEmail());
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
                List<ProgramacaoTreino> progs = dao.listarProgramacoesPorAluno(alunoId);
                if (progs.isEmpty()) { json(ex, 404, "{\"erro\":\"Nenhuma ficha ativa.\"}"); return; }

                ProgramacaoTreino ficha = progs.get(0);
                Treino treino = ficha.getTreino();
                EventBus.emit("PostgreSQL", "SELECT FROM item_treino JOIN exercicio WHERE treinoId=" + treino.getId(), "BuscarFichaHandler");
                EventBus.emit("Entities", "ItemTreino+Exercicio loaded", "treinoId=" + treino.getId());
                EventBus.emit("ServidorMobile", "BuscarFichaHandler", "Buscando itens do treino " + treino.getId());
                List<ItemTreino> itens = dao.listarItensPorTreino(treino.getId());

                JsonObject j = new JsonObject();
                j.addProperty("idProgramacao", ficha.getId());
                j.addProperty("idFicha", treino.getId());
                j.addProperty("nomeTreino", treino.getNome());
                j.addProperty("objetivo", treino.getObjetivo() != null ? treino.getObjetivo().name() : "");

                JsonArray exs = new JsonArray();
                for (ItemTreino item : itens) {
                    JsonObject o = new JsonObject();
                    o.addProperty("idItem", item.getId());
                    o.addProperty("nomeExercicio", item.getExercicio().getNome());
                    o.addProperty("grupoMuscular", item.getExercicio().getGrupoMuscular());
                    o.addProperty("descanso", item.getIntervaloDescanso());
                    o.addProperty("progressaoCarga", item.isProgressaoCarga());
                    String urlMidia = item.getExercicio().getUrlMidia();
                    o.addProperty("urlMidia", urlMidia != null && !urlMidia.isEmpty() ? urlMidia : "");

                    JsonArray ss = new JsonArray();
                    for (SerieTreino s : item.getSeriesTreino()) {
                        JsonObject so = new JsonObject();
                        so.addProperty("serie", s.getNumeroDaSerie());
                        so.addProperty("reps", s.getRepeticoes());
                        so.addProperty("carga", s.getCarga());
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
                String comentario = body.has("comentario") ? body.get("comentario").getAsString() : "Treino concluído.";

                EventBus.emit("JPA", "EntityManager.find(Aluno)", "alunoId=" + alunoId);
                EventBus.emit("PostgreSQL", "SELECT FROM aluno WHERE id=" + alunoId, "find");
                EventBus.emit("Entities", "Aluno loaded", "alunoId=" + alunoId);
                Aluno aluno = em.find(Aluno.class, alunoId);
                EventBus.emit("JPA", "EntityManager.find(Treino)", "treinoId=" + treinoId);
                EventBus.emit("PostgreSQL", "SELECT FROM treino WHERE id=" + treinoId, "find");
                EventBus.emit("Entities", "Treino loaded", "treinoId=" + treinoId);
                Treino treino = em.find(Treino.class, treinoId);
                if (aluno == null || treino == null) {
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
                    ProgramacaoTreino prog = progs.get(0);
                    EventBus.emit("JPA", "EntityManager.persist(SessaoTreino)", "Criando sessão de treino");
                    EventBus.emit("PostgreSQL", "INSERT INTO sessao_treino (alunoId=" + alunoId + ", treinoId=" + treinoId + ")", "persist");
                    EventBus.emit("Entities", "SessaoTreino created", "alunoId=" + alunoId);
                    SessaoTreino sessao = new SessaoTreino();
                    sessao.setProgramacaoTreino(prog);
                    sessao.setData(LocalDateTime.now());
                    sessao.setConcluido(true);
                    em.persist(sessao);

                    if (body.has("itensRealizados")) {
                        for (var e : body.get("itensRealizados").getAsJsonArray()) {
                            JsonObject o = e.getAsJsonObject();
                            int idItem = o.get("itemTreinoId").getAsInt();
                            float carga = o.get("carga").getAsFloat();
                            boolean feito = o.get("feito").getAsBoolean();
                            int tExec = o.has("tempoExecucao") ? o.get("tempoExecucao").getAsInt() : 0;
                            int tDesc = o.has("tempoDescanso") ? o.get("tempoDescanso").getAsInt() : 0;
                            String sc = o.has("statusCarga") ? o.get("statusCarga").getAsString() : "MANTEVE";

                            EventBus.emit("JPA", "EntityManager.find(ItemTreino)", "itemTreinoId=" + idItem);
                            EventBus.emit("PostgreSQL", "SELECT FROM item_treino WHERE id=" + idItem, "find");
                            EventBus.emit("Entities", "ItemTreino loaded", "itemTreinoId=" + idItem);
                            ItemTreino it = em.find(ItemTreino.class, idItem);
                            if (it != null) {
                                ItemRealizado ir = new ItemRealizado();
                                ir.setSessaoTreino(sessao);
                                ir.setItemTreino(it);
                                ir.setCargaUtilizada(carga);
                                ir.setFeito(feito);
                                ir.setTempoExecucaoSegundos(tExec);
                                ir.setTempoDescansoSegundos(tDesc);
                                ir.setStatusCarga(sc);
                                EventBus.emit("JPA", "EntityManager.persist(ItemRealizado)", "feito=" + feito + ", carga=" + carga);
                                EventBus.emit("PostgreSQL", "INSERT INTO item_realizado (itemTreinoId=" + idItem + ")", "persist");
                                EventBus.emit("Entities", "ItemRealizado created", "feito=" + feito);
                                em.persist(ir);
                            }
                        }
                    }
                }

                EventBus.emit("JPA", "EntityManager.persist(ComentarioTreino)", "Registrando feedback do treino");
                EventBus.emit("PostgreSQL", "INSERT INTO comentario_treino (alunoId=" + alunoId + ")", "persist");
                EventBus.emit("Entities", "ComentarioTreino created", "alunoId=" + alunoId);
                ComentarioTreino c = new ComentarioTreino();
                c.setAluno(aluno);
                c.setTreino(treino);
                c.setTexto(comentario);
                c.setDataCriacao(LocalDateTime.now());
                c.setLido(false);
                em.persist(c);

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

    static class ListarExerciciosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (options(ex)) return;
            cors(ex);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                json(ex, 405, "{\"erro\":\"Use GET.\"}"); return;
            }
            try {
                EventBus.emit("ServidorMobile", "ListarExerciciosHandler", "Recebendo GET /api/exercicios");
                EventBus.emit("PostgreSQL", "SELECT FROM exercicio ORDER BY grupoMuscular", "select");
                ExercicioDAO dao = new ExercicioDAO();
                JsonArray a = new JsonArray();
                for (Exercicio e : dao.listarTodos()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("id", e.getId());
                    o.addProperty("nome", e.getNome());
                    o.addProperty("grupo", e.getGrupoMuscular());
                    o.addProperty("descricao", e.getDescricao() != null ? e.getDescricao() : "");
                    a.add(o);
                }
                json(ex, 200, a.toString());
            } catch (Exception e) {
                json(ex, 500, "{\"erro\":\"Erro ao buscar exercícios.\"}");
            }
        }
    }

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

                long treinosMes = dao.buscarQuantidadeTreinosMes(alunoId);
                EventBus.emit("PostgreSQL", "SELECT COUNT FROM comentario_treino WHERE alunoId=" + alunoId, "count");
                long totalTreinos = dao.buscarTotalTreinos(alunoId);
                EventBus.emit("PostgreSQL", "SELECT MAX(dataCriacao) FROM comentario_treino WHERE alunoId=" + alunoId, "select");
                String ultimoTreino = dao.buscarDataUltimoTreino(alunoId);
                EventBus.emit("PostgreSQL", "SELECT DISTINCT data FROM sessao_treino WHERE alunoId=" + alunoId, "select");
                long streak = dao.buscarStreakAtual(alunoId);

                JsonObject j = new JsonObject();
                j.addProperty("treinosMes", treinosMes);
                j.addProperty("totalTreinos", totalTreinos);
                j.addProperty("ultimoTreino", ultimoTreino);
                j.addProperty("streak", streak);
                j.addProperty("fichaAtiva", dao.buscarNomeFichaAtiva(alunoId));

                java.util.Map<Integer, Long> semanaMap = dao.buscarTreinosPorSemana(alunoId, 4);
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
                List<ComentarioTreino> comentarios = dao.buscarComentariosPorAluno(alunoId);

                JsonArray arr = new JsonArray();
                for (ComentarioTreino c : comentarios) {
                    JsonObject o = new JsonObject();
                    o.addProperty("data", c.getDataCriacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    o.addProperty("treinoNome", c.getTreino().getNome());
                    o.addProperty("comentario", c.getTexto());
                    o.addProperty("lido", c.isLido());
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
                    Aluno a = em.find(Aluno.class, alunoId);
                    em.close();
                    if (a == null) { json(ex, 404, "{\"erro\":\"Aluno não encontrado.\"}"); return; }

                    JsonObject j = new JsonObject();
                    j.addProperty("id", a.getId());
                    j.addProperty("nome", a.getNome());
                    j.addProperty("email", a.getEmail());
                    j.addProperty("cpf", a.getCpf());
                    j.addProperty("peso", a.getPeso());
                    j.addProperty("altura", a.getAltura());
                    j.addProperty("imc", a.getImc());

                    AlunoDAO alunoDAO = new AlunoDAO();
                    EventBus.emit("ServidorMobile", "PerfilHandler", "Buscando avaliações físicas do aluno " + alunoId);
                    EventBus.emit("PostgreSQL", "SELECT FROM avaliacao_fisica WHERE alunoId=" + alunoId, "select");
                    List<AvaliacaoFisica> avals = alunoDAO.buscarAvaliacoesPorAluno(alunoId);
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
                    Aluno a = em.find(Aluno.class, alunoId);
                    if (a != null) {
                        a.setPeso(peso);
                        a.setAltura(altura);
                        a.setImc(peso / (altura * altura));
                        EventBus.emit("PostgreSQL", "UPDATE aluno SET peso=" + peso + " WHERE id=" + alunoId, "merge");
                        em.merge(a);
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
