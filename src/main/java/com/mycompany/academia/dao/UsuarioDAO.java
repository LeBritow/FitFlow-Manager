package com.mycompany.academia.dao;

import com.mycompany.academia.config.JPAUtil;
import com.mycompany.academia.model.Usuario;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class UsuarioDAO {

    public Usuario autenticar(String login, String senha) {
        EntityManager em = JPAUtil.getEntityManager();
        
        try {
            String jpql = "SELECT u FROM Usuario u WHERE (u.email = :login OR u.cpf = :login) AND u.senha = :senha";
            TypedQuery<Usuario> query = em.createQuery(jpql, Usuario.class);
            
            query.setParameter("login", login);
            query.setParameter("senha", senha);
            
            return query.getSingleResult();
            
        } catch (NoResultException e) {
            return null; 
        } finally {
            em.close();
        }
    }
    
    public boolean atualizarSenhaPorEmail(String email, String novaSenha) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            
            String jpql = "SELECT u FROM Usuario u WHERE u.email = :email";
            TypedQuery<Usuario> query = em.createQuery(jpql, Usuario.class);
            query.setParameter("email", email);
            
            query.setMaxResults(1); 
            
            Usuario usuario = query.getSingleResult();
            
            usuario.setSenha(novaSenha);
            em.merge(usuario);
            
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
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM Usuario u", Usuario.class).getResultList();
        } finally {
            em.close();
        }
    }
    
    public boolean excluir(Usuario usuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            usuario = em.merge(usuario); 
            em.remove(usuario);
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
    
    public boolean salvar(Usuario usuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            
            if (usuario.getId() == 0) {
                em.persist(usuario);
            } else {
                em.merge(usuario);
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