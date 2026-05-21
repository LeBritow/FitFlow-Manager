package com.mycompany.academia;

import jakarta.persistence.EntityManager;

public class AlunoDAO {

    public void salvar(Aluno aluno) {
        EntityManager em = JPAUtil.getEntityManager();
        
        try {
            // Inicia a transação (prepara o banco para receber dados)
            em.getTransaction().begin();
            
            // O persist pega o objeto Java e transforma no INSERT do SQL
            em.persist(aluno);
            
            // Confirma a gravação no banco
            em.getTransaction().commit();
            
        } catch (Exception e) {
            // Se der erro, desfaz qualquer alteração pela metade
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            // Sempre fecha o EntityManager para não vazar memória
            em.close();
        }
    }
}