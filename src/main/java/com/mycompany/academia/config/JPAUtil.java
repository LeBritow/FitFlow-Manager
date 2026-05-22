package com.mycompany.academia.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPAUtil {

    // O Factory é criado apenas uma vez quando a aplicação sobe
    private static final EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory("academia-pu");

    // Método que as outras classes vão chamar para pegar a conexão pronta
    public static EntityManager getEntityManager() {
        return FACTORY.createEntityManager();
    }
}