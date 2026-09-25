package antifraud.servicoauditoria.service;

import antifraud.servicoauditoria.document.Auditoria;
import antifraud.servicoauditoria.dto.ResultadoAnaliseEventoDTO;
import antifraud.servicoauditoria.repository.AuditoriaRepository;
import antifraud.servicoauditoria.util.AuditoriaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditoriaService {

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
}
