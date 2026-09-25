package antifraud.servicoauditoria.consumer;

import antifraud.servicoauditoria.config.RabbitMQConfig;
import antifraud.servicoauditoria.dto.ResultadoAnaliseEventoDTO;
import antifraud.servicoauditoria.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ResultadoAnaliseConsumer {

    private final AuditoriaService auditoriaService;

    @RabbitListener(queues = RabbitMQConfig.FILA_RESULTADOS)
    public void consumir(ResultadoAnaliseEventoDTO evento) {
        log.info("Resultado de risco recebido para a transação {}", evento.transacaoId());
        auditoriaService.registrar(evento);
    }
}
