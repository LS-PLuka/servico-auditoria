package antifraud.servicoauditoria.service;

import antifraud.servicoauditoria.document.Auditoria;
import antifraud.servicoauditoria.dto.ResultadoAnaliseEventoDTO;
import antifraud.servicoauditoria.enums.NivelRisco;
import antifraud.servicoauditoria.repository.AuditoriaRepository;
import antifraud.servicoauditoria.util.AuditoriaMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private AuditoriaMapper auditoriaMapper;

    @InjectMocks
    private AuditoriaService auditoriaService;

    @Test
    @DisplayName("Deve mapear e salvar uma auditoria para evento novo")
    void registrar_eventoNovo_salvaAuditoriaUmaVez() {
        ResultadoAnaliseEventoDTO evento = criarEvento();
        Auditoria auditoria = criarAuditoria(evento);
        when(auditoriaRepository.existsByTransacaoId(evento.transacaoId())).thenReturn(false);
        when(auditoriaMapper.paraDocumento(evento)).thenReturn(auditoria);

        auditoriaService.registrar(evento);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaMapper).paraDocumento(evento);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(auditoria);
        assertThat(captor.getValue().getTransacaoId()).isEqualTo(evento.transacaoId());
        assertThat(captor.getValue().getPontuacao()).isEqualTo(evento.pontuacao());
        assertThat(captor.getValue().getNivel()).isEqualTo(evento.nivel());
    }

    @Test
    @DisplayName("Não deve mapear nem salvar uma auditoria para evento duplicado")
    void registrar_eventoDuplicado_naoSalvaAuditoria() {
        ResultadoAnaliseEventoDTO evento = criarEvento();
        when(auditoriaRepository.existsByTransacaoId(evento.transacaoId())).thenReturn(true);

        auditoriaService.registrar(evento);

        verify(auditoriaMapper, never()).paraDocumento(evento);
        verify(auditoriaRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private ResultadoAnaliseEventoDTO criarEvento() {
        return new ResultadoAnaliseEventoDTO(
                UUID.randomUUID(),
                155,
                NivelRisco.BLOQUEADA,
                List.of("VALOR_ALTO", "CONTA_NOVA"),
                LocalDateTime.of(2026, 9, 24, 10, 30)
        );
    }

    private Auditoria criarAuditoria(ResultadoAnaliseEventoDTO evento) {
        return new Auditoria(
                null,
                evento.transacaoId(),
                evento.pontuacao(),
                evento.nivel(),
                evento.regrasDisparadas(),
                evento.analisadoEm(),
                LocalDateTime.of(2026, 9, 24, 10, 31)
        );
    }
}
