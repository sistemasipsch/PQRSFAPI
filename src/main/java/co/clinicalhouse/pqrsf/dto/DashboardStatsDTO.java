package co.clinicalhouse.pqrsf.dto;

public class DashboardStatsDTO {
    private long total;
    private long radicados;
    private long revisados;
    private long enProceso;
    private long solucionados;

    public DashboardStatsDTO() {}

    public DashboardStatsDTO(long total, long radicados, long revisados, long enProceso, long solucionados) {
        this.total = total;
        this.radicados = radicados;
        this.revisados = revisados;
        this.enProceso = enProceso;
        this.solucionados = solucionados;
    }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getRadicados() { return radicados; }
    public void setRadicados(long radicados) { this.radicados = radicados; }

    public long getRevisados() { return revisados; }
    public void setRevisados(long revisados) { this.revisados = revisados; }

    public long getEnProceso() { return enProceso; }
    public void setEnProceso(long enProceso) { this.enProceso = enProceso; }

    public long getSolucionados() { return solucionados; }
    public void setSolucionados(long solucionados) { this.solucionados = solucionados; }
}
