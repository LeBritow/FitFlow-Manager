package com.mycompany.academia.admin.dao;

import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.config.JPAUtil;
import com.mycompany.academia.admin.model.Usuario;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class UsuarioDAO {

    public Usuario autenticar(String pLogin, String pSenha) {
        EventBus.emit("UsuarioDAO", "autenticar", "login=" + pLogin);
        EntityManager em = JPAUtil.getEntityManager();
        
        try {
            String jpql = "SELECT u FROM Usuario u WHERE (u.email = :login OR u.cpf = :login) AND u.senha = :senha";
            TypedQuery<Usuario> query = em.createQuery(jpql, Usuario.class);
            
            query.setParameter("login", pLogin);
            query.setParameter("senha", pSenha);
            EventBus.emit("JPA", "JPQL Query", "SELECT Usuario WHERE email OR cpf = :login");
            
            return query.getSingleResult();
            
        } catch (NoResultException e) {
            return null; 
        } finally {
            em.close();
        }
    }
    
    public boolean atualizarSenhaPorEmail(String pEmail, String pNovaSenha) {
        EventBus.emit("UsuarioDAO", "atualizarSenhaPorEmail", "email=" + pEmail);
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            
            String jpql = "SELECT u FROM Usuario u WHERE u.email = :email";
            TypedQuery<Usuario> query = em.createQuery(jpql, Usuario.class);
            query.setParameter("email", pEmail);
            
            query.setMaxResults(1); 
            
            Usuario oUsuario = query.getSingleResult();
            
            oUsuario.setSenha(pNovaSenha);
            em.merge(oUsuario);
            
            em.getTransaction().commit();
            return true;
            
        } catch (NoResultException e) {
            return false; 
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
    
    public List<Usuario> listarTodos() {
        EventBus.emit("UsuarioDAO", "listarTodos", "");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM Usuario u", Usuario.class).getResultList();
        } finally {
            em.close();
        }
    }
    
    public boolean excluir(Usuario pUsuario) {
        EventBus.emit("UsuarioDAO", "excluir", "usuario=" + pUsuario.getNome());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            pUsuario = em.merge(pUsuario);

            em.remove(pUsuario);
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
    
    public boolean inserir(Usuario pUsuario) {
        EventBus.emit("UsuarioDAO", "inserir", "usuario=" + pUsuario.getNome());
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            
            if (pUsuario.getId() == 0) {
                em.persist(pUsuario);
            } else {
                em.merge(pUsuario);
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
}