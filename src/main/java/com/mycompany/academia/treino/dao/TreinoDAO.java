package com.mycompany.academia.treino.dao;

import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.treino.model.ItemTreino;
import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.config.JPAUtil;
import com.mycompany.academia.treino.model.ProgramacaoTreino;
import com.mycompany.academia.treino.model.Treino;
import jakarta.persistence.EntityManager;
import java.util.List;

public class TreinoDAO {

    public Treino inserirTreino(Treino treino) {
        EventBus.emit("TreinoDAO", "inserirTreino", "treino=" + treino.getNome());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (treino.getId() == 0) {
                em.persist(treino);
                EventBus.emit("PostgreSQL", "INSERT INTO treino (nome=" + treino.getNome() + ")", "persist");
                EventBus.emit("Entities", "Treino created", "nome=" + treino.getNome());
            } else {
                treino = em.merge(treino);
                EventBus.emit("PostgreSQL", "UPDATE treino SET ... WHERE id=" + treino.getId(), "merge");
                EventBus.emit("Entities", "Treino updated", "id=" + treino.getId());
            }
            em.getTransaction().commit();
            return treino; 
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    public boolean inserirItemTreino(ItemTreino item) {
        EventBus.emit("TreinoDAO", "inserirItemTreino", "itemTreinoId=" + item.getId());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (item.getId() == 0) {
                em.persist(item);
                Treino oTreinoItem = item.getTreino();
                EventBus.emit("PostgreSQL", "INSERT INTO item_treino (treinoId=" + oTreinoItem.getId() + ")", "persist");
            } else {
                em.merge(item);
                EventBus.emit("PostgreSQL", "UPDATE item_treino SET ... WHERE id=" + item.getId(), "merge");
            }
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
            return false;
        } finally {
            em.close();
        }
    }

