package com.mycompany.academia.treino.dao;

import com.mycompany.academia.core.config.JPAUtil;
import com.mycompany.academia.treino.model.Objetivo;
import jakarta.persistence.EntityManager;
import java.util.List;

public class ObjetivoDAO {

    public void inserir(Objetivo objetivo) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(objetivo);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public List<Objetivo> listarTodos() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT o FROM Objetivo o ORDER BY o.nome", Objetivo.class).getResultList();
        } finally {
            em.close();
        }
    }
}
