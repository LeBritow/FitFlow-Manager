package com.mycompany.academia;

import jakarta.persistence.EntityManager;

public class SetupBanco {

    public static void main(String[] args) {
        System.out.println("Iniciando a inserção de usuários no banco...");
        
        EntityManager em = JPAUtil.getEntityManager();
        
        try {
            em.getTransaction().begin();
            
            // 1. Criando o seu usuário de Aluno
            Aluno aluno = new Aluno();
            aluno.setNome("Gustavo Silva Guimarães de Brito");
            aluno.setEmail("gustavo@email.com");
            aluno.setCpf("11122233344");
            aluno.setSenha("senha123");
            aluno.setPeso(78.5f);
            aluno.setAltura(1.75f);
            aluno.setImc(25.6f);
            
            em.persist(aluno);
            
            // 2. Criando um usuário Administrador (Admin)
            Admin admin = new Admin();
            admin.setNome("Administrador Chefe");
            admin.setEmail("admin@academia.com");
            admin.setCpf("00000000000");
            admin.setSenha("admin123");
            
            em.persist(admin);
            
            em.getTransaction().commit();
            System.out.println("Sucesso! Aluno e Admin cadastrados no PostgreSQL.");
            
        } catch (Exception e) {
            em.getTransaction().rollback();
            System.err.println("Erro ao salvar usuários:");
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}