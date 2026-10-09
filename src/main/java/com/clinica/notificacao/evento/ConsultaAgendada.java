package com.clinica.notificacao.evento;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultaAgendada(
        UUID consultaId,
        String pacienteNome,
        String pacienteTelefone,
        String profissionalNome,
        OffsetDateTime dataHora,
        OffsetDateTime ocorridoEm
) {

}
