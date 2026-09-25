package antifraud.servicoauditoria.document;

import antifraud.servicoauditoria.enums.NivelRisco;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Document(collection = "auditorias")
@Getter
@AllArgsConstructor
public class Auditoria {

    @Id
    private final String id;

    @Indexed(unique = true)
    private final UUID transacaoId;

    private final int pontuacao;
    private final NivelRisco nivel;
    private final List<String> regrasDisparadas;
    private final LocalDateTime analisadoEm;
    private final LocalDateTime registradoEm;
}
