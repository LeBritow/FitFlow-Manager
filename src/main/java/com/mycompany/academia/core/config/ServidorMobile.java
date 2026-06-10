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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ServidorMobile {

    private static HttpServer servidorAtual;
    private static final Map<String, Integer> sessoesAtivas = new HashMap<>(); // token → alunoId

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

            // Arquivos estáticos (SPA)
            servidorAtual.createContext("/", new StaticFileHandler());

            servidorAtual.setExecutor(null);
            servidorAtual.start();
            System.out.println("🚀 Servidor Mobile rodando em http://localhost:8081");
            System.out.println("📱 Acesse pelo celular com o IP da máquina: http://" + getLocalIp() + ":8081");

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

    // ─── helpers ────────────────────────────────────────────────────────

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

    // ─── TESTE ─────────────────────────────────────────────────────────

    static class TesteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            JsonObject j = new JsonObject();
            j.addProperty("status", "sucesso");
            j.addProperty("mensagem", "API FitFlow online!");
            json(ex, 200, j.toString());
        }
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
                JsonObject body = bodyJson(ex);
                String login = body.get("login").getAsString();
                String senha = body.get("senha").getAsString();

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

    // ─── FICHA ──────────────────────────────────────────────────────────

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
                Map<String, String> q = queryParams(ex.getRequestURI().getQuery());
                int alunoId = Integer.parseInt(q.getOrDefault("alunoId", "0"));
                if (alunoId == 0) { json(ex, 400, "{\"erro\":\"alunoId obrigatório.\"}"); return; }

                TreinoDAO dao = new TreinoDAO();
                List<ProgramacaoTreino> progs = dao.listarProgramacoesPorAluno(alunoId);
                if (progs.isEmpty()) { json(ex, 404, "{\"erro\":\"Nenhuma ficha ativa.\"}"); return; }

                ProgramacaoTreino ficha = progs.get(0);
                Treino treino = ficha.getTreino();
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
                json(ex, 200, j.toString());

            } catch (Exception e) {
                json(ex, 500, "{\"erro\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ─── FINALIZAR TREINO ──────────────────────────────────────────────

    static class FinalizarTreinoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (options(ex)) return;
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                json(ex, 405, "{\"erro\":\"Use POST.\"}"); return;
            }
            EntityManager em = JPAUtil.getEntityManager();
            try {
                JsonObject body = bodyJson(ex);
                int alunoId = body.get("alunoId").getAsInt();
                int treinoId = body.get("treinoId").getAsInt();
                String comentario = body.has("comentario") ? body.get("comentario").getAsString() : "Treino concluído.";

                Aluno aluno = em.find(Aluno.class, alunoId);
                Treino treino = em.find(Treino.class, treinoId);
                if (aluno == null || treino == null) {
                    json(ex, 404, "{\"status\":\"erro\",\"mensagem\":\"Aluno ou Treino não encontrados.\"}");
                    return;
                }

                em.getTransaction().begin();

                List<ProgramacaoTreino> progs = em.createQuery(
                    "SELECT p FROM ProgramacaoTreino p WHERE p.aluno.id = :a AND p.treino.id = :t", ProgramacaoTreino.class)
                    .setParameter("a", alunoId).setParameter("t", treinoId).getResultList();

                if (!progs.isEmpty()) {
                    ProgramacaoTreino prog = progs.get(0);
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
                                em.persist(ir);
                            }
                        }
                    }
                }

                ComentarioTreino c = new ComentarioTreino();
                c.setAluno(aluno);
                c.setTreino(treino);
                c.setTexto(comentario);
                c.setDataCriacao(LocalDateTime.now());
                c.setLido(false);
                em.persist(c);

                em.getTransaction().commit();
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

    // ─── LISTAR EXERCÍCIOS ──────────────────────────────────────────────

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
                int alunoId = Integer.parseInt(queryParams(ex.getRequestURI().getQuery()).getOrDefault("alunoId", "0"));
                if (alunoId == 0) { json(ex, 400, "{\"erro\":\"alunoId obrigatório.\"}"); return; }

                TreinoDAO dao = new TreinoDAO();
                LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

                long treinosMes = dao.buscarQuantidadeTreinosMes(alunoId);
                long totalTreinos = dao.buscarTotalTreinos(alunoId);
                String ultimoTreino = dao.buscarDataUltimoTreino(alunoId);
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

    // ─── HISTÓRICO (Feed) ──────────────────────────────────────────────

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
                int alunoId = Integer.parseInt(queryParams(ex.getRequestURI().getQuery()).getOrDefault("alunoId", "0"));
                if (alunoId == 0) { json(ex, 400, "{\"erro\":\"alunoId obrigatório.\"}"); return; }

                TreinoDAO dao = new TreinoDAO();
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
                    if (alunoId == 0) { json(ex, 400, "{\"erro\":\"alunoId obrigatório.\"}"); return; }
                    EntityManager em = JPAUtil.getEntityManager();
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
                    JsonObject body = bodyJson(ex);
                    alunoId = body.get("alunoId").getAsInt();
                    float peso = body.get("peso").getAsFloat();
                    float altura = body.get("altura").getAsFloat();

                    EntityManager em = JPAUtil.getEntityManager();
                    em.getTransaction().begin();
                    Aluno a = em.find(Aluno.class, alunoId);
                    if (a != null) {
                        a.setPeso(peso);
                        a.setAltura(altura);
                        a.setImc(peso / (altura * altura));
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

    // ─── STATIC FILE SERVER ────────────────────────────────────────────

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            cors(ex);
            String path = ex.getRequestURI().getPath();

            // Redirecionar URLs antigas para nova estrutura
            if (path == null || path.equals("/") || path.equals("/login.html") || path.equals("/index.html")) {
                path = "/pages/login.html";
            } else if (path.equals("/app.html") || path.equals("/treino.html")) {
                path = "/pages/app.html";
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
