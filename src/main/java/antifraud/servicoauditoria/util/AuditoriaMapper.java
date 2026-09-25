package antifraud.servicoauditoria.util;

import antifraud.servicoauditoria.document.Auditoria;
import antifraud.servicoauditoria.dto.AuditoriaResponseDTO;
import antifraud.servicoauditoria.dto.ResultadoAnaliseEventoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuditoriaMapper {

    private final Clock relogio;

    public Auditoria paraDocumento(ResultadoAnaliseEventoDTO evento) {
        return new Auditoria(
                null,
                evento.transacaoId(),
                evento.pontuacao(),
                evento.nivel(),
                List.copyOf(evento.regrasDisparadas()),
                evento.analisadoEm(),
                LocalDateTime.now(relogio)
        );
    }

    public AuditoriaResponseDTO paraResponse(Auditoria auditoria) {
        return new AuditoriaResponseDTO(
                auditoria.getId(),
                auditoria.getTransacaoId(),
                auditoria.getPontuacao(),
                auditoria.getNivel(),
                List.copyOf(auditoria.getRegrasDisparadas()),
                auditoria.getAnalisadoEm(),
                auditoria.getRegistradoEm()
        );
    }
}
