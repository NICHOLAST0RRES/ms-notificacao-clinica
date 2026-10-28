package com.clinica.notificacao;


import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(InfraDeTeste.class)
public abstract class TesteDeIntegracao {

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.execute("truncate table notificacoes, mensagens_processadas, ultimo_evento_por_consulta");
    }

    protected int contar(String tabela) {
        Integer total = jdbc.queryForObject("select count(*) from " + tabela, Integer.class);
        return total == null ? 0 : total;
    }
}
