package antifraud.servicoauditoria.consumer;

import antifraud.servicoauditoria.dto.ResultadoAnaliseEventoDTO;
import antifraud.servicoauditoria.enums.NivelRisco;
import antifraud.servicoauditoria.service.AuditoriaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResultadoAnaliseConsumerTest {

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ResultadoAnaliseConsumer consumer;

    @Test
    @DisplayName("Deve delegar o mesmo evento recebido ao serviço uma vez")
    void consumir_eventoRecebido_delegaAoServiceUmaVez() {
        ResultadoAnaliseEventoDTO evento = new ResultadoAnaliseEventoDTO(
                UUID.randomUUID(),
                155,
                NivelRisco.BLOQUEADA,
                List.of("VALOR_ALTO"),
                LocalDateTime.of(2026, 9, 24, 10, 30)
        );

        consumer.consumir(evento);

        verify(auditoriaService).registrar(evento);
    }
}
