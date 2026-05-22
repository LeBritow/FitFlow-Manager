package com.mycompany.academia.dao;

import com.mycompany.academia.model.Aluno;
import com.mycompany.academia.model.ItemTreino;
import com.mycompany.academia.config.JPAUtil;
import com.mycompany.academia.model.ProgramacaoTreino;
import com.mycompany.academia.model.Treino;
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

    // Guarda o exercício e, graças ao CascadeType.ALL, guarda as Séries automaticamente!
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

    // AQUI ESTÁ A GRANDE MUDANÇA: Adicionado o "LEFT JOIN FETCH i.seriesTreino" para trazer as linhas das séries junto com o exercício
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
}