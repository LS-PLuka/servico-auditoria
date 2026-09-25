package antifraud.servicoauditoria;

import org.springframework.boot.SpringApplication;

public class TestServicoAuditoriaApplication {

    public static void main(String[] args) {
        SpringApplication.from(ServicoAuditoriaApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
