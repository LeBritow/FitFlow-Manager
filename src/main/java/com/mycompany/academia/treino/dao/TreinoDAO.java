package com.mycompany.academia.treino.dao;

import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.treino.model.ItemTreino;
import com.mycompany.academia.core.config.JPAUtil;
import com.mycompany.academia.treino.model.ProgramacaoTreino;
import com.mycompany.academia.treino.model.Treino;
import jakarta.persistence.EntityManager;
import java.util.List;

public class TreinoDAO {

    public Treino salvarTreino(Treino treino) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (treino.getId() == 0) {
                em.persist(treino);
            } else {
                treino = em.merge(treino);
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

    public boolean salvarItemTreino(ItemTreino item) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (item.getId() == 0) {
                em.persist(item);
            } else {
                em.merge(item);
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
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT a FROM Aluno a ORDER BY a.nome", Aluno.class).getResultList();
        } finally {
            em.close();
        }
    }
    
    public boolean salvarProgramacao(ProgramacaoTreino programacao) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(programacao);
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
    
    public List<ProgramacaoTreino> listarProgramacoesPorAluno(int alunoId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT p FROM ProgramacaoTreino p JOIN FETCH p.treino WHERE p.aluno.id = :alunoId", ProgramacaoTreino.class)
                     .setParameter("alunoId", alunoId)
                     .getResultList();
        } finally {
            em.close();
        }
    }

    public List<ItemTreino> listarItensPorTreino(int treinoId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT DISTINCT i FROM ItemTreino i JOIN FETCH i.exercicio LEFT JOIN FETCH i.seriesTreino WHERE i.treino.id = :treinoId", ItemTreino.class)
                     .setParameter("treinoId", treinoId)
                     .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<Treino> listarFichasPadrao() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT t FROM Treino t WHERE t.fichaPadrao = true ORDER BY t.nome", Treino.class)
                     .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<String> buscarNomesExerciciosPorAluno(int alunoId) {
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
    
    public List<com.mycompany.academia.treino.model.ComentarioTreino> buscarComentariosPorAluno(int alunoId) {
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

    public List<com.mycompany.academia.treino.model.ItemRealizado> buscarItensRealizados(int alunoId, int treinoId, java.time.LocalDateTime dataComentario) {
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

            com.mycompany.academia.core.session.SessaoTreino melhorSessao = sessoes.get(0);
            long menorDiferenca = Math.abs(java.time.temporal.ChronoUnit.SECONDS.between(melhorSessao.getData(), dataComentario));
            for (com.mycompany.academia.core.session.SessaoTreino s : sessoes) {
            long diferenca = Math.abs(java.time.temporal.ChronoUnit.SECONDS.between(s.getData(), dataComentario));
                if (diferenca < menorDiferenca) {
                    menorDiferenca = diferenca;
                    melhorSessao = s;
                }
            }

            String jpqlItens = "SELECT DISTINCT ir FROM ItemRealizado ir " +
                               "JOIN FETCH ir.itemTreino it " +
                               "JOIN FETCH it.exercicio e " +
                               "LEFT JOIN FETCH it.seriesTreino st " +
                               "WHERE ir.sessaoTreino.id = :sessaoId";
                               
            return em.createQuery(jpqlItens, com.mycompany.academia.treino.model.ItemRealizado.class)
                     .setParameter("sessaoId", melhorSessao.getId())
                     .getResultList();
        } finally {
            em.close();
        }
    }

    public List<com.mycompany.academia.treino.model.ItemRealizado> buscarHistoricoCargas(int alunoId, String nomeExercicio) {
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT ir FROM ItemRealizado ir " +
                          "JOIN FETCH ir.sessaoTreino s " +
                          "WHERE s.programacaoTreino.aluno.id = :alunoId " +
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
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT pt.treino.nome FROM ProgramacaoTreino pt " +
                          "WHERE pt.aluno.id = :alunoId " +
                          "AND :hoje BETWEEN pt.dataInicioSemanas AND pt.dataFimSemanas";
            
            java.util.List<String> resultados = em.createQuery(jpql, String.class)
                                        .setParameter("alunoId", alunoId)
                                        .setParameter("hoje", java.time.LocalDateTime.now())
                                        .getResultList();
            
            return resultados.isEmpty() ? "Nenhuma Ativa" : resultados.get(0);
        } catch (Exception e) {
            return "Erro ao buscar";
        } finally {
            em.close();
        }
    }

    public String buscarDataUltimoTreino(int alunoId) {
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

    public long buscarQuantidadeTreinosMes(int alunoId) {
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
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            prog = em.merge(prog);
            
            Treino treino = prog.getTreino();
            boolean isFichaExclusiva = !treino.isFichaPadrao();
            
            em.remove(prog); 
            
            if (isFichaExclusiva) {
                em.createQuery("DELETE FROM ItemTreino i WHERE i.treino.id = :tId")
                  .setParameter("tId", treino.getId())
                  .executeUpdate();
                em.remove(treino);
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
    
    public void marcarComentariosComoLidos(int alunoId) {
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