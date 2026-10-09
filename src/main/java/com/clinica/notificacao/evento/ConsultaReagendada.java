package com.clinica.notificacao.evento;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultaReagendada(UUID consultaId,
                                 String pacienteNome,
                                 String pacienteTelefone,
                                 String profissionalNome,
                                 OffsetDateTime dataHoraAnterior,
                                 OffsetDateTime dataHoraNova,
                                 OffsetDateTime ocorridoEm) {
}
