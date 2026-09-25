package antifraud.servicoauditoria.controller;

import antifraud.servicoauditoria.dto.AuditoriaResponseDTO;
import antifraud.servicoauditoria.dto.PaginaResponseDTO;
import antifraud.servicoauditoria.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auditorias")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auditorias", description = "Consulta do histórico de análises antifraude")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping("/transacao/{transacaoId}")
    @Operation(summary = "Buscar auditoria por transação", description = "Retorna a auditoria registrada para uma transação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auditoria encontrada"),
            @ApiResponse(responseCode = "404", description = "Auditoria não encontrada")
    })
    public ResponseEntity<AuditoriaResponseDTO> buscarPorTransacaoId(@PathVariable UUID transacaoId) {
        log.info("Consulta de auditoria para a transação {}", transacaoId);
        return ResponseEntity.ok(auditoriaService.buscarPorTransacaoId(transacaoId));
    }

    @GetMapping
    @Operation(summary = "Listar auditorias", description = "Lista auditorias da mais recente para a mais antiga, com 10 registros por página.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auditorias listadas"),
            @ApiResponse(responseCode = "400", description = "Número da página inválido")
    })
    public ResponseEntity<PaginaResponseDTO<AuditoriaResponseDTO>> listar(
            @Parameter(description = "Número da página, começando em zero", example = "0")
            @RequestParam(defaultValue = "0") int pagina) {
        log.info("Consulta da página {} do histórico de auditorias", pagina);
        return ResponseEntity.ok(auditoriaService.listar(pagina));
    }
}
