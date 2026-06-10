package com.mycompany.academia.core.config;

import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.aluno.model.AvaliacaoFisica;
import com.mycompany.academia.core.session.SessaoTreino;
import com.mycompany.academia.treino.enums.ObjetivoTreino;
import com.mycompany.academia.treino.model.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class SeedData {

    private static EntityManager em;

    public static void main(String[] args) {
        em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            long totalAlunos = (long) em.createQuery("SELECT COUNT(a) FROM Aluno a").getSingleResult();
            System.out.println("Total de alunos no banco: " + totalAlunos);
            if (totalAlunos > 0) {
                List<Aluno> existentes = em.createQuery("SELECT a FROM Aluno a", Aluno.class).getResultList();
                for (Aluno a : existentes) {
                    System.out.println("  -> ID=" + a.getId() + " Nome=" + a.getNome() + " CPF=" + a.getCpf());
                }
            }

            // Remove dados conflitantes para garantir execucao limpa
            String CPF_ALVO = "111.222.333-44";
            List<Aluno> conflitantes = em.createQuery("SELECT a FROM Aluno a WHERE a.cpf = :cpf", Aluno.class)
                .setParameter("cpf", CPF_ALVO).getResultList();
            for (Aluno velho : conflitantes) {
                int id = velho.getId();
                System.out.println("Removendo aluno ID=" + id + " (" + velho.getNome() + ") com CPF conflitante");
                // Ordem correta para evitar violacao de FK
                em.createQuery("DELETE FROM ItemRealizado ir WHERE ir.sessaoTreino.id IN (SELECT s.id FROM SessaoTreino s WHERE s.programacaoTreino.aluno.id = :id)").setParameter("id", id).executeUpdate();
                em.createQuery("DELETE FROM SessaoTreino s WHERE s.programacaoTreino.aluno.id = :id").setParameter("id", id).executeUpdate();
                em.createQuery("DELETE FROM ProgramacaoTreino p WHERE p.aluno.id = :id").setParameter("id", id).executeUpdate();
                em.createQuery("DELETE FROM AvaliacaoFisica af WHERE af.aluno.id = :id").setParameter("id", id).executeUpdate();
                // Remove comentarios E series/items/templates (independente do aluno)
                em.createQuery("DELETE FROM ComentarioTreino c WHERE c.aluno.id = :id OR c.treino.id IN (SELECT t.id FROM Treino t WHERE t.fichaPadrao = true)").setParameter("id", id).executeUpdate();
                em.createQuery("DELETE FROM SerieTreino st WHERE st.itemTreino.id IN (SELECT i.id FROM ItemTreino i WHERE i.treino.fichaPadrao = true)").executeUpdate();
                em.createQuery("DELETE FROM ItemTreino i WHERE i.treino.fichaPadrao = true").executeUpdate();
                em.createQuery("DELETE FROM Treino t WHERE t.fichaPadrao = true").executeUpdate();
                em.remove(em.merge(velho));
                System.out.println("Dados conflitantes removidos.");
            }

            // 1. Exercicios
            Exercicio supino = criarExercicio("Supino Reto", "Peito", "Deitado no banco reto, empurre a barra");
            Exercicio crucifixo = criarExercicio("Crucifixo", "Peito", "Deitado, abra os bracos com halteres");
            Exercicio remada = criarExercicio("Remada Curvada", "Costas", "Curvado, puxe a barra ate o abdomen");
            Exercicio puxada = criarExercicio("Puxada Alta", "Costas", "Sentado na polia, puxe ate o peito");
            Exercicio agachamento = criarExercicio("Agachamento Livre", "Pernas", "Barra nas costas, agache ate 90");
            Exercicio legPress = criarExercicio("Leg Press 45", "Pernas", "Empurre a plataforma com as pernas");
            Exercicio desenvolvimento = criarExercicio("Desenvolvimento", "Ombros", "Empurre a barra acima da cabeca");
            Exercicio roscaDireta = criarExercicio("Rosca Direta", "Biceps", "Com barra W, flexione os cotovelos");
            Exercicio tricepsPulley = criarExercicio("Triceps Pulley", "Triceps", "Na polia alta, estenda os cotovelos");
            Exercicio stiff = criarExercicio("Stiff", "Posterior", "Com barra, incline o tronco mantendo as pernas esticadas");
            Exercicio panturrilha = criarExercicio("Panturrilha em Pe", "Panturrilhas", "Em pe no step, eleve os calcanhares");

            // 2. Aluno
            Aluno aluno = new Aluno();
            aluno.setNome("Carlos Eduardo");
            aluno.setEmail("carlos.edu@email.com");
            aluno.setCpf("111.222.333-44");
            aluno.setSenha("123456");
            aluno.setPeso(78.5f);
            aluno.setAltura(1.78f);
            aluno.setImc(78.5f / (1.78f * 1.78f));
            em.persist(aluno);

            // 3. Fichas de treino
            // Ficha A - Peito e Triceps
            Treino fA = criarTreino("Ficha A - Peito e Triceps", ObjetivoTreino.HIPERTROFIA, true);
            ItemTreino iA1 = criarItem(fA, supino, 60, true,
                serie(1, 12, 20), serie(2, 10, 25), serie(3, 10, 25), serie(4, 8, 30));
            ItemTreino iA2 = criarItem(fA, crucifixo, 45, true,
                serie(1, 12, 12), serie(2, 12, 14), serie(3, 10, 14));
            ItemTreino iA3 = criarItem(fA, tricepsPulley, 45, true,
                serie(1, 15, 15), serie(2, 12, 18), serie(3, 12, 18));
            ItemTreino iA4 = criarItem(fA, supino, 60, true,
                serie(1, 10, 25), serie(2, 8, 30), serie(3, 6, 35));

            // Ficha B - Costas e Biceps
            Treino fB = criarTreino("Ficha B - Costas e Biceps", ObjetivoTreino.HIPERTROFIA, true);
            ItemTreino iB1 = criarItem(fB, remada, 60, true,
                serie(1, 12, 40), serie(2, 10, 45), serie(3, 8, 50));
            ItemTreino iB2 = criarItem(fB, puxada, 60, true,
                serie(1, 12, 35), serie(2, 10, 40), serie(3, 8, 45));
            ItemTreino iB3 = criarItem(fB, roscaDireta, 45, true,
                serie(1, 12, 10), serie(2, 10, 12), serie(3, 10, 12), serie(4, 8, 14));
            ItemTreino iB4 = criarItem(fB, remada, 60, false,
                serie(1, 12, 35), serie(2, 10, 40));

            // Ficha C - Pernas e Ombros
            Treino fC = criarTreino("Ficha C - Pernas e Ombros", ObjetivoTreino.FORCA_PURA, true);
            ItemTreino iC1 = criarItem(fC, agachamento, 120, true,
                serie(1, 8, 60), serie(2, 6, 70), serie(3, 5, 80), serie(4, 3, 90));
            ItemTreino iC2 = criarItem(fC, legPress, 90, true,
                serie(1, 10, 100), serie(2, 8, 120), serie(3, 6, 140));
            ItemTreino iC3 = criarItem(fC, desenvolvimento, 60, true,
                serie(1, 8, 20), serie(2, 6, 25), serie(3, 5, 28));
            ItemTreino iC4 = criarItem(fC, stiff, 60, false,
                serie(1, 10, 40), serie(2, 10, 45), serie(3, 8, 50));
            ItemTreino iC5 = criarItem(fC, panturrilha, 30, false,
                serie(1, 20, 60), serie(2, 15, 70), serie(3, 12, 80));

            List<Treino> fichas = Arrays.asList(fA, fB, fC);
            List<List<ItemTreino>> itensPorFicha = Arrays.asList(
                Arrays.asList(iA1, iA2, iA3, iA4),
                Arrays.asList(iB1, iB2, iB3, iB4),
                Arrays.asList(iC1, iC2, iC3, iC4, iC5)
            );

            // 4. Programacao de treino
            ProgramacaoTreino pA = criarProgramacao(aluno, fA, "Segunda");
            ProgramacaoTreino pB = criarProgramacao(aluno, fB, "Quarta");
            ProgramacaoTreino pC = criarProgramacao(aluno, fC, "Sexta");
            List<ProgramacaoTreino> progs = Arrays.asList(pA, pB, pC);

            // 5. Sessoes + Itens Realizados (8 semanas, 3x/semana)
            criarSessoesEItens(aluno, fichas, itensPorFicha, progs);

            // 6. Avaliacoes fisicas (peso ao longo do tempo)
            criarAvaliacoes(aluno);

            // 7. Comentarios / feedbacks
            criarComentarios(aluno, fichas);

            em.getTransaction().commit();

            long totalSessoes = (long) em.createQuery("SELECT COUNT(s) FROM SessaoTreino s").getSingleResult();
            long totalItens = (long) em.createQuery("SELECT COUNT(ir) FROM ItemRealizado ir").getSingleResult();
            System.out.println("Seed concluido com sucesso!");
            System.out.println("  Aluno: " + aluno.getNome() + " (ID=" + aluno.getId() + ")");
            System.out.println("  Exercicios: 11");
            System.out.println("  Fichas: 3");
            System.out.println("  Sessoes: " + totalSessoes);
            System.out.println("  ItensRealizados: " + totalItens);
            System.out.println("  Avaliacoes: 5");
            System.out.println("  Comentarios: 4");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    // ========== HELPERS ==========

    static Exercicio criarExercicio(String nome, String grupo, String descricao) {
        Exercicio e = new Exercicio();
        e.setNome(nome);
        e.setGrupoMuscular(grupo);
        e.setDescricao(descricao);
        em.persist(e);
        return e;
    }

    static Treino criarTreino(String nome, ObjetivoTreino obj, boolean padrao) {
        Treino t = new Treino();
        t.setNome(nome);
        t.setObjetivo(obj);
        t.setFichaPadrao(padrao);
        em.persist(t);
        return t;
    }

    static SerieTreino serie(int numero, int reps, float carga) {
        SerieTreino s = new SerieTreino();
        s.setNumeroDaSerie(numero);
        s.setRepeticoes(reps);
        s.setCarga(carga);
        return s;
    }

    static ItemTreino criarItem(Treino treino, Exercicio exercicio, float descanso, boolean progCarga, SerieTreino... series) {
        ItemTreino i = new ItemTreino();
        i.setTreino(treino);
        i.setExercicio(exercicio);
        i.setIntervaloDescanso(descanso);
        i.setProgressaoCarga(progCarga);
        for (SerieTreino s : series) i.adicionarSerie(s);
        em.persist(i);
        return i;
    }

    static ProgramacaoTreino criarProgramacao(Aluno aluno, Treino ficha, String dia) {
        ProgramacaoTreino p = new ProgramacaoTreino();
        p.setAluno(aluno);
        p.setTreino(ficha);
        p.setDataInicioSemanas(LocalDateTime.now().minusWeeks(8));
        p.setDataFimSemanas(LocalDateTime.now().plusWeeks(4));
        p.setDiaDaSemana(dia);
        em.persist(p);
        return p;
    }

    static void criarSessoesEItens(Aluno aluno, List<Treino> fichas, List<List<ItemTreino>> itensPorFicha, List<ProgramacaoTreino> progs) {
        // [fichaIndex][itemIndex] = {cargaBase, incrementoPorSemana}
        float[][][] plano = {
            { {20, 1.5f}, {12, 1.0f}, {15, 1.0f}, {25, 1.5f} },
            { {40, 2.0f}, {35, 1.5f}, {10, 0.8f}, {35, 1.5f} },
            { {60, 3.0f}, {100, 5.0f}, {20, 1.5f}, {40, 2.0f}, {60, 3.0f} }
        };

        Random rnd = new Random(42);
        int[] diasSemana = {1, 3, 5}; // seg, qua, sex

        for (int semana = 0; semana < 8; semana++) {
            for (int f = 0; f < 3; f++) {
                LocalDate dataSessao = LocalDate.now().minusWeeks(8 - semana)
                    .with(java.time.DayOfWeek.of(diasSemana[f]));
                LocalDateTime dataHora = LocalDateTime.of(dataSessao, LocalTime.of(18, 30));

                SessaoTreino sessao = new SessaoTreino();
                sessao.setProgramacaoTreino(progs.get(f));
                sessao.setData(dataHora);
                sessao.setConcluido(true);
                em.persist(sessao);

                List<ItemTreino> itens = itensPorFicha.get(f);
                float[][] cargas = plano[f];

                for (int idx = 0; idx < itens.size() && idx < cargas.length; idx++) {
                    float base = cargas[idx][0];
                    float inc = cargas[idx][1];
                    float carga = base + semana * inc + (rnd.nextFloat() - 0.3f) * 2f;
                    carga = Math.round(carga * 10f) / 10f;

                    ItemRealizado ir = new ItemRealizado();
                    ir.setSessaoTreino(sessao);
                    ir.setItemTreino(itens.get(idx));
                    ir.setCargaUtilizada(carga);
                    ir.setFeito(true);

                    float cargaAnterior = base + Math.max(0, semana - 1) * inc;
                    if (semana == 0) ir.setStatusCarga("MANTEVE");
                    else if (carga > cargaAnterior + 0.5f) ir.setStatusCarga("SUBIU");
                    else if (carga < cargaAnterior - 0.5f) ir.setStatusCarga("DIMINUIU");
                    else ir.setStatusCarga("MANTEVE");

                    ir.setTempoExecucaoSegundos(30 + rnd.nextInt(30));
                    ir.setTempoDescansoSegundos(45 + rnd.nextInt(30));
                    em.persist(ir);
                }
            }
        }
    }

    static void criarAvaliacoes(Aluno aluno) {
        float[][] medidas = {
            {82.5f, 1.78f},
            {81.0f, 1.78f},
            {80.2f, 1.78f},
            {79.4f, 1.78f},
            {78.5f, 1.78f},
        };
        for (int i = 0; i < medidas.length; i++) {
            float peso = medidas[i][0];
            float altura = medidas[i][1];
            float imc = peso / (altura * altura);
            LocalDate data = LocalDate.now().minusWeeks(8 - i * 2);
            AvaliacaoFisica af = new AvaliacaoFisica(aluno, peso, altura, imc, data);
            em.persist(af);
        }
        // atualiza os dados atuais do aluno
        aluno.setPeso(78.5f);
        aluno.setAltura(1.78f);
        aluno.setImc(78.5f / (1.78f * 1.78f));
        em.merge(aluno);
    }

    static void criarComentarios(Aluno aluno, List<Treino> fichas) {
        String[][] feedbacks = {
            {"8", "Treino pesado hoje, mas consegui completar todas as series."},
            {"6", "Senti um pouco de dor no ombro durante o desenvolvimento."},
            {"4", "Consegui aumentar a carga no supino! Evoluindo bem."},
            {"2", "Treino muito bom, energia total."},
        };
        for (String[] fb : feedbacks) {
            int semanas = Integer.parseInt(fb[0]);
            ComentarioTreino c = new ComentarioTreino();
            c.setAluno(aluno);
            c.setTreino(fichas.get(0));
            c.setTexto(fb[1]);
            c.setDataCriacao(LocalDateTime.now().minusWeeks(semanas));
            c.setLido(false);
            em.persist(c);
        }
    }
}
