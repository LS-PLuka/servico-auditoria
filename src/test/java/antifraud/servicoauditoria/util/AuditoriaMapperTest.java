package antifraud.servicoauditoria.util;

import antifraud.servicoauditoria.document.Auditoria;
import antifraud.servicoauditoria.dto.AuditoriaResponseDTO;
import antifraud.servicoauditoria.dto.ResultadoAnaliseEventoDTO;
import antifraud.servicoauditoria.enums.NivelRisco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditoriaMapperTest {

    @Test
    @DisplayName("Deve copiar o resultado da análise e registrar o horário atual")
    void paraDocumento_eventoValido_copiaCamposEGeraRegistro() {
        Instant instanteRegistro = Instant.parse("2026-09-24T14:00:00Z");
        AuditoriaMapper mapper = new AuditoriaMapper(Clock.fixed(instanteRegistro, ZoneOffset.UTC));
        UUID transacaoId = UUID.randomUUID();
        LocalDateTime analisadoEm = LocalDateTime.of(2026, 9, 24, 10, 30);
        List<String> regras = List.of("VALOR_ALTO", "CONTA_NOVA");
        ResultadoAnaliseEventoDTO evento = new ResultadoAnaliseEventoDTO(
                transacaoId, 155, NivelRisco.BLOQUEADA, regras, analisadoEm
        );

        Auditoria auditoria = mapper.paraDocumento(evento);

        assertThat(auditoria.getId()).isNull();
        assertThat(auditoria.getTransacaoId()).isEqualTo(transacaoId);
        assertThat(auditoria.getPontuacao()).isEqualTo(155);
        assertThat(auditoria.getNivel()).isEqualTo(NivelRisco.BLOQUEADA);
        assertThat(auditoria.getRegrasDisparadas()).containsExactlyElementsOf(regras);
        assertThat(auditoria.getAnalisadoEm()).isEqualTo(analisadoEm);
        assertThat(auditoria.getRegistradoEm()).isEqualTo(LocalDateTime.of(2026, 9, 24, 14, 0));
    }

    @Test
    @DisplayName("Deve copiar todos os campos persistidos para o DTO de resposta")
    void paraResponse_auditoriaValida_copiaTodosOsCampos() {
        AuditoriaMapper mapper = new AuditoriaMapper(Clock.systemUTC());
        UUID transacaoId = UUID.randomUUID();
        Auditoria auditoria = new Auditoria(
                "auditoria-1",
                transacaoId,
                155,
                NivelRisco.BLOQUEADA,
                List.of("VALOR_ALTO"),
                LocalDateTime.of(2026, 9, 24, 10, 30),
                LocalDateTime.of(2026, 9, 24, 10, 31)
        );

        AuditoriaResponseDTO response = mapper.paraResponse(auditoria);

        assertThat(response.id()).isEqualTo("auditoria-1");
        assertThat(response.transacaoId()).isEqualTo(transacaoId);
        assertThat(response.pontuacao()).isEqualTo(155);
        assertThat(response.nivel()).isEqualTo(NivelRisco.BLOQUEADA);
        assertThat(response.regrasDisparadas()).containsExactly("VALOR_ALTO");
        assertThat(response.analisadoEm()).isEqualTo(LocalDateTime.of(2026, 9, 24, 10, 30));
        assertThat(response.registradoEm()).isEqualTo(LocalDateTime.of(2026, 9, 24, 10, 31));
    }
}
