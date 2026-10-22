package com.clinica.notificacao.processamento;

import com.clinica.notificacao.TesteDeIntegracao;
import com.clinica.notificacao.processamento.RegistroDeEventos.Resultado;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistroDeEventosTest extends TesteDeIntegracao {

    private static final UUID CONSULTA = UUID.fromString("0199f000-0000-7000-8000-0000000000aa");

    @Autowired
    private RegistroDeEventos registro;

    @Test
    void mesmaMensagemDuasVezesGeraUmaNotificacaoSo() {
        Message mensagem = mensagem(UUID.randomUUID(), "{}");

        assertThat(registro.registrar(mensagem, "ConsultaAgendada", CONSULTA, as(10, 0), true))
                .isEqualTo(Resultado.REGISTRADO);
        assertThat(registro.registrar(mensagem, "ConsultaAgendada", CONSULTA, as(10, 0), true))
                .isEqualTo(Resultado.REPETIDO);

        assertThat(contar("notificacoes")).isEqualTo(1);
    }

    @Test
    void eventoDeEstadoMaisAntigoQueOUltimoEDescartado() {
        registrar("ConsultaAgendada", as(10, 0), true);
        registrar("ConsultaCancelada", as(11, 0), true);

        // O reagendamento aconteceu às 10h30, mas só chegou depois do cancelamento.
        assertThat(registrar("ConsultaReagendada", as(10, 30), true)).isEqualTo(Resultado.OBSOLETO);

        assertThat(ultimoTipo()).isEqualTo("ConsultaCancelada");
        assertThat(contar("notificacoes")).isEqualTo(2);
    }

    @Test
    void lembreteNaoMudaOEstadoDaConsulta() {
        registrar("ConsultaAgendada", as(10, 0), true);

        assertThat(registrar("LembreteDeConsulta", as(18, 0), false)).isEqualTo(Resultado.REGISTRADO);

        assertThat(ultimoTipo()).isEqualTo("ConsultaAgendada");
    }

    @Test
    void lembreteGeradoAntesDaUltimaMudancaDaConsultaEDescartado() {
        registrar("ConsultaAgendada", as(10, 0), true);
        registrar("ConsultaCancelada", as(18, 2), true);

        // O lembrete foi gerado às 18h01, um minuto antes do cancelamento, mas chegou depois dele.
        assertThat(registrar("LembreteDeConsulta", as(18, 1), false)).isEqualTo(Resultado.OBSOLETO);

        assertThat(contar("notificacoes")).isEqualTo(2);
    }

    @Test
    void falhaNoMeioDesfazTudoParaORetryConseguirReprocessar() {
        Message corpoInvalido = mensagem(UUID.randomUUID(), "isto não é json");

        assertThatThrownBy(() -> registro.registrar(corpoInvalido, "ConsultaAgendada", CONSULTA, as(10, 0), true))
                .isInstanceOf(DataAccessException.class);

        // Se a marca de "já processada" ficasse gravada, o retry acharia que é repetida e a notificação se perderia.
        assertThat(contar("mensagens_processadas")).isZero();
        assertThat(contar("ultimo_evento_por_consulta")).isZero();
    }

    @Test
    void mensagemSemMessageIdERecusada() {
        Message semId = MessageBuilder.withBody("{}".getBytes(StandardCharsets.UTF_8)).build();

        assertThatThrownBy(() -> registro.registrar(semId, "ConsultaAgendada", CONSULTA, as(10, 0), true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(contar("mensagens_processadas")).isZero();
    }

    private Resultado registrar(String tipo, OffsetDateTime ocorridoEm, boolean eventoDeEstado) {
        return registro.registrar(mensagem(UUID.randomUUID(), "{}"), tipo, CONSULTA, ocorridoEm, eventoDeEstado);
    }

    private String ultimoTipo() {
        return jdbc.queryForObject(
                "select tipo from ultimo_evento_por_consulta where consulta_id = ?", String.class, CONSULTA);
    }

    private static Message mensagem(UUID messageId, String corpo) {
        return MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(messageId.toString())
                .build();
    }

    // Um horário de Salvador no dia 09/10/2026.
    private static OffsetDateTime as(int hora, int minuto) {
        return OffsetDateTime.of(2026, 10, 9, hora, minuto, 0, 0, ZoneOffset.ofHours(-3));
    }
}