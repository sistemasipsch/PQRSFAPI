package co.clinicalhouse.pqrsf.sla;

import co.clinicalhouse.pqrsf.sla.SlaPolicy.NivelRiesgo;
import co.clinicalhouse.pqrsf.sla.SlaPolicy.Semaforo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlaPolicyTest {

    private static final LocalDateTime RADICACION = LocalDateTime.of(2026, 11, 18, 8, 0);

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "'(P) Petición', SIMPLE",
            "'(S) Sugerencia', SIMPLE",
            "'(F) Felicitación', SIMPLE",
            "'(Q) Queja', PRIORIZADO",
            "'(R) Reclamo', PRIORIZADO",
            "'(T) Tutela', VITAL",
            "'Acción de tutela', VITAL",
            "'', SIMPLE"
    })
    @DisplayName("El nivel de riesgo se deriva del tipo de solicitud")
    void riesgoPorTipo(String tipo, NivelRiesgo esperado) {
        assertEquals(esperado, SlaPolicy.riesgoPara(tipo));
    }

    @Test
    @DisplayName("Plazos: simple 48h, priorizado 24h, vital 24h")
    void plazos() {
        assertEquals(48, NivelRiesgo.SIMPLE.getPlazoHoras());
        assertEquals(24, NivelRiesgo.PRIORIZADO.getPlazoHoras());
        assertEquals(24, NivelRiesgo.VITAL.getPlazoHoras());
    }

    @ParameterizedTest(name = "{0} tras {1}h → {2}")
    @CsvSource({
            // Petición: 48h → amarillo desde 24h, rojo desde 48h
            "'(P) Petición', 1,  VERDE",
            "'(P) Petición', 23, VERDE",
            "'(P) Petición', 24, AMARILLO",
            "'(P) Petición', 47, AMARILLO",
            "'(P) Petición', 48, ROJO",
            // Queja: 24h → amarillo desde 12h, rojo desde 24h
            "'(Q) Queja',    11, VERDE",
            "'(Q) Queja',    12, AMARILLO",
            "'(Q) Queja',    24, ROJO",
            // Tutela: 24h
            "'(T) Tutela',   6,  VERDE",
            "'(T) Tutela',   20, AMARILLO",
            "'(T) Tutela',   30, ROJO"
    })
    @DisplayName("Semáforo según tiempo transcurrido y nivel de riesgo")
    void semaforo(String tipo, long horas, Semaforo esperado) {
        var ev = SlaPolicy.evaluar(tipo, "radicado", RADICACION, RADICACION.plusHours(horas));
        assertEquals(esperado, ev.semaforo());
    }

    @Test
    @DisplayName("Una solicitud solucionada queda FINALIZADA aunque esté vencida")
    void solucionadaEsFinalizada() {
        var ev = SlaPolicy.evaluar("(T) Tutela", "solucionado", RADICACION, RADICACION.plusDays(10));
        assertEquals(Semaforo.FINALIZADO, ev.semaforo());
    }

    @Test
    @DisplayName("Fecha límite y texto de vencimiento")
    void fechaLimiteYTexto() {
        var ev = SlaPolicy.evaluar("(Q) Queja", "en proceso", RADICACION, RADICACION.plusHours(30));
        assertEquals(RADICACION.plusHours(24), ev.fechaLimite());
        assertEquals(-360, ev.minutosRestantes());
        assertTrue(ev.texto().startsWith("Vencida hace 6h"), ev.texto());

        var ok = SlaPolicy.evaluar("(P) Petición", "radicado", RADICACION, RADICACION.plusHours(2));
        assertTrue(ok.texto().startsWith("Vence en 1d 22h"), ok.texto());
    }

    @Test
    @DisplayName("Los plazos y el umbral configurados desde el panel reemplazan los predeterminados")
    void plazosConfigurables() {
        try {
            SlaPolicy.configurar(java.util.Map.of(NivelRiesgo.SIMPLE, 72L), 0.8);
            assertEquals(72, NivelRiesgo.SIMPLE.getPlazoHoras());
            assertEquals(24, NivelRiesgo.PRIORIZADO.getPlazoHoras()); // no configurado → predeterminado
            // 72h con umbral 80%: a las 50h sigue en verde, a las 58h pasa a amarillo
            assertEquals(Semaforo.VERDE, SlaPolicy.evaluar("(P) Petición", "radicado", RADICACION, RADICACION.plusHours(50)).semaforo());
            assertEquals(Semaforo.AMARILLO, SlaPolicy.evaluar("(P) Petición", "radicado", RADICACION, RADICACION.plusHours(58)).semaforo());
            assertEquals(RADICACION.plusHours(72), SlaPolicy.evaluar("(P) Petición", "radicado", RADICACION, RADICACION).fechaLimite());
        } finally {
            SlaPolicy.configurar(java.util.Map.of(), null);
        }
        assertEquals(48, NivelRiesgo.SIMPLE.getPlazoHoras());
    }

    @Test
    @DisplayName("Estado con guion bajo o mayúsculas se reconoce como solucionado")
    void estadoNormalizado() {
        assertEquals(Semaforo.FINALIZADO,
                SlaPolicy.evaluar("(P) Petición", " Solucionado ", RADICACION, RADICACION.plusHours(1)).semaforo());
    }
}
