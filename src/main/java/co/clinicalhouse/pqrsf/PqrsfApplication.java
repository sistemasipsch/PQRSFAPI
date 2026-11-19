package co.clinicalhouse.pqrsf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PqrsfApplication {

    public static void main(String[] args) {
        SpringApplication.run(PqrsfApplication.class, args);
    }
}
