package com.mycompany.academia.aluno.dao;

import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.aluno.model.AvaliacaoFisica;
import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.config.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class AlunoDAO {

    public void inserirOuAtualizar(Aluno pAluno) {
        EventBus.emit("AlunoDAO", "inserirOuAtualizar", "aluno=" + pAluno.getNome());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (pAluno.getId() == 0) {
                em.persist(pAluno);
            } else {
                em.merge(pAluno);
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

    // Busca todos os alunos cadastrados
    public List<Aluno> listarTodos() {
        EventBus.emit("AlunoDAO", "listarTodos", "");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT a FROM Aluno a";
            TypedQuery<Aluno> query = em.createQuery(jpql, Aluno.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public List<AvaliacaoFisica> listarAvaliacoesDoAluno(int pAlunoId) {
        EventBus.emit("AlunoDAO", "listarAvaliacoesDoAluno", "alunoId=" + pAlunoId);
        EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
    try {
        String jpql = "SELECT a FROM AvaliacaoFisica a WHERE a.aluno.id = :alunoId ORDER BY a.dataAvaliacao ASC";
        TypedQuery<AvaliacaoFisica> query = em.createQuery(jpql, AvaliacaoFisica.class);
        query.setParameter("alunoId", pAlunoId);
        EventBus.emit("JPA", "JPQL Query", "AvaliacaoFisica WHERE alunoId=" + pAlunoId);
        return query.getResultList();
    } finally {
        em.close();
    }
}

    public void inserirAvaliacaoFisica(com.mycompany.academia.aluno.model.AvaliacaoFisica pAvaliacao) {
        EventBus.emit("AlunoDAO", "inserirAvaliacaoFisica", "alunoId=" + pAvaliacao.getAluno().getId());
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            
            pAvaliacao.setAluno(em.merge(pAvaliacao.getAluno())); 
            
            em.persist(pAvaliacao);
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
    
    public void atualizarAvaliacaoFisica(com.mycompany.academia.aluno.model.AvaliacaoFisica pAvaliacao) {
        EventBus.emit("AlunoDAO", "atualizarAvaliacaoFisica", "alunoId=" + pAvaliacao.getAluno().getId());
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(pAvaliacao);
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

    public void excluirAvaliacaoFisica(com.mycompany.academia.aluno.model.AvaliacaoFisica pAvaliacao) {
        EventBus.emit("AlunoDAO", "excluirAvaliacaoFisica", "avaliacaoId=" + pAvaliacao.getId());
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            pAvaliacao = em.merge(pAvaliacao); 
            em.remove(pAvaliacao);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}