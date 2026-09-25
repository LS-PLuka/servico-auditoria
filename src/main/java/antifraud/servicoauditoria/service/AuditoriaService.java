package antifraud.servicoauditoria.service;

import antifraud.servicoauditoria.document.Auditoria;
import antifraud.servicoauditoria.dto.AuditoriaResponseDTO;
import antifraud.servicoauditoria.dto.PaginaResponseDTO;
import antifraud.servicoauditoria.dto.ResultadoAnaliseEventoDTO;
import antifraud.servicoauditoria.exception.AuditoriaNaoEncontradaException;
import antifraud.servicoauditoria.exception.PaginaInvalidaException;
import antifraud.servicoauditoria.repository.AuditoriaRepository;
import antifraud.servicoauditoria.util.AuditoriaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditoriaService {

    private static final int TAMANHO_PAGINA = 10;

    private final AuditoriaRepository auditoriaRepository;
    private final AuditoriaMapper auditoriaMapper;

    public void registrar(ResultadoAnaliseEventoDTO evento) {
        if (auditoriaRepository.existsByTransacaoId(evento.transacaoId())) {
            log.info("Resultado duplicado ignorado para a transação {}", evento.transacaoId());
            return;
        }

        Auditoria auditoria = auditoriaMapper.paraDocumento(evento);

        try {
            auditoriaRepository.save(auditoria);
            log.info("Auditoria registrada para a transação {}", evento.transacaoId());
        } catch (DuplicateKeyException exception) {
            log.info("Resultado duplicado ignorado para a transação {}", evento.transacaoId());
        }
    }

    public AuditoriaResponseDTO buscarPorTransacaoId(UUID transacaoId) {
        Auditoria auditoria = auditoriaRepository.findByTransacaoId(transacaoId)
                .orElseThrow(() -> new AuditoriaNaoEncontradaException(transacaoId));

        return auditoriaMapper.paraResponse(auditoria);
    }

    public PaginaResponseDTO<AuditoriaResponseDTO> listar(int pagina) {
        if (pagina < 0) {
            throw new PaginaInvalidaException();
        }

        Pageable pageable = PageRequest.of(
                pagina,
                TAMANHO_PAGINA,
                Sort.by(Sort.Direction.DESC, "registradoEm")
        );
        Page<AuditoriaResponseDTO> auditorias = auditoriaRepository.findAll(pageable)
                .map(auditoriaMapper::paraResponse);

        return new PaginaResponseDTO<>(
                auditorias.getContent(),
                auditorias.getNumber(),
                auditorias.getTotalPages(),
                auditorias.getTotalElements(),
                auditorias.getSize()
        );
    }
}
