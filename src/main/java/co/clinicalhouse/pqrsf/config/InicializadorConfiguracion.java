package co.clinicalhouse.pqrsf.config;

import co.clinicalhouse.pqrsf.security.PermisoService;
import co.clinicalhouse.pqrsf.service.ConfiguracionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Al arrancar: siembra permisos, áreas y cargos (si están vacíos) y aplica los tiempos de respuesta guardados. */
@Component
public class InicializadorConfiguracion implements ApplicationRunner {

    private final PermisoService permisoService;
    private final ConfiguracionService configuracionService;

    public InicializadorConfiguracion(PermisoService permisoService, ConfiguracionService configuracionService) {
        this.permisoService = permisoService;
        this.configuracionService = configuracionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        configuracionService.inicializar(); // primero el catálogo de cargos…
        permisoService.inicializar();       // …luego los permisos de cada cargo
    }
}
