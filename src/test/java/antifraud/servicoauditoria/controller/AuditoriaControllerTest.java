package antifraud.servicoauditoria.controller;

import antifraud.servicoauditoria.dto.AuditoriaResponseDTO;
import antifraud.servicoauditoria.dto.PaginaResponseDTO;
import antifraud.servicoauditoria.enums.NivelRisco;
import antifraud.servicoauditoria.exception.AuditoriaNaoEncontradaException;
import antifraud.servicoauditoria.exception.GlobalExceptionHandler;
import antifraud.servicoauditoria.exception.PaginaInvalidaException;
import antifraud.servicoauditoria.service.AuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuditoriaControllerTest {

    @Mock
    private AuditoriaService auditoriaService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuditoriaController(auditoriaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Deve retornar 200 e a auditoria quando a transação existir")
    void buscarPorTransacaoId_auditoriaExistente_retornaOk() throws Exception {
        UUID transacaoId = UUID.randomUUID();
        AuditoriaResponseDTO response = criarResponse(transacaoId);
        when(auditoriaService.buscarPorTransacaoId(transacaoId)).thenReturn(response);

        mockMvc.perform(get("/auditorias/transacao/{transacaoId}", transacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("auditoria-1"))
                .andExpect(jsonPath("$.transacaoId").value(transacaoId.toString()))
                .andExpect(jsonPath("$.pontuacao").value(155))
                .andExpect(jsonPath("$.nivel").value("BLOQUEADA"));

        verify(auditoriaService).buscarPorTransacaoId(transacaoId);
    }

    @Test
    @DisplayName("Deve retornar 404 quando não existir auditoria para a transação")
    void buscarPorTransacaoId_auditoriaInexistente_retornaNotFound() throws Exception {
        UUID transacaoId = UUID.randomUUID();
        when(auditoriaService.buscarPorTransacaoId(transacaoId))
                .thenThrow(new AuditoriaNaoEncontradaException(transacaoId));

        mockMvc.perform(get("/auditorias/transacao/{transacaoId}", transacaoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Não encontrado"))
                .andExpect(jsonPath("$.mensagem").value("Auditoria não encontrada para a transação " + transacaoId));
    }

    @Test
    @DisplayName("Deve retornar 200 com a estrutura paginada")
    void listar_paginaValida_retornaOkComPaginacao() throws Exception {
        UUID transacaoId = UUID.randomUUID();
        PaginaResponseDTO<AuditoriaResponseDTO> response = new PaginaResponseDTO<>(
                List.of(criarResponse(transacaoId)), 0, 3, 27, 10
        );
        when(auditoriaService.listar(0)).thenReturn(response);

        mockMvc.perform(get("/auditorias").param("pagina", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].transacaoId").value(transacaoId.toString()))
                .andExpect(jsonPath("$.paginaAtual").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.totalItens").value(27))
                .andExpect(jsonPath("$.tamanhoPagina").value(10));

        verify(auditoriaService).listar(0);
    }

    @Test
    @DisplayName("Deve usar a página zero quando o parâmetro for omitido")
    void listar_paginaOmitida_usaPaginaZero() throws Exception {
        when(auditoriaService.listar(0)).thenReturn(new PaginaResponseDTO<>(List.of(), 0, 0, 0, 10));

        mockMvc.perform(get("/auditorias"))
                .andExpect(status().isOk());

        verify(auditoriaService).listar(0);
    }

    @Test
    @DisplayName("Deve retornar 400 para página negativa")
    void listar_paginaNegativa_retornaBadRequest() throws Exception {
        when(auditoriaService.listar(-1)).thenThrow(new PaginaInvalidaException());

        mockMvc.perform(get("/auditorias").param("pagina", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.mensagem").value("O número da página deve ser maior ou igual a zero"));
    }

    private AuditoriaResponseDTO criarResponse(UUID transacaoId) {
        return new AuditoriaResponseDTO(
                "auditoria-1",
                transacaoId,
                155,
                NivelRisco.BLOQUEADA,
                List.of("VALOR_ALTO"),
                LocalDateTime.of(2026, 9, 24, 10, 30),
                LocalDateTime.of(2026, 9, 24, 10, 31)
        );
    }
}
