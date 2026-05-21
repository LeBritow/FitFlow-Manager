package com.mycompany.academia;

import jakarta.persistence.EntityManager;
import java.util.List;

public class TreinoDAO {

    // Guarda o cabeçalho do treino (Nome e Objetivo) e devolve o objeto com o ID gerado
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
            return treino; // Devolvemos o treino para usar o ID dele nos Itens
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

    // Guarda cada exercício que foi adicionado à ficha
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

    // Procura exclusivamente os utilizadores que são Alunos para o menu pendente
    public List<Aluno> listarAlunos() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT a FROM Aluno a ORDER BY a.nome", Aluno.class).getResultList();
        } finally {
            em.close();
        }
    }
    
    // Guarda a ligação entre o Aluno e o Treino
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
    
    // Busca todas as programações (fichas) que pertencem a um aluno específico
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
            return em.createQuery("SELECT i FROM ItemTreino i JOIN FETCH i.exercicio WHERE i.treino.id = :treinoId", ItemTreino.class)
                     .setParameter("treinoId", treinoId)
                     .getResultList();
        } finally {
            em.close();
        }
    }
    
    // Busca todos os treinos que foram marcados como Templates (Fichas Padrão)
    public List<Treino> listarFichasPadrao() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT t FROM Treino t WHERE t.fichaPadrao = true ORDER BY t.nome", Treino.class)
                     .getResultList();
        } finally {
            em.close();
        }
    }
    
    
}