-- Adiciona ON DELETE CASCADE em todas as FKs para permitir deleção em cascata

-- itemrealizado → itemtreino
ALTER TABLE itemrealizado DROP CONSTRAINT fkfxmjm66a2baurkhn6y94cjih8;
ALTER TABLE itemrealizado ADD CONSTRAINT fkfxmjm66a2baurkhn6y94cjih8
    FOREIGN KEY (item_treino_id) REFERENCES itemtreino(id) ON DELETE CASCADE;

-- serietreino → itemtreino
ALTER TABLE serietreino DROP CONSTRAINT fk4p79x6wjfdmgm3w9mmlpb7cjq;
ALTER TABLE serietreino ADD CONSTRAINT fk4p79x6wjfdmgm3w9mmlpb7cjq
    FOREIGN KEY (item_treino_id) REFERENCES itemtreino(id) ON DELETE CASCADE;

-- sessaotreino → programacaotreino
ALTER TABLE sessaotreino DROP CONSTRAINT fkk6xe6xuekx69uy6ggeha5qc7j;
ALTER TABLE sessaotreino ADD CONSTRAINT fkk6xe6xuekx69uy6ggeha5qc7j
    FOREIGN KEY (programacao_treino_id) REFERENCES programacaotreino(id) ON DELETE CASCADE;

-- comentariotreino → sessaotreino
ALTER TABLE comentariotreino DROP CONSTRAINT fk19u1cvi8jrcn6pi0g3fvegoeh;
ALTER TABLE comentariotreino ADD CONSTRAINT fk19u1cvi8jrcn6pi0g3fvegoeh
    FOREIGN KEY (sessao_treino_id) REFERENCES sessaotreino(id) ON DELETE CASCADE;

-- itemrealizado → sessaotreino
ALTER TABLE itemrealizado DROP CONSTRAINT fk84fgi4lsw3hoytyn02n75gnxr;
ALTER TABLE itemrealizado ADD CONSTRAINT fk84fgi4lsw3hoytyn02n75gnxr
    FOREIGN KEY (sessao_treino_id) REFERENCES sessaotreino(id) ON DELETE CASCADE;

-- comentariotreino → treino
ALTER TABLE comentariotreino DROP CONSTRAINT fkna1e2tpprjl1fv6ud0xqpr9h;
ALTER TABLE comentariotreino ADD CONSTRAINT fkna1e2tpprjl1fv6ud0xqpr9h
    FOREIGN KEY (treino_id) REFERENCES treino(id) ON DELETE CASCADE;

-- itemtreino → treino
ALTER TABLE itemtreino DROP CONSTRAINT fk5wmojo63xig8l2m2ollfw8wdx;
ALTER TABLE itemtreino ADD CONSTRAINT fk5wmojo63xig8l2m2ollfw8wdx
    FOREIGN KEY (treino_id) REFERENCES treino(id) ON DELETE CASCADE;

-- programacaotreino → treino
ALTER TABLE programacaotreino DROP CONSTRAINT fkhc5yqqrdv4t4h4qhiakocub8a;
ALTER TABLE programacaotreino ADD CONSTRAINT fkhc5yqqrdv4t4h4qhiakocub8a
    FOREIGN KEY (treino_id) REFERENCES treino(id) ON DELETE CASCADE;

-- avaliacao_fisica → aluno
ALTER TABLE avaliacao_fisica DROP CONSTRAINT fklpqo1wgju9ktawh8pgo3xjw7k;
ALTER TABLE avaliacao_fisica ADD CONSTRAINT fklpqo1wgju9ktawh8pgo3xjw7k
    FOREIGN KEY (aluno_id) REFERENCES aluno(id) ON DELETE CASCADE;

-- comentariotreino → aluno
ALTER TABLE comentariotreino DROP CONSTRAINT fkkyo0gqbj5ij6yguctveual3s5;
ALTER TABLE comentariotreino ADD CONSTRAINT fkkyo0gqbj5ij6yguctveual3s5
    FOREIGN KEY (aluno_id) REFERENCES aluno(id) ON DELETE CASCADE;

-- programacaotreino → aluno
ALTER TABLE programacaotreino DROP CONSTRAINT fkopkboicd6g0yg62jbic3p9xe6;
ALTER TABLE programacaotreino ADD CONSTRAINT fkopkboicd6g0yg62jbic3p9xe6
    FOREIGN KEY (aluno_id) REFERENCES aluno(id) ON DELETE CASCADE;