    public List<Aluno> listarAlunos() {
        EventBus.emit("TreinoDAO", "listarAlunos", "");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            EventBus.emit("PostgreSQL", "SELECT FROM aluno ORDER BY nome", "query");
            return em.createQuery("SELECT a FROM Aluno a ORDER BY a.nome", Aluno.class).getResultList();
        } finally {
            em.close();
        }
    }
    
    public boolean inserirProgramacao(ProgramacaoTreino programacao) {
        EventBus.emit("TreinoDAO", "inserirProgramacao", "alunoId=" + programacao.getAluno().getId());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(programacao);
            Aluno oAlunoProg = programacao.getAluno();
            int oAlunoIdProg = oAlunoProg.getId();
            EventBus.emit("PostgreSQL", "INSERT INTO programacao_treino (alunoId=" + oAlunoIdProg + ")", "persist");
            EventBus.emit("Entities", "ProgramacaoTreino created", "alunoId=" + oAlunoIdProg);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
            return false;
        } finally {
            em.close();
        }
    }
    
    public List<ProgramacaoTreino> listarProgramacoesDoAluno(int alunoId) {
        EventBus.emit("TreinoDAO", "listarProgramacoesDoAluno", "alunoId=" + alunoId);
        EntityManager em = JPAUtil.getEntityManager();
        try {
            EventBus.emit("PostgreSQL", "SELECT FROM programacao_treino JOIN treino WHERE alunoId=" + alunoId, "query");
            EventBus.emit("Entities", "ProgramacaoTreino+Treino loaded", "alunoId=" + alunoId);
            List<ProgramacaoTreino> r = em.createQuery("SELECT p FROM ProgramacaoTreino p JOIN FETCH p.treino WHERE p.aluno.id = :alunoId", ProgramacaoTreino.class)
                     .setParameter("alunoId", alunoId)
                     .getResultList();
            EventBus.emit("JPA", "JPQL Query", "ProgramacaoTreino JOIN FETCH treino WHERE alunoId=" + alunoId);
            return r;
        } finally {
            em.close();
        }
    }

    public List<ItemTreino> listarItensDoTreino(int treinoId) {
        EventBus.emit("TreinoDAO", "listarItensDoTreino", "treinoId=" + treinoId);
        EntityManager em = JPAUtil.getEntityManager();
        try {
            EventBus.emit("PostgreSQL", "SELECT FROM item_treino JOIN exercicio WHERE treinoId=" + treinoId, "query");
            List<ItemTreino> r = em.createQuery("SELECT DISTINCT i FROM ItemTreino i JOIN FETCH i.exercicio LEFT JOIN FETCH i.seriesTreino WHERE i.treino.id = :treinoId", ItemTreino.class)
                     .setParameter("treinoId", treinoId)
                     .getResultList();
            EventBus.emit("JPA", "JPQL Query", "ItemTreino JOIN FETCH exercicio+series WHERE treinoId=" + treinoId);
            return r;
        } finally {
            em.close();
        }
    }
    
    public List<Treino> listarFichasPadrao() {
        EventBus.emit("TreinoDAO", "listarFichasPadrao", "");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT t FROM Treino t WHERE t.fichaPadrao = true ORDER BY t.nome", Treino.class)
                     .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<String> listarNomesExerciciosDoAluno(int alunoId) {
        EventBus.emit("TreinoDAO", "listarNomesExerciciosDoAluno", "alunoId=" + alunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT DISTINCT e.nome FROM ItemTreino it " +
                          "JOIN it.exercicio e " +
                          "WHERE it.treino IN (" +
                          "    SELECT pt.treino FROM ProgramacaoTreino pt WHERE pt.aluno.id = :alunoId" +
                          "    AND :hoje BETWEEN pt.dataInicioSemanas AND pt.dataFimSemanas" +
                          ") " +
                          "AND EXISTS (" +
                          "    SELECT 1 FROM ItemRealizado ir " +
                          "    WHERE ir.itemTreino = it AND ir.feito = true" +
                          ")";

            jakarta.persistence.TypedQuery<String> query = em.createQuery(jpql, String.class);
            query.setParameter("alunoId", alunoId);
            query.setParameter("hoje", java.time.LocalDateTime.now());
            return query.getResultList();
        } catch (Exception e) {
            System.err.println("Erro ao buscar exercícios do aluno: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        } finally {
            em.close();
        }
    }
    
    public List<com.mycompany.academia.treino.model.ComentarioTreino> listarComentariosDoAluno(int alunoId) {
        EventBus.emit("TreinoDAO", "listarComentariosDoAluno", "alunoId=" + alunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT c FROM ComentarioTreino c " +
                          "JOIN FETCH c.treino t " +
                          "WHERE c.aluno.id = :alunoId " +
                          "ORDER BY c.dataCriacao DESC";
                          
            jakarta.persistence.TypedQuery<com.mycompany.academia.treino.model.ComentarioTreino> query = em.createQuery(jpql, com.mycompany.academia.treino.model.ComentarioTreino.class);
            query.setParameter("alunoId", alunoId);
            return query.getResultList();
        } catch (Exception e) {
            System.err.println("Erro ao buscar feedbacks do aluno: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        } finally {
            em.close();
        }
    }   

    public List<com.mycompany.academia.treino.model.ItemRealizado> listarItensRealizados(int alunoId, int treinoId, java.time.LocalDateTime dataComentario) {
        EventBus.emit("TreinoDAO", "listarItensRealizados", "alunoId=" + alunoId + ", treinoId=" + treinoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpqlSessao = "SELECT s FROM SessaoTreino s " +
                                "WHERE s.programacaoTreino.aluno.id = :alunoId " +
                                "AND s.programacaoTreino.treino.id = :treinoId " +
                                "AND (SELECT COUNT(ir) FROM ItemRealizado ir WHERE ir.sessaoTreino = s) > 0 " + 
                                "ORDER BY s.data DESC";
            List<com.mycompany.academia.core.session.SessaoTreino> sessoes = em.createQuery(jpqlSessao, com.mycompany.academia.core.session.SessaoTreino.class)
                                           .setParameter("alunoId", alunoId)
                                           .setParameter("treinoId", treinoId)
                                           .getResultList();
            if (sessoes.isEmpty()) return java.util.Collections.emptyList();

            com.mycompany.academia.core.session.SessaoTreino oMelhorSessao = sessoes.get(0);
            long menorDiferenca = Math.abs(java.time.temporal.ChronoUnit.SECONDS.between(oMelhorSessao.getData(), dataComentario));
            for (com.mycompany.academia.core.session.SessaoTreino oS : sessoes) {
            long diferenca = Math.abs(java.time.temporal.ChronoUnit.SECONDS.between(oS.getData(), dataComentario));
                if (diferenca < menorDiferenca) {
                    menorDiferenca = diferenca;
                    oMelhorSessao = oS;
                }
            }

            String jpqlItens = "SELECT DISTINCT ir FROM ItemRealizado ir " +
                               "JOIN FETCH ir.itemTreino it " +
                               "JOIN FETCH it.exercicio e " +
                               "LEFT JOIN FETCH it.seriesTreino st " +
                               "WHERE ir.sessaoTreino.id = :sessaoId";
                               
            return em.createQuery(jpqlItens, com.mycompany.academia.treino.model.ItemRealizado.class)
                     .setParameter("sessaoId", oMelhorSessao.getId())
                     .getResultList();
        } finally {
            em.close();
        }
    }

    public List<com.mycompany.academia.treino.model.ItemRealizado> listarHistoricoCargas(int alunoId, String nomeExercicio) {
        EventBus.emit("TreinoDAO", "listarHistoricoCargas", "alunoId=" + alunoId + ", exercicio=" + nomeExercicio);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT ir FROM ItemRealizado ir " +
                          "JOIN FETCH ir.sessaoTreino s " +
                          "JOIN FETCH s.programacaoTreino pt " +
                          "JOIN FETCH pt.treino t " +
                          "WHERE pt.aluno.id = :alunoId " +
                          "AND ir.itemTreino.exercicio.nome = :nomeExercicio " +
                          "AND ir.feito = true " +
                          "ORDER BY s.data ASC";
            return em.createQuery(jpql, com.mycompany.academia.treino.model.ItemRealizado.class)
                     .setParameter("alunoId", alunoId)
                     .setParameter("nomeExercicio", nomeExercicio)
                     .getResultList();
        } finally {
            em.close();
        }
    }

    public String buscarNomeFichaAtiva(int alunoId) {
        EventBus.emit("TreinoDAO", "buscarNomeFichaAtiva", "alunoId=" + alunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT pt.treino.nome FROM ProgramacaoTreino pt " +
                          "WHERE pt.aluno.id = :alunoId " +
                          "AND :hoje BETWEEN pt.dataInicioSemanas AND pt.dataFimSemanas";
            
            java.util.List<String> resultados = em.createQuery(jpql, String.class)
                                        .setParameter("alunoId", alunoId)
                                        .setParameter("hoje", java.time.LocalDateTime.now())
                                        .getResultList();
            
            if (resultados.isEmpty()) {
                return "Nenhuma Ativa";
            }
            return resultados.get(0);
        } catch (Exception e) {
            return "Erro ao buscar";
        } finally {
            em.close();
        }
    }

    public String buscarDataUltimoTreino(int alunoId) {
        EventBus.emit("TreinoDAO", "buscarDataUltimoTreino", "alunoId=" + alunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT MAX(c.dataCriacao) FROM ComentarioTreino c WHERE c.treino IN (" +
                          "    SELECT pt.treino FROM ProgramacaoTreino pt WHERE pt.aluno.id = :alunoId" +
                          ")";
            
            java.time.LocalDateTime ultimaData = em.createQuery(jpql, java.time.LocalDateTime.class)
                                                   .setParameter("alunoId", alunoId)
                                                   .getSingleResult();
            
            if (ultimaData == null) return "Sem registros";
            
            java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return ultimaData.format(formatador);
        } catch (Exception e) {
            return "Não realizado";
        } finally {
            em.close();
        }
    }

    public long contarTreinosNoMes(int alunoId) {
        EventBus.emit("TreinoDAO", "contarTreinosNoMes", "alunoId=" + alunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            java.time.LocalDateTime inicioDoMes = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
            
            String jpql = "SELECT COUNT(c) FROM ComentarioTreino c WHERE c.treino IN (" +
                          "    SELECT pt.treino FROM ProgramacaoTreino pt WHERE pt.aluno.id = :alunoId" +
                          ") AND c.dataCriacao >= :inicioDoMes";
            
            return em.createQuery(jpql, Long.class)
                     .setParameter("alunoId", alunoId)
                     .setParameter("inicioDoMes", inicioDoMes)
                     .getSingleResult();
        } catch (Exception e) {
            return 0;
        } finally {
            em.close();
        }
    }
    
    public boolean excluirProgramacao(ProgramacaoTreino prog) {
        EventBus.emit("TreinoDAO", "excluirProgramacao", "programacaoId=" + prog.getId());
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            prog = em.merge(prog);
            
            Treino oTreino = prog.getTreino();
            boolean isFichaExclusiva = !oTreino.isFichaPadrao();
            
            em.remove(prog);
            
            if (isFichaExclusiva) {
                em.remove(oTreino);
            }
            
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            return false;
        } finally {
            em.close();
        }
    }
    
    public List<com.mycompany.academia.core.session.SessaoTreino> listarSessoesDoAluno(int alunoId) {
        EventBus.emit("TreinoDAO", "listarSessoesDoAluno", "alunoId=" + alunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT DISTINCT s FROM SessaoTreino s " +
                          "JOIN FETCH s.programacaoTreino pt " +
                          "JOIN FETCH pt.treino t " +
                          "WHERE pt.aluno.id = :alunoId " +
                          "AND s.concluido = true " +
                          "ORDER BY s.data DESC";
            return em.createQuery(jpql, com.mycompany.academia.core.session.SessaoTreino.class)
                     .setParameter("alunoId", alunoId)
                     .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public long contarFichas() {
        EventBus.emit("TreinoDAO", "contarFichas", "");
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(t) FROM Treino t WHERE t.fichaPadrao = true", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarFeedbacksNaoLidos() {
        EventBus.emit("TreinoDAO", "contarFeedbacksNaoLidos", "");
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(c) FROM ComentarioTreino c WHERE c.lido = false", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarTreinosHoje() {
        EventBus.emit("TreinoDAO", "contarTreinosHoje", "");
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            java.time.LocalDateTime inicio = java.time.LocalDate.now().atStartOfDay();
            java.time.LocalDateTime fim = inicio.plusDays(1);
            return em.createQuery("SELECT COUNT(s) FROM SessaoTreino s WHERE s.concluido = true AND s.data >= :inicio AND s.data < :fim", Long.class)
                     .setParameter("inicio", inicio)
                     .setParameter("fim", fim)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarTotalTreinos(int alunoId) {
        EventBus.emit("TreinoDAO", "contarTotalTreinos", "alunoId=" + alunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT COUNT(c) FROM ComentarioTreino c WHERE c.treino IN (" +
                          "    SELECT pt.treino FROM ProgramacaoTreino pt WHERE pt.aluno.id = :alunoId" +
                          ")";
            return em.createQuery(jpql, Long.class)
                     .setParameter("alunoId", alunoId)
                     .getSingleResult();
        } catch (Exception e) {
            return 0;
        } finally {
            em.close();
        }
    }

    public java.util.Map<Integer, Long> listarTreinosPorSemana(int alunoId, int semanas) {
        EventBus.emit("TreinoDAO", "listarTreinosPorSemana", "alunoId=" + alunoId + ", semanas=" + semanas);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            java.time.LocalDate inicio = java.time.LocalDate.now().minusWeeks(semanas).with(java.time.DayOfWeek.MONDAY);
            String jpql = "SELECT s.data FROM SessaoTreino s " +
                          "WHERE s.programacaoTreino.aluno.id = :alunoId " +
                          "AND s.concluido = true AND s.data >= :inicio " +
                          "ORDER BY s.data ASC";
            List<java.time.LocalDateTime> datas = em.createQuery(jpql, java.time.LocalDateTime.class)
                                                    .setParameter("alunoId", alunoId)
                                                    .setParameter("inicio", inicio.atStartOfDay())
                                                    .getResultList();

            java.util.Map<Integer, Long> semMap = new java.util.LinkedHashMap<>();
            for (int i = 0; i < semanas; i++) {
                int ano = inicio.plusWeeks(i).getYear();
                int semana = inicio.plusWeeks(i).get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                semMap.put(ano * 100 + semana, 0L);
            }
            for (java.time.LocalDateTime dt : datas) {
                int ano = dt.getYear();
                int semana = dt.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                int chave = ano * 100 + semana;
                semMap.merge(chave, 1L, Long::sum);
            }
            return semMap;
        } catch (Exception e) {
            return java.util.Collections.emptyMap();
        } finally {
            em.close();
        }
    }

    public long buscarSequenciaAtual(int alunoId) {
        EventBus.emit("TreinoDAO", "buscarSequenciaAtual", "alunoId=" + alunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT DISTINCT CAST(s.data AS date) FROM SessaoTreino s " +
                          "WHERE s.programacaoTreino.aluno.id = :alunoId " +
                          "AND s.concluido = true ORDER BY s.data DESC";
            List<java.sql.Date> datas = em.createQuery(jpql, java.sql.Date.class)
                                          .setParameter("alunoId", alunoId)
                                          .getResultList();
            if (datas.isEmpty()) return 0;

            long streak = 0;
            java.time.LocalDate hoje = java.time.LocalDate.now();
            for (java.sql.Date d : datas) {
                java.time.LocalDate ld = d.toLocalDate();
                if (streak == 0) {
                    if (ld.equals(hoje) || ld.equals(hoje.minusDays(1))) {
                        streak = 1;
                    } else {
                        return 0;
                    }
                } else {
                    java.time.LocalDate esperada = hoje.minusDays(streak);
                    if (ld.equals(esperada)) {
                        streak++;
                    } else {
                        break;
                    }
                }
            }
            return streak;
        } catch (Exception e) {
            return 0;
        } finally {
            em.close();
        }
    }

    public long contarAlunosAtivosNoMes() {
        EventBus.emit("TreinoDAO", "contarAlunosAtivosNoMes", "");
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            java.time.LocalDateTime inicio = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
            java.time.LocalDateTime fim = inicio.plusMonths(1);
            return em.createQuery("SELECT COUNT(DISTINCT s.programacaoTreino.aluno.id) FROM SessaoTreino s WHERE s.concluido = true AND s.data >= :inicio AND s.data < :fim", Long.class)
                     .setParameter("inicio", inicio)
                     .setParameter("fim", fim)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<com.mycompany.academia.treino.model.ComentarioTreino> listarFeedbacksRecentes(int limite) {
        EventBus.emit("TreinoDAO", "listarFeedbacksRecentes", "limite=" + limite);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT c FROM ComentarioTreino c " +
                          "JOIN FETCH c.treino t " +
                          "JOIN FETCH c.aluno a " +
                          "ORDER BY c.lido ASC, c.dataCriacao DESC";
            return em.createQuery(jpql, com.mycompany.academia.treino.model.ComentarioTreino.class)
                     .setMaxResults(limite)
                     .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public void marcarComentariosComoLidos(int alunoId) {
        EventBus.emit("TreinoDAO", "marcarComentariosComoLidos", "alunoId=" + alunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("UPDATE ComentarioTreino c SET c.lido = true WHERE c.aluno.id = :alunoId AND c.lido = false")
              .setParameter("alunoId", alunoId)
              .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.err.println("Erro ao atualizar status dos comentários: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}