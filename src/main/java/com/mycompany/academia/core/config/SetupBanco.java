package com.mycompany.academia.core.config;

import com.mycompany.academia.admin.model.Admin;
import jakarta.persistence.EntityManager;

public class SetupBanco {

    public static void main(String[] args) {
        System.out.println("Iniciando a inserção do administrador no banco...");
        
        EntityManager em = JPAUtil.getEntityManager();
        
        try {
            em.getTransaction().begin();
            
            Admin admin = new Admin();
            admin.setNome("Administrador");
            admin.setEmail("admin");
            admin.setCpf("00000000000");
            admin.setSenha("admin123");
            
            em.persist(admin);
            
            em.getTransaction().commit();
            System.out.println("Sucesso! Administrador cadastrado no PostgreSQL.");
            
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.err.println("Erro ao salvar administrador:");
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}