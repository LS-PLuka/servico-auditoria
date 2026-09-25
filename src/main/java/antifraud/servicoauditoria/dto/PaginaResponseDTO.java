package antifraud.servicoauditoria.dto;

import java.util.List;

public record PaginaResponseDTO<T>(
        List<T> conteudo,
        int paginaAtual,
        int totalPaginas,
        long totalItens,
        int tamanhoPagina
) { }
