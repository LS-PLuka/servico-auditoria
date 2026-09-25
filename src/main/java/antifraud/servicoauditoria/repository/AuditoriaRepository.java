package antifraud.servicoauditoria.repository;

import antifraud.servicoauditoria.document.Auditoria;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditoriaRepository extends MongoRepository<Auditoria, String> {

    Optional<Auditoria> findByTransacaoId(UUID transacaoId);

    boolean existsByTransacaoId(UUID transacaoId);

    long countByTransacaoId(UUID transacaoId);
}
