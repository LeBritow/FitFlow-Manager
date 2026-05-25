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
            // A mágica: Uma subquery (SELECT pt.treino...) descobre quais são os treinos do aluno,
            // e a query principal pega os itens (exercícios) que pertencem a esses treinos.
            String jpql = "SELECT DISTINCT e.nome FROM ItemTreino it " +
                          "JOIN it.exercicio e " +
                          "WHERE it.treino IN (" +
                          "    SELECT pt.treino FROM ProgramacaoTreino pt WHERE pt.aluno.id = :alunoId" +
                          ")";
                          
            jakarta.persistence.TypedQuery<String> query = em.createQuery(jpql, String.class);
            query.setParameter("alunoId", alunoId);
            return query.getResultList();
        } catch (Exception e) {
            System.err.println("Erro ao buscar exercícios do aluno: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList(); // Retorna lista vazia para não quebrar a tela
        } finally {
            em.close();
        }
    }
    
    public List<String> buscarComentariosPorAluno(int alunoId) {
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT CONCAT('[', t.nome, '] ', c.texto) FROM ComentarioTreino c " +
                          "JOIN c.treino t " +
                          "WHERE t IN (" +
                          "    SELECT pt.treino FROM ProgramacaoTreino pt WHERE pt.aluno.id = :alunoId" +
                          ")";
                          
            jakarta.persistence.TypedQuery<String> query = em.createQuery(jpql, String.class);
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

    // 1. CARD: Ficha Ativa (Busca o nome do treino planejado para o período atual)
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

    // 2. CARD: Último Treino (Busca a data do último feedback enviado pelo app mobile)
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

    // 3. CARD: Treinos no Mês (Conta quantos treinos foram concluídos no mês atual)
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
    
    // Método para excluir uma ProgramacaoTreino (Ficha do Aluno)
    public boolean excluirProgramacao(ProgramacaoTreino prog) {
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            prog = em.merge(prog);
            
            Treino treino = prog.getTreino();
            boolean isFichaExclusiva = !treino.isFichaPadrao();
            
            // 1. Remove a atribuição da ficha ao aluno
            em.remove(prog); 
            
            // 2. Se for uma ficha feita só para ele, apaga os itens e o treino base também
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
}