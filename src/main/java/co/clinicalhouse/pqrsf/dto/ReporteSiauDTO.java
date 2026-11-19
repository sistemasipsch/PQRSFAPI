package co.clinicalhouse.pqrsf.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReporteSiauDTO {

    private long totalRequerimientos;
    private long enTerminoVerde;
    private long alertaAmarillo;
    private long vencidasRojo;
    private long solucionados;
    private double cumplimientoPorcentaje;
    private double tiempoPromedioDias;

    private Map<String, Long> distribucionEstados = new HashMap<>();
    private Map<String, Long> distribucionTipos = new HashMap<>();
    private Map<String, Long> distribucionCanales = new HashMap<>();
    private List<ResponsableReporteItemDTO> reportePorResponsable = new ArrayList<>();

    public ReporteSiauDTO() {}

    public static class ResponsableReporteItemDTO {
        private String responsable;
        private long totalAsignadas;
        private long solucionadas;
        private long enProceso;
        private long pendientes;
        private long vencidas;
        private double efectividadPorcentaje;

        public ResponsableReporteItemDTO() {}

        public ResponsableReporteItemDTO(String responsable, long totalAsignadas, long solucionadas, 
                                        long enProceso, long pendientes, long vencidas, double efectividadPorcentaje) {
            this.responsable = responsable;
            this.totalAsignadas = totalAsignadas;
            this.solucionadas = solucionadas;
            this.enProceso = enProceso;
            this.pendientes = pendientes;
            this.vencidas = vencidas;
            this.efectividadPorcentaje = efectividadPorcentaje;
        }

        public String getResponsable() { return responsable; }
        public void setResponsable(String responsable) { this.responsable = responsable; }

        public long getTotalAsignadas() { return totalAsignadas; }
        public void setTotalAsignadas(long totalAsignadas) { this.totalAsignadas = totalAsignadas; }

        public long getSolucionadas() { return solucionadas; }
        public void setSolucionadas(long solucionadas) { this.solucionadas = solucionadas; }

        public long getEnProceso() { return enProceso; }
        public void setEnProceso(long enProceso) { this.enProceso = enProceso; }

        public long getPendientes() { return pendientes; }
        public void setPendientes(long pendientes) { this.pendientes = pendientes; }

        public long getVencidas() { return vencidas; }
        public void setVencidas(long vencidas) { this.vencidas = vencidas; }

        public double getEfectividadPorcentaje() { return efectividadPorcentaje; }
        public void setEfectividadPorcentaje(double efectividadPorcentaje) { this.efectividadPorcentaje = efectividadPorcentaje; }
    }

    public long getTotalRequerimientos() { return totalRequerimientos; }
    public void setTotalRequerimientos(long totalRequerimientos) { this.totalRequerimientos = totalRequerimientos; }

    public long getEnTerminoVerde() { return enTerminoVerde; }
    public void setEnTerminoVerde(long enTerminoVerde) { this.enTerminoVerde = enTerminoVerde; }

    public long getAlertaAmarillo() { return alertaAmarillo; }
    public void setAlertaAmarillo(long alertaAmarillo) { this.alertaAmarillo = alertaAmarillo; }

    public long getVencidasRojo() { return vencidasRojo; }
    public void setVencidasRojo(long vencidasRojo) { this.vencidasRojo = vencidasRojo; }

    public long getSolucionados() { return solucionados; }
    public void setSolucionados(long solucionados) { this.solucionados = solucionados; }

    public double getCumplimientoPorcentaje() { return cumplimientoPorcentaje; }
    public void setCumplimientoPorcentaje(double cumplimientoPorcentaje) { this.cumplimientoPorcentaje = cumplimientoPorcentaje; }

    public double getTiempoPromedioDias() { return tiempoPromedioDias; }
    public void setTiempoPromedioDias(double tiempoPromedioDias) { this.tiempoPromedioDias = tiempoPromedioDias; }

    public Map<String, Long> getDistribucionEstados() { return distribucionEstados; }
    public void setDistribucionEstados(Map<String, Long> distribucionEstados) { this.distribucionEstados = distribucionEstados; }

    public Map<String, Long> getDistribucionTipos() { return distribucionTipos; }
    public void setDistribucionTipos(Map<String, Long> distribucionTipos) { this.distribucionTipos = distribucionTipos; }

    public Map<String, Long> getDistribucionCanales() { return distribucionCanales; }
    public void setDistribucionCanales(Map<String, Long> distribucionCanales) { this.distribucionCanales = distribucionCanales; }

    public List<ResponsableReporteItemDTO> getReportePorResponsable() { return reportePorResponsable; }
    public void setReportePorResponsable(List<ResponsableReporteItemDTO> reportePorResponsable) { this.reportePorResponsable = reportePorResponsable; }

    // ==================== DISTRIBUCIÓN POR ÁREA ====================
    /**
     * Desglose de las solicitudes del periodo por área asignada (incluye las áreas activas del
     * catálogo aunque no tengan solicitudes, y "Sin área" para las que aún no tienen una).
     */
    public record AreaReporteItemDTO(
            String area,
            long total,
            long enTermino,
            long porVencer,
            long vencidas,
            long solucionadas,
            double cumplimientoPorcentaje,
            Map<String, Long> tipos,
            Map<String, Long> estados) {}

    private List<AreaReporteItemDTO> reportePorArea = new ArrayList<>();

    public List<AreaReporteItemDTO> getReportePorArea() { return reportePorArea; }
    public void setReportePorArea(List<AreaReporteItemDTO> reportePorArea) { this.reportePorArea = reportePorArea; }

    // ==================== DISTRIBUCIÓN POR RIESGO Y SERIES TEMPORALES ====================
    private Map<String, Long> distribucionRiesgo = new HashMap<>();
    private SerieTemporalDTO serieMensual = new SerieTemporalDTO();
    private SerieTemporalDTO serieDiaria = new SerieTemporalDTO();

    public Map<String, Long> getDistribucionRiesgo() { return distribucionRiesgo; }
    public void setDistribucionRiesgo(Map<String, Long> distribucionRiesgo) { this.distribucionRiesgo = distribucionRiesgo; }

    public SerieTemporalDTO getSerieMensual() { return serieMensual; }
    public void setSerieMensual(SerieTemporalDTO serieMensual) { this.serieMensual = serieMensual; }

    public SerieTemporalDTO getSerieDiaria() { return serieDiaria; }
    public void setSerieDiaria(SerieTemporalDTO serieDiaria) { this.serieDiaria = serieDiaria; }

    /** Serie para gráficos: etiquetas del eje X y conteos de radicadas / solucionadas. */
    public static class SerieTemporalDTO {
        private List<String> etiquetas = new ArrayList<>();
        private List<Long> radicadas = new ArrayList<>();
        private List<Long> solucionadas = new ArrayList<>();

        public List<String> getEtiquetas() { return etiquetas; }
        public void setEtiquetas(List<String> etiquetas) { this.etiquetas = etiquetas; }

        public List<Long> getRadicadas() { return radicadas; }
        public void setRadicadas(List<Long> radicadas) { this.radicadas = radicadas; }

        public List<Long> getSolucionadas() { return solucionadas; }
        public void setSolucionadas(List<Long> solucionadas) { this.solucionadas = solucionadas; }
    }
}
