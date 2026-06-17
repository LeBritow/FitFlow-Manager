package com.mycompany.academia.treino.dao;

import com.mycompany.academia.treino.model.Exercicio;
import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.config.JPAUtil;
import jakarta.persistence.EntityManager;
import java.util.List;

public class ExercicioDAO {

    public boolean inserir(Exercicio pExercicio) {
        EventBus.emit("ExercicioDAO", "salvar", "exercicio=" + pExercicio.getNome());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (pExercicio.getId() == 0) {
                em.persist(pExercicio);
            } else {
                em.merge(pExercicio);
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

    public List<Exercicio> listarTodos() {
        EventBus.emit("ExercicioDAO", "listarTodos", "");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT e FROM Exercicio e ORDER BY e.grupoMuscular, e.nome", Exercicio.class).getResultList();
        } finally {
            em.close();
        }
    }

    public boolean excluir(Exercicio pExercicio) {
        EventBus.emit("ExercicioDAO", "excluir", "exercicio=" + pExercicio.getNome());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            pExercicio = em.merge(pExercicio);
            em.remove(pExercicio);
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
    
    public long contarExercicios() {
        EventBus.emit("ExercicioDAO", "contarExercicios", "");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(e) FROM Exercicio e", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Exercicio> listarPorGrupoMuscular(String pGrupo) {
        EventBus.emit("ExercicioDAO", "listarPorGrupoMuscular", "grupo=" + pGrupo);
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT e FROM Exercicio e WHERE e.grupoMuscular = :grupo", Exercicio.class)
                     .setParameter("grupo", pGrupo)
                     .getResultList();
        } finally {
            em.close();
        }
    }
}