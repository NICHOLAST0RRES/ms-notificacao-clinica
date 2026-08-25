package com.clinica.notificacao.evento;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultaReagendada(UUID consultaId,
                                 String pacienteNome,
                                 String pacienteTelefone,
                                 String profissionalNome,
                                 LocalDateTime dataHoraAnterior,
                                 LocalDateTime dataHoraNova) {
}
