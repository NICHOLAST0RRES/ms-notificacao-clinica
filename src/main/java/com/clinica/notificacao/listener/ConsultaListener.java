package com.clinica.notificacao.listener;

import com.clinica.notificacao.config.RabbitMqConfig;
import com.clinica.notificacao.evento.ConsultaAgendada;
import com.clinica.notificacao.evento.ConsultaCancelada;
import com.clinica.notificacao.evento.ConsultaReagendada;
import com.clinica.notificacao.evento.LembreteDeConsulta;
import com.clinica.notificacao.processamento.RegistroDeEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class ConsultaListener {
    private static final Logger log = LoggerFactory.getLogger(ConsultaListener.class);

    private final RegistroDeEventos registro;

    public ConsultaListener(RegistroDeEventos registro) {
        this.registro = registro;
    }

    @RabbitListener(queues = RabbitMqConfig.FILA_AGENDADA)
    public void aoAgendar(ConsultaAgendada evento, Message mensagem) {
        processar(mensagem, "ConsultaAgendada", evento.consultaId(), evento.ocorridoEm(), true);
    }

    @RabbitListener(queues = RabbitMqConfig.FILA_CANCELADA)
    public void aoCancelar(ConsultaCancelada evento, Message mensagem) {
        processar(mensagem, "ConsultaCancelada", evento.consultaId(), evento.ocorridoEm(), true);
    }

    @RabbitListener(queues = RabbitMqConfig.FILA_REAGENDADA)
    public void aoReagendar(ConsultaReagendada evento, Message mensagem) {
        processar(mensagem, "ConsultaReagendada", evento.consultaId(), evento.ocorridoEm(), true);
    }

    @RabbitListener(queues = RabbitMqConfig.FILA_LEMBRETE)
    public void aoLembrar(LembreteDeConsulta evento, Message mensagem) {
        processar(mensagem, "LembreteDeConsulta", evento.consultaId(), evento.ocorridoEm(), false);
    }

    private void processar(Message mensagem, String tipo, UUID consultaId,
                           OffsetDateTime ocorridoEm, boolean eventoDeEstado) {
        String messageId = mensagem.getMessageProperties().getMessageId();

        switch (registro.registrar(mensagem, tipo, consultaId, ocorridoEm, eventoDeEstado)) {
            case REGISTRADO -> log.info("{} da consulta {} registrado (mensagem {})", tipo, consultaId, messageId);
            case REPETIDO   -> log.info("Mensagem {} repetida, ignorada", messageId);
            case OBSOLETO   -> log.info("{} da consulta {} é mais antigo que o último processado, ignorado (mensagem {})",
                    tipo, consultaId, messageId);
        }
    }
}