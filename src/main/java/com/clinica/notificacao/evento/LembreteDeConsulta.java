package com.clinica.notificacao.evento;

import java.time.LocalDateTime;
import java.util.UUID;

public record LembreteDeConsulta(
        UUID consultaId,
        String pacienteNome,
        String pacienteTelefone,
        String profissionalNome,
        LocalDateTime dataHora
) {


}
