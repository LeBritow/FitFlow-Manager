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
            // JPQL: Busca o usuário onde o email ou o cpf seja igual ao login digitado
            String jpql = "SELECT u FROM Usuario u WHERE (u.email = :login OR u.cpf = :login) AND u.senha = :senha";
            TypedQuery<Usuario> query = em.createQuery(jpql, Usuario.class);
            
            // Substitui os parâmetros da query pelos valores que vieram da tela
            query.setParameter("login", login);
            query.setParameter("senha", senha);
            
            // Retorna o usuário se encontrar, ou dá erro se não achar nada
            return query.getSingleResult();
            
        } catch (NoResultException e) {
            // Se não encontrar ninguém com esses dados, retorna null
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
            
            // --- A SOLUÇÃO ENTRA AQUI ---
            // Força o banco a pegar apenas o 1º usuário que encontrar com esse email,
            // ignorando se houver duplicatas.
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
            // JPQL simples e direta: "Traga todos os usuários"
            return em.createQuery("SELECT u FROM Usuario u", Usuario.class).getResultList();
        } finally {
            em.close();
        }
    }
    
    public boolean excluir(Usuario usuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            // O merge garante que o Hibernate reconheça o objeto antes de apagar
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
            
            // Se o ID for 0, significa que é um usuário novo, então criamos (persist)
            if (usuario.getId() == 0) {
                em.persist(usuario);
            } else {
                // Se já tem ID, significa que estamos editando, então atualizamos (merge)
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