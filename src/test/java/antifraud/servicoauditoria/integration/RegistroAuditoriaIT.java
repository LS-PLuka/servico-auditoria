package antifraud.servicoauditoria.integration;

import antifraud.servicoauditoria.config.RabbitMQConfig;
import antifraud.servicoauditoria.document.Auditoria;
import antifraud.servicoauditoria.dto.ResultadoAnaliseEventoDTO;
import antifraud.servicoauditoria.enums.NivelRisco;
import antifraud.servicoauditoria.repository.AuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RegistroAuditoriaIT {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGODB = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.13-management-alpine")
    );

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @BeforeEach
    void limparColecao() {
        auditoriaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve consumir, persistir e ignorar a repetição do mesmo resultado")
    void publicar_resultadoRepetido_persisteUmaAuditoria() {
        UUID transacaoId = UUID.randomUUID();
        LocalDateTime analisadoEm = LocalDateTime.of(2026, 9, 24, 10, 30);
        List<String> regras = List.of(
                "VALOR_ALTO",
                "CONTA_NOVA",
                "HORARIO_SUSPEITO",
                "VALOR_MUITO_ALTO_CONTA_NOVA",
                "PAIS_ESTRANGEIRO"
        );
        ResultadoAnaliseEventoDTO evento = new ResultadoAnaliseEventoDTO(
                transacaoId, 155, NivelRisco.BLOQUEADA, regras, analisadoEm
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_RISCO,
                RabbitMQConfig.ROUTING_KEY_RESULTADO,
                evento
        );

        await().untilAsserted(() -> {
            Auditoria auditoria = auditoriaRepository.findByTransacaoId(transacaoId).orElse(null);
            assertThat(auditoria).isNotNull();
            assertThat(auditoria.getTransacaoId()).isEqualTo(transacaoId);
            assertThat(auditoria.getPontuacao()).isEqualTo(155);
            assertThat(auditoria.getNivel()).isEqualTo(NivelRisco.BLOQUEADA);
            assertThat(auditoria.getRegrasDisparadas()).containsExactlyElementsOf(regras);
            assertThat(auditoria.getAnalisadoEm()).isEqualTo(analisadoEm);
            assertThat(auditoria.getRegistradoEm()).isNotNull();
            assertThat(auditoria.getRegistradoEm()).isCloseTo(
                    LocalDateTime.now(),
                    org.assertj.core.api.Assertions.within(30, ChronoUnit.SECONDS)
            );
        });

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_RISCO,
                RabbitMQConfig.ROUTING_KEY_RESULTADO,
                evento
        );

        await().during(java.time.Duration.ofSeconds(1)).untilAsserted(() ->
                assertThat(auditoriaRepository.countByTransacaoId(transacaoId)).isEqualTo(1)
        );
    }
}
