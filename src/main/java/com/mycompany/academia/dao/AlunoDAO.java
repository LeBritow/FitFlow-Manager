package com.mycompany.academia.dao;

import com.mycompany.academia.model.Aluno;
import com.mycompany.academia.config.JPAUtil;
import jakarta.persistence.EntityManager;

public class AlunoDAO {

    public void salvar(Aluno aluno) {
        EntityManager em = JPAUtil.getEntityManager();
        
        try {
            em.getTransaction().begin();
            
            em.persist(aluno);
            
            em.getTransaction().commit();
            
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}