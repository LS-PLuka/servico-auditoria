package antifraud.servicoauditoria.exception;

public class PaginaInvalidaException extends RuntimeException {

    public PaginaInvalidaException() {
        super("O número da página deve ser maior ou igual a zero");
    }
}
