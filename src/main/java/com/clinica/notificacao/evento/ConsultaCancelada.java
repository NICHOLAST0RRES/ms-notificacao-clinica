package com.clinica.notificacao.evento;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultaCancelada(
        UUID consultaId,
        String pacienteNome,
        String pacienteTelefone,
        OffsetDateTime  dataHoraOriginal,
        OffsetDateTime ocorridoEm
) {
}
