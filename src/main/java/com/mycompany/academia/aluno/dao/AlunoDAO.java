package com.mycompany.academia.aluno.dao;

import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.aluno.model.AvaliacaoFisica;
import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.config.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class AlunoDAO {

    public void salvarOuAtualizar(Aluno aluno) {
        EventBus.emit("AlunoDAO", "salvarOuAtualizar", "aluno=" + aluno.getNome());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (aluno.getId() == 0) {
                em.persist(aluno);
            } else {
                em.merge(aluno);
            }
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

    // Busca todos os alunos cadastrados (Para preencher a tabela do Dashboard)
    public List<Aluno> buscarTodos() {
        EventBus.emit("AlunoDAO", "buscarTodos", "");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT a FROM Aluno a";
            TypedQuery<Aluno> query = em.createQuery(jpql, Aluno.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public List<AvaliacaoFisica> buscarAvaliacoesPorAluno(int alunoId) {
        EventBus.emit("AlunoDAO", "buscarAvaliacoesPorAluno", "alunoId=" + alunoId);
        EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
    try {
        String jpql = "SELECT a FROM AvaliacaoFisica a WHERE a.aluno.id = :alunoId ORDER BY a.dataAvaliacao ASC";
        TypedQuery<AvaliacaoFisica> query = em.createQuery(jpql, AvaliacaoFisica.class);
        query.setParameter("alunoId", alunoId);
        EventBus.emit("JPA", "JPQL Query", "AvaliacaoFisica WHERE alunoId=" + alunoId);
        return query.getResultList();
    } finally {
        em.close();
    }
}

    public void salvarAvaliacaoFisica(com.mycompany.academia.aluno.model.AvaliacaoFisica avaliacao) {
        EventBus.emit("AlunoDAO", "salvarAvaliacaoFisica", "alunoId=" + avaliacao.getAluno().getId());
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            
            avaliacao.setAluno(em.merge(avaliacao.getAluno())); 
            
            em.persist(avaliacao);
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
    
    public void atualizarAvaliacaoFisica(com.mycompany.academia.aluno.model.AvaliacaoFisica avaliacao) {
        EventBus.emit("AlunoDAO", "atualizarAvaliacaoFisica", "alunoId=" + avaliacao.getAluno().getId());
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(avaliacao);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
    
    public long contarAlunos() {
        EventBus.emit("AlunoDAO", "contarAlunos", "");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(a) FROM Aluno a", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public void deletarAvaliacaoFisica(com.mycompany.academia.aluno.model.AvaliacaoFisica avaliacao) {
        EventBus.emit("AlunoDAO", "deletarAvaliacaoFisica", "avaliacaoId=" + avaliacao.getId());
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            avaliacao = em.merge(avaliacao); 
            em.remove(avaliacao);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}