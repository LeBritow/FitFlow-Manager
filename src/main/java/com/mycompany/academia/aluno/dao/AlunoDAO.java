package com.mycompany.academia.aluno.dao;

import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.aluno.model.AvaliacaoFisica;
import com.mycompany.academia.core.config.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class AlunoDAO {

    // Salva um novo ou atualiza um existente
    public void salvarOuAtualizar(Aluno aluno) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            // CORREÇÃO: Como o ID é 'int', checamos se é igual a 0
            if (aluno.getId() == 0) {
                em.persist(aluno); // Cria novo
            } else {
                em.merge(aluno); // Atualiza existente
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

    // Busca um aluno específico pelo ID
    public Aluno buscarPorId(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Aluno.class, id);
        } finally {
            em.close();
        }
    }

    // Busca todos os alunos cadastrados (Para preencher a tabela do Dashboard)
    public List<Aluno> buscarTodos() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT a FROM Aluno a";
            TypedQuery<Aluno> query = em.createQuery(jpql, Aluno.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    // Exclui um aluno do banco
    public void excluir(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Aluno aluno = em.find(Aluno.class, id);
            if (aluno != null) {
                em.remove(aluno);
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
    
    public List<AvaliacaoFisica> buscarAvaliacoesPorAluno(int alunoId) {
    EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
    try {
        String jpql = "SELECT a FROM AvaliacaoFisica a WHERE a.aluno.id = :alunoId ORDER BY a.dataAvaliacao ASC";
        TypedQuery<AvaliacaoFisica> query = em.createQuery(jpql, AvaliacaoFisica.class);
        query.setParameter("alunoId", alunoId);
        return query.getResultList();
    } finally {
        em.close();
    }
}

    // Salva a nova avaliação garantindo que o Aluno seja reconhecido na transação atual
    public void salvarAvaliacaoFisica(com.mycompany.academia.aluno.model.AvaliacaoFisica avaliacao) {
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            
            // Reanexa o aluno à sessão do banco antes de salvar para evitar erros de "Detached Entity"
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
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(avaliacao); // O merge atualiza o registro existente com base no ID
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
    
    // 1. CARD: Ficha Ativa (Busca o nome do treino planejado para o período atual)
    public String buscarNomeFichaAtiva(int alunoId) {
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT pt.treino.nome FROM ProgramacaoTreino pt " +
                          "WHERE pt.aluno.id = :alunoId " +
                          "AND :hoje BETWEEN pt.dataInicioSemanas AND pt.dataFimSemanas";
            
            List<String> resultados = em.createQuery(jpql, String.class)
                                        .setParameter("alunoId", alunoId)
                                        .setParameter("hoje", java.time.LocalDateTime.now())
                                        .getResultList();
            
            return resultados.isEmpty() ? "Nenhuma Ativa" : resultados.get(0);
        } finally {
            em.close();
        }
    }

    // 2. CARD: Último Treino (Busca a data do último feedback enviado pelo app mobile)
    public String buscarDataUltimoTreino(int alunoId) {
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            // Nota: Se a sua classe ComentarioTreino ou ItemRealizado tiver um atributo LocalDateTime chamado 'data',
            // nós buscamos o registro mais recente (MAX). Aqui usaremos o ComentarioTreino como base:
            String jpql = "SELECT MAX(c.dataCriacao) FROM ComentarioTreino c WHERE c.treino IN (" +
                          "    SELECT pt.treino FROM ProgramacaoTreino pt WHERE pt.aluno.id = :alunoId" +
                          ")";
            
            java.time.LocalDateTime ultimaData = em.createQuery(jpql, java.time.LocalDateTime.class)
                                                   .setParameter("alunoId", alunoId)
                                                   .getSingleResult();
            
            if (ultimaData == null) return "Sem registros";
            
            java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return ultimaData.format(formatador);
        } catch (Exception e) {
            return "Não realizado";
        } finally {
            em.close();
        }
    }

    // 3. CARD: Treinos no Mês (Conta quantos treinos foram concluídos no mês atual)
    public long buscarQuantidadeTreinosMes(int alunoId) {
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            java.time.LocalDateTime inicioDoMes = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
            
            String jpql = "SELECT COUNT(c) FROM ComentarioTreino c WHERE c.treino IN (" +
                          "    SELECT pt.treino FROM ProgramacaoTreino pt WHERE pt.aluno.id = :alunoId" +
                          ") AND c.dataCriacao >= :inicioDoMes";
            
            return em.createQuery(jpql, Long.class)
                     .setParameter("alunoId", alunoId)
                     .setParameter("inicioDoMes", inicioDoMes)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }
    
    public void deletarAvaliacaoFisica(com.mycompany.academia.aluno.model.AvaliacaoFisica avaliacao) {
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            // Dá um merge antes para garantir que o Hibernate sabe qual objeto estamos apagando
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