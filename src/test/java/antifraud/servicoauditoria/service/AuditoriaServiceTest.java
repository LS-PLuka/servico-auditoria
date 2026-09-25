package antifraud.servicoauditoria.service;

import antifraud.servicoauditoria.document.Auditoria;
import antifraud.servicoauditoria.dto.AuditoriaResponseDTO;
import antifraud.servicoauditoria.dto.PaginaResponseDTO;
import antifraud.servicoauditoria.dto.ResultadoAnaliseEventoDTO;
import antifraud.servicoauditoria.enums.NivelRisco;
import antifraud.servicoauditoria.exception.AuditoriaNaoEncontradaException;
import antifraud.servicoauditoria.exception.PaginaInvalidaException;
import antifraud.servicoauditoria.repository.AuditoriaRepository;
import antifraud.servicoauditoria.util.AuditoriaMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    @DisplayName("Deve buscar a auditoria pela transação e retornar o DTO mapeado")
    void buscarPorTransacaoId_auditoriaExistente_retornaResponse() {
        UUID transacaoId = UUID.randomUUID();
        Auditoria auditoria = criarAuditoria(criarEvento(transacaoId));
        AuditoriaResponseDTO response = criarResponse(auditoria);
        when(auditoriaRepository.findByTransacaoId(transacaoId)).thenReturn(java.util.Optional.of(auditoria));
        when(auditoriaMapper.paraResponse(auditoria)).thenReturn(response);

        AuditoriaResponseDTO resultado = auditoriaService.buscarPorTransacaoId(transacaoId);

        assertThat(resultado).isSameAs(response);
        verify(auditoriaRepository).findByTransacaoId(transacaoId);
        verify(auditoriaMapper).paraResponse(auditoria);
    }

    @Test
    @DisplayName("Deve lançar exceção quando não existir auditoria para a transação")
    void buscarPorTransacaoId_auditoriaInexistente_lancaExcecao() {
        UUID transacaoId = UUID.randomUUID();
        when(auditoriaRepository.findByTransacaoId(transacaoId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> auditoriaService.buscarPorTransacaoId(transacaoId))
                .isInstanceOf(AuditoriaNaoEncontradaException.class)
                .hasMessageContaining(transacaoId.toString());
        verify(auditoriaRepository).findByTransacaoId(transacaoId);
        verify(auditoriaMapper, never()).paraResponse(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Deve listar auditorias com dez itens e as mais recentes primeiro")
    void listar_primeiraPagina_retornaConteudoEMetadados() {
        Auditoria primeira = criarAuditoria(criarEvento());
        Auditoria segunda = criarAuditoria(criarEvento());
        AuditoriaResponseDTO primeiraResponse = criarResponse(primeira);
        AuditoriaResponseDTO segundaResponse = criarResponse(segunda);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(auditoriaRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(primeira, segunda), invocation.getArgument(0), 22
                ));
        when(auditoriaMapper.paraResponse(primeira)).thenReturn(primeiraResponse);
        when(auditoriaMapper.paraResponse(segunda)).thenReturn(segundaResponse);

        PaginaResponseDTO<AuditoriaResponseDTO> resultado = auditoriaService.listar(0);

        verify(auditoriaRepository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("registradoEm").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(resultado.conteudo()).containsExactly(primeiraResponse, segundaResponse);
        assertThat(resultado.paginaAtual()).isZero();
        assertThat(resultado.totalPaginas()).isEqualTo(3);
        assertThat(resultado.totalItens()).isEqualTo(22);
        assertThat(resultado.tamanhoPagina()).isEqualTo(10);
        verify(auditoriaMapper).paraResponse(primeira);
        verify(auditoriaMapper).paraResponse(segunda);
    }

    @Test
    @DisplayName("Deve rejeitar número de página negativo sem consultar o repositório")
    void listar_paginaNegativa_lancaExcecao() {
        assertThatThrownBy(() -> auditoriaService.listar(-1))
                .isInstanceOf(PaginaInvalidaException.class)
                .hasMessage("O número da página deve ser maior ou igual a zero");
        verify(auditoriaRepository, never()).findAll(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    private ResultadoAnaliseEventoDTO criarEvento() {
        return criarEvento(UUID.randomUUID());
    }

    private ResultadoAnaliseEventoDTO criarEvento(UUID transacaoId) {
        return new ResultadoAnaliseEventoDTO(
                transacaoId,
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

    private AuditoriaResponseDTO criarResponse(Auditoria auditoria) {
        return new AuditoriaResponseDTO(
                auditoria.getId(),
                auditoria.getTransacaoId(),
                auditoria.getPontuacao(),
                auditoria.getNivel(),
                auditoria.getRegrasDisparadas(),
                auditoria.getAnalisadoEm(),
                auditoria.getRegistradoEm()
        );
    }
}
