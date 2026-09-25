package antifraud.servicoauditoria.dto;

public record ErroResponseDTO(
        int status,
        String erro,
        String mensagem
) { }
