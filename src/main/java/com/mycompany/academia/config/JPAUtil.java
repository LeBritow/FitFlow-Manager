package com.mycompany.academia.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPAUtil {

    private static final EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory("academia-pu");

    public static EntityManager getEntityManager() {
        return FACTORY.createEntityManager();
    }
}