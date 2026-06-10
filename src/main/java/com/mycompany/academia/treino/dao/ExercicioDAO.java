package com.mycompany.academia.treino.dao;

import com.mycompany.academia.treino.model.Exercicio;
import com.mycompany.academia.core.config.JPAUtil;
import jakarta.persistence.EntityManager;
import java.util.List;

public class ExercicioDAO {

    public boolean salvar(Exercicio exercicio) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (exercicio.getId() == 0) {
                em.persist(exercicio);
            } else {
                em.merge(exercicio);
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
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT e FROM Exercicio e ORDER BY e.grupoMuscular, e.nome", Exercicio.class).getResultList();
        } finally {
            em.close();
        }
    }

    public boolean excluir(Exercicio exercicio) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            exercicio = em.merge(exercicio);
            em.remove(exercicio);
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
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(e) FROM Exercicio e", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Exercicio> buscarPorGrupoMuscular(String grupo) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT e FROM Exercicio e WHERE e.grupoMuscular = :grupo", Exercicio.class)
                     .setParameter("grupo", grupo)
                     .getResultList();
        } finally {
            em.close();
        }
    }
}