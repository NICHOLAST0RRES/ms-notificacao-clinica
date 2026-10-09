package com.clinica.notificacao.processamento;


import org.springframework.amqp.core.Message;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RegistroDeEventos {

    public enum Resultado { REGISTRADO, REPETIDO, OBSOLETO }

    private final JdbcTemplate jdbc;

    public RegistroDeEventos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Resultado registrar(Message mensagem, String tipo, UUID consultaId,
                               OffsetDateTime ocorridoEm, boolean eventoDeEstado) {
        String idDaMensagem = mensagem.getMessageProperties().getMessageId();
        if (idDaMensagem == null) {
            throw new IllegalArgumentException("Mensagem sem MessageId não pode ser processada com segurança");
        }
        UUID messageId = UUID.fromString(idDaMensagem);

        // 1. Idempotência: a mesma mensagem só passa uma vez
        int inseridas = jdbc.update("""
                insert into mensagens_processadas (message_id, tipo, recebida_em)
                values (?, ?, now())
                on conflict (message_id) do nothing
                """, messageId, tipo);

        if (inseridas == 0) {
            return Resultado.REPETIDO;
        }

        // 2. Ordem: evento de estado mais antigo que o último da consulta é descartado
        if (eventoDeEstado && ocorridoEm != null) {
            int atualizadas = jdbc.update("""
                    insert into ultimo_evento_por_consulta (consulta_id, ocorrido_em, tipo)
                    values (?, ?, ?)
                    on conflict (consulta_id) do update
                        set ocorrido_em = excluded.ocorrido_em, tipo = excluded.tipo
                        where excluded.ocorrido_em >= ultimo_evento_por_consulta.ocorrido_em
                    """, consultaId, ocorridoEm, tipo);

            if (atualizadas == 0) {
                return Resultado.OBSOLETO;
            }
        }

        // 3. Efeito: notificação pendente
        String corpo = new String(mensagem.getBody(), StandardCharsets.UTF_8);
        jdbc.update("""
                insert into notificacoes (id, message_id, consulta_id, tipo, evento, status, criada_em)
                values (?, ?, ?, ?, ?::jsonb, 'PENDENTE', now())
                """, UUID.randomUUID(), messageId, consultaId, tipo, corpo);

        return Resultado.REGISTRADO;
    }
}
