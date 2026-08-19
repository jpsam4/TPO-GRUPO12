package uade.prog3.tpo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uade.prog3.tpo.config.TransporteNeo4j;

@SpringBootApplication
public class TpoApplication {
    public static void main(String[] args) {
        // Antes de levantar Spring hay que saber si Bolt (7687) llega o si hay
        // que ir por la Query API (HTTPS 443). De eso depende que beans se
        // crean, asi que no se puede decidir mas tarde.
        TransporteNeo4j.elegirYConfigurar();
        SpringApplication.run(TpoApplication.class, args);
    }
}
