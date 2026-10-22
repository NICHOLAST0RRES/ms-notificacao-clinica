package com.clinica.notificacao.contrato;

import com.clinica.notificacao.TesteDeIntegracao;
import com.clinica.notificacao.config.RabbitMqConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ContratoDosEventosTest extends TesteDeIntegracao {
    @Autowired
    private RabbitTemplate rabbit;

    @ParameterizedTest(name = "{2}")
    @CsvSource({
            "consulta-agendada.json,    consulta.agendada,   ConsultaAgendada,   0199f000-0000-7000-8000-000000000001",
            "consulta-cancelada.json,   consulta.cancelada,  ConsultaCancelada,  0199f000-0000-7000-8000-000000000002",
            "consulta-reagendada.json,  consulta.reagendada, ConsultaReagendada, 0199f000-0000-7000-8000-000000000003",
            "lembrete-de-consulta.json, lembrete.gerado,     LembreteDeConsulta, 0199f000-0000-7000-8000-000000000004"
    })
    void consumidorLeOEventoDoJeitoQueODotNetPublica(String arquivo, String routingKey, String tipo, UUID consultaId)
            throws IOException {
        byte[] json = new ClassPathResource("contratos/" + arquivo).getContentAsByteArray();
        UUID messageId = UUID.randomUUID();

        // Publica como o relay do .NET publica: JSON cru, content type e MessageId.
        rabbit.send(RabbitMqConfig.EXCHANGE, routingKey, MessageBuilder.withBody(json)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(messageId.toString())
                .build());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jdbc.queryForList(
                        "select tipo from notificacoes where message_id = ? and consulta_id = ?",
                        String.class, messageId, consultaId))
                        .containsExactly(tipo));
    }
}