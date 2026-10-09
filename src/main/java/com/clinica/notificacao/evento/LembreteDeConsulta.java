package com.clinica.notificacao.evento;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LembreteDeConsulta(
        UUID consultaId,
        String pacienteNome,
        String pacienteTelefone,
        String profissionalNome,
        OffsetDateTime dataHora,
        OffsetDateTime ocorridoEm
) {


}
