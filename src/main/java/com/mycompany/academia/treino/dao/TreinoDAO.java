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

    public Treino inserirTreino(Treino pTreino) {
        EventBus.emit("TreinoDAO", "inserirTreino", "treino=" + pTreino.getNome());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (pTreino.getId() == 0) {
                em.persist(pTreino);
                EventBus.emit("PostgreSQL", "INSERT INTO treino (nome=" + pTreino.getNome() + ")", "persist");
                EventBus.emit("Entities", "Treino created", "nome=" + pTreino.getNome());
            } else {
                pTreino = em.merge(pTreino);
                EventBus.emit("PostgreSQL", "UPDATE treino SET ... WHERE id=" + pTreino.getId(), "merge");
                EventBus.emit("Entities", "Treino updated", "id=" + pTreino.getId());
            }
            em.getTransaction().commit();
            return pTreino; 
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

    public boolean inserirItemTreino(ItemTreino pItem) {
        EventBus.emit("TreinoDAO", "inserirItemTreino", "itemTreinoId=" + pItem.getId());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (pItem.getId() == 0) {
                em.persist(pItem);
                EventBus.emit("PostgreSQL", "INSERT INTO item_treino (treinoId=" + pItem.getTreino().getId() + ")", "persist");
            } else {
                em.merge(pItem);
                EventBus.emit("PostgreSQL", "UPDATE item_treino SET ... WHERE id=" + pItem.getId(), "merge");
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
    
    public boolean inserirProgramacao(ProgramacaoTreino pProgramacao) {
        EventBus.emit("TreinoDAO", "inserirProgramacao", "alunoId=" + pProgramacao.getAluno().getId());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(pProgramacao);
            EventBus.emit("PostgreSQL", "INSERT INTO programacao_treino (alunoId=" + pProgramacao.getAluno().getId() + ")", "persist");
            EventBus.emit("Entities", "ProgramacaoTreino created", "alunoId=" + pProgramacao.getAluno().getId());
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
    
    public List<ProgramacaoTreino> listarProgramacoesDoAluno(int pAlunoId) {
        EventBus.emit("TreinoDAO", "listarProgramacoesDoAluno", "alunoId=" + pAlunoId);
        EntityManager em = JPAUtil.getEntityManager();
        try {
            EventBus.emit("PostgreSQL", "SELECT FROM programacao_treino JOIN treino WHERE alunoId=" + pAlunoId, "query");
            EventBus.emit("Entities", "ProgramacaoTreino+Treino loaded", "alunoId=" + pAlunoId);
            List<ProgramacaoTreino> r = em.createQuery("SELECT p FROM ProgramacaoTreino p JOIN FETCH p.treino WHERE p.aluno.id = :alunoId", ProgramacaoTreino.class)
                     .setParameter("alunoId", pAlunoId)
                     .getResultList();
            EventBus.emit("JPA", "JPQL Query", "ProgramacaoTreino JOIN FETCH treino WHERE alunoId=" + pAlunoId);
            return r;
        } finally {
            em.close();
        }
    }

    public List<ItemTreino> listarItensDoTreino(int pTreinoId) {
        EventBus.emit("TreinoDAO", "listarItensDoTreino", "treinoId=" + pTreinoId);
        EntityManager em = JPAUtil.getEntityManager();
        try {
            EventBus.emit("PostgreSQL", "SELECT FROM item_treino JOIN exercicio WHERE treinoId=" + pTreinoId, "query");
            List<ItemTreino> r = em.createQuery("SELECT DISTINCT i FROM ItemTreino i JOIN FETCH i.exercicio LEFT JOIN FETCH i.seriesTreino WHERE i.treino.id = :treinoId", ItemTreino.class)
                     .setParameter("treinoId", pTreinoId)
                     .getResultList();
            EventBus.emit("JPA", "JPQL Query", "ItemTreino JOIN FETCH exercicio+series WHERE treinoId=" + pTreinoId);
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
    
    public List<String> listarNomesExerciciosDoAluno(int pAlunoId) {
        EventBus.emit("TreinoDAO", "listarNomesExerciciosDoAluno", "alunoId=" + pAlunoId);
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
            query.setParameter("alunoId", pAlunoId);
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
    
    public List<com.mycompany.academia.treino.model.ComentarioTreino> listarComentariosDoAluno(int pAlunoId) {
        EventBus.emit("TreinoDAO", "listarComentariosDoAluno", "alunoId=" + pAlunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT c FROM ComentarioTreino c " +
                          "JOIN FETCH c.treino t " +
                          "WHERE c.aluno.id = :alunoId " +
                          "ORDER BY c.dataCriacao DESC";
                          
            jakarta.persistence.TypedQuery<com.mycompany.academia.treino.model.ComentarioTreino> query = em.createQuery(jpql, com.mycompany.academia.treino.model.ComentarioTreino.class);
            query.setParameter("alunoId", pAlunoId);
            return query.getResultList();
        } catch (Exception e) {
            System.err.println("Erro ao buscar feedbacks do aluno: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        } finally {
            em.close();
        }
    }   

    public List<com.mycompany.academia.treino.model.ItemRealizado> listarItensRealizados(int pAlunoId, int pTreinoId, java.time.LocalDateTime pDataComentario) {
        EventBus.emit("TreinoDAO", "listarItensRealizados", "alunoId=" + pAlunoId + ", treinoId=" + pTreinoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpqlSessao = "SELECT s FROM SessaoTreino s " +
                                "WHERE s.programacaoTreino.aluno.id = :alunoId " +
                                "AND s.programacaoTreino.treino.id = :treinoId " +
                                "AND (SELECT COUNT(ir) FROM ItemRealizado ir WHERE ir.sessaoTreino = s) > 0 " + 
                                "ORDER BY s.data DESC";
            List<com.mycompany.academia.core.session.SessaoTreino> sessoes = em.createQuery(jpqlSessao, com.mycompany.academia.core.session.SessaoTreino.class)
                                           .setParameter("alunoId", pAlunoId)
                                           .setParameter("treinoId", pTreinoId)
                                           .getResultList();
            if (sessoes.isEmpty()) return java.util.Collections.emptyList();

            com.mycompany.academia.core.session.SessaoTreino oMelhorSessao = sessoes.get(0);
            long menorDiferenca = Math.abs(java.time.temporal.ChronoUnit.SECONDS.between(oMelhorSessao.getData(), pDataComentario));
            for (com.mycompany.academia.core.session.SessaoTreino s : sessoes) {
            long diferenca = Math.abs(java.time.temporal.ChronoUnit.SECONDS.between(s.getData(), pDataComentario));
                if (diferenca < menorDiferenca) {
                    menorDiferenca = diferenca;
                    oMelhorSessao = s;
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

    public List<com.mycompany.academia.treino.model.ItemRealizado> listarHistoricoCargas(int pAlunoId, String pNomeExercicio) {
        EventBus.emit("TreinoDAO", "listarHistoricoCargas", "alunoId=" + pAlunoId + ", exercicio=" + pNomeExercicio);
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
                     .setParameter("alunoId", pAlunoId)
                     .setParameter("nomeExercicio", pNomeExercicio)
                     .getResultList();
        } finally {
            em.close();
        }
    }

    public String buscarNomeFichaAtiva(int pAlunoId) {
        EventBus.emit("TreinoDAO", "buscarNomeFichaAtiva", "alunoId=" + pAlunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT pt.treino.nome FROM ProgramacaoTreino pt " +
                          "WHERE pt.aluno.id = :alunoId " +
                          "AND :hoje BETWEEN pt.dataInicioSemanas AND pt.dataFimSemanas";
            
            java.util.List<String> resultados = em.createQuery(jpql, String.class)
                                        .setParameter("alunoId", pAlunoId)
                                        .setParameter("hoje", java.time.LocalDateTime.now())
                                        .getResultList();
            
            return resultados.isEmpty() ? "Nenhuma Ativa" : resultados.get(0);
        } catch (Exception e) {
            return "Erro ao buscar";
        } finally {
            em.close();
        }
    }

    public String buscarDataUltimoTreino(int pAlunoId) {
        EventBus.emit("TreinoDAO", "buscarDataUltimoTreino", "alunoId=" + pAlunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT MAX(c.dataCriacao) FROM ComentarioTreino c WHERE c.treino IN (" +
                          "    SELECT pt.treino FROM ProgramacaoTreino pt WHERE pt.aluno.id = :alunoId" +
                          ")";
            
            java.time.LocalDateTime ultimaData = em.createQuery(jpql, java.time.LocalDateTime.class)
                                                   .setParameter("alunoId", pAlunoId)
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

    public long contarTreinosNoMes(int pAlunoId) {
        EventBus.emit("TreinoDAO", "contarTreinosNoMes", "alunoId=" + pAlunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            java.time.LocalDateTime inicioDoMes = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
            
            String jpql = "SELECT COUNT(c) FROM ComentarioTreino c WHERE c.treino IN (" +
                          "    SELECT pt.treino FROM ProgramacaoTreino pt WHERE pt.aluno.id = :alunoId" +
                          ") AND c.dataCriacao >= :inicioDoMes";
            
            return em.createQuery(jpql, Long.class)
                     .setParameter("alunoId", pAlunoId)
                     .setParameter("inicioDoMes", inicioDoMes)
                     .getSingleResult();
        } catch (Exception e) {
            return 0;
        } finally {
            em.close();
        }
    }
    
    public boolean excluirProgramacao(ProgramacaoTreino pProg) {
        EventBus.emit("TreinoDAO", "excluirProgramacao", "programacaoId=" + pProg.getId());
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            pProg = em.merge(pProg);
            
            Treino oTreino = pProg.getTreino();
            boolean isFichaExclusiva = !oTreino.isFichaPadrao();
            
            em.remove(pProg);
            
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
    
    public List<com.mycompany.academia.core.session.SessaoTreino> listarSessoesDoAluno(int pAlunoId) {
        EventBus.emit("TreinoDAO", "listarSessoesDoAluno", "alunoId=" + pAlunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT DISTINCT s FROM SessaoTreino s " +
                          "JOIN FETCH s.programacaoTreino pt " +
                          "JOIN FETCH pt.treino t " +
                          "WHERE pt.aluno.id = :alunoId " +
                          "AND s.concluido = true " +
                          "ORDER BY s.data DESC";
            return em.createQuery(jpql, com.mycompany.academia.core.session.SessaoTreino.class)
                     .setParameter("alunoId", pAlunoId)
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

    public long contarTotalTreinos(int pAlunoId) {
        EventBus.emit("TreinoDAO", "contarTotalTreinos", "alunoId=" + pAlunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT COUNT(c) FROM ComentarioTreino c WHERE c.treino IN (" +
                          "    SELECT pt.treino FROM ProgramacaoTreino pt WHERE pt.aluno.id = :alunoId" +
                          ")";
            return em.createQuery(jpql, Long.class)
                     .setParameter("alunoId", pAlunoId)
                     .getSingleResult();
        } catch (Exception e) {
            return 0;
        } finally {
            em.close();
        }
    }

    public java.util.Map<Integer, Long> listarTreinosPorSemana(int pAlunoId, int pSemanas) {
        EventBus.emit("TreinoDAO", "listarTreinosPorSemana", "alunoId=" + pAlunoId + ", semanas=" + pSemanas);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            java.time.LocalDate inicio = java.time.LocalDate.now().minusWeeks(pSemanas).with(java.time.DayOfWeek.MONDAY);
            String jpql = "SELECT s.data FROM SessaoTreino s " +
                          "WHERE s.programacaoTreino.aluno.id = :alunoId " +
                          "AND s.concluido = true AND s.data >= :inicio " +
                          "ORDER BY s.data ASC";
            List<java.time.LocalDateTime> datas = em.createQuery(jpql, java.time.LocalDateTime.class)
                                                    .setParameter("alunoId", pAlunoId)
                                                    .setParameter("inicio", inicio.atStartOfDay())
                                                    .getResultList();

            java.util.Map<Integer, Long> semMap = new java.util.LinkedHashMap<>();
            for (int i = 0; i < pSemanas; i++) {
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

    public long buscarSequenciaAtual(int pAlunoId) {
        EventBus.emit("TreinoDAO", "buscarSequenciaAtual", "alunoId=" + pAlunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT DISTINCT CAST(s.data AS date) FROM SessaoTreino s " +
                          "WHERE s.programacaoTreino.aluno.id = :alunoId " +
                          "AND s.concluido = true ORDER BY s.data DESC";
            List<java.sql.Date> datas = em.createQuery(jpql, java.sql.Date.class)
                                          .setParameter("alunoId", pAlunoId)
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

    public void marcarComentariosComoLidos(int pAlunoId) {
        EventBus.emit("TreinoDAO", "marcarComentariosComoLidos", "alunoId=" + pAlunoId);
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("UPDATE ComentarioTreino c SET c.lido = true WHERE c.aluno.id = :alunoId AND c.lido = false")
              .setParameter("alunoId", pAlunoId)
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