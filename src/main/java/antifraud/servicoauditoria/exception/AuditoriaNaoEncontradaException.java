package antifraud.servicoauditoria.exception;

import java.util.UUID;

public class AuditoriaNaoEncontradaException extends RuntimeException {

    public AuditoriaNaoEncontradaException(UUID transacaoId) {
        super("Auditoria não encontrada para a transação " + transacaoId);
    }
}
