package com.clinica.notificacao.processamento;


import org.springframework.amqp.core.Message;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RegistroDeEventos {
    private final JdbcTemplate jdbc;

    public RegistroDeEventos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Registra o evento uma única vez.
     * Retorna false se a mensagem já tinha sido processada (repetição).
     */
    @Transactional
    public boolean registrar(Message mensagem, String tipo, UUID consultaId) {
        String idDaMensagem = mensagem.getMessageProperties().getMessageId();
        if (idDaMensagem == null) {
            throw new IllegalArgumentException("Mensagem sem MessageId não pode ser processada com segurança");
        }

        UUID messageId = UUID.fromString(idDaMensagem);

        int inseridas = jdbc.update("""
                insert into mensagens_processadas (message_id, tipo, recebida_em)
                values (?, ?, now())
                on conflict (message_id) do nothing
                """, messageId, tipo);

        if (inseridas == 0) {
            return false;
        }

        String corpo = new String(mensagem.getBody(), StandardCharsets.UTF_8);

        jdbc.update("""
                insert into notificacoes (id, message_id, consulta_id, tipo, evento, status, criada_em)
                values (?, ?, ?, ?, ?::jsonb, 'PENDENTE', now())
                """, UUID.randomUUID(), messageId, consultaId, tipo, corpo);

        return true;
    }
}
