package com.clinica.notificacao.listener;

import com.clinica.notificacao.config.RabbitMqConfig;
import com.clinica.notificacao.evento.ConsultaAgendada;
import com.clinica.notificacao.evento.ConsultaCancelada;
import com.clinica.notificacao.evento.ConsultaReagendada;
import com.clinica.notificacao.evento.LembreteDeConsulta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsultaListener {
    private static final Logger log = LoggerFactory.getLogger(ConsultaListener.class);

    @RabbitListener(queues = RabbitMqConfig.FILA_AGENDADA)
    public void aoAgendar(ConsultaAgendada evento) {
        log.info("Consulta agendada {} — paciente {} ({}), com {} em {}",
                evento.consultaId(), evento.pacienteNome(), evento.pacienteTelefone(),
                evento.profissionalNome(), evento.dataHora());
    }

    @RabbitListener(queues = RabbitMqConfig.FILA_CANCELADA)
    public void aoCancelar(ConsultaCancelada evento) {
        log.info("Consulta cancelada {} — paciente {} ({}), horario {}",
                evento.consultaId(), evento.pacienteNome(), evento.pacienteTelefone(),
                evento.dataHoraOriginal());
    }

    @RabbitListener(queues = RabbitMqConfig.FILA_REAGENDADA)
    public void aoReagendar(ConsultaReagendada evento) {
        log.info("Consulta reagendada {} — paciente {} ({}), de {} para {}",
                evento.consultaId(), evento.pacienteNome(), evento.pacienteTelefone(),
                evento.dataHoraAnterior(), evento.dataHoraNova());
    }

    @RabbitListener(queues = RabbitMqConfig.FILA_LEMBRETE)
    public void aoLembrar(LembreteDeConsulta evento) {
        log.info("Lembrete da consulta {} — paciente {} ({}), em {}",
                evento.consultaId(), evento.pacienteNome(), evento.pacienteTelefone(),
                evento.dataHora());
    }

}
