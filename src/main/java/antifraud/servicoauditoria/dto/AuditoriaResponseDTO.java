package antifraud.servicoauditoria.dto;

import antifraud.servicoauditoria.enums.NivelRisco;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AuditoriaResponseDTO(
        String id,
        UUID transacaoId,
        int pontuacao,
        NivelRisco nivel,
        List<String> regrasDisparadas,
        LocalDateTime analisadoEm,
        LocalDateTime registradoEm
) { }
