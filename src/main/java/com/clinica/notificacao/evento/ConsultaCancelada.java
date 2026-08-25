package com.clinica.notificacao.evento;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultaCancelada(
        UUID consultaId,
        String pacienteNome,
        String pacienteTelefone,
        LocalDateTime dataHoraOriginal
) {
}
