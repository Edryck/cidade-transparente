package br.edu.utfpr.cidadetransparente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Sem o usuário em memória com senha gerada: a autenticação é só por JWT
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
// Expurgo diário do IP no registro de acesso (AuditoriaService)
@EnableScheduling
public class CidadetransparenteApplication {

    public static void main(String[] args) {
        SpringApplication.run(CidadetransparenteApplication.class, args);
    }

}