package antifraud.servicoauditoria.exception;

import antifraud.servicoauditoria.dto.ErroResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuditoriaNaoEncontradaException.class)
    public ResponseEntity<ErroResponseDTO> handleAuditoriaNaoEncontrada(AuditoriaNaoEncontradaException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErroResponseDTO(404, "Não encontrado", exception.getMessage()));
    }

    @ExceptionHandler(PaginaInvalidaException.class)
    public ResponseEntity<ErroResponseDTO> handlePaginaInvalida(PaginaInvalidaException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponseDTO(400, "Parâmetro inválido", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponseDTO> handleParametroInvalido(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponseDTO(400, "Parâmetro inválido", "O parâmetro informado possui formato inválido"));
    }
}
