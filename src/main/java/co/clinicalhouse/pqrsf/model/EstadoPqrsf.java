package co.clinicalhouse.pqrsf.model;

public enum EstadoPqrsf {
    RADICADO("radicado"),
    REVISADO("revisado"),
    EN_PROCESO("en proceso"),
    SOLUCIONADO("solucionado");

    private final String valor;

    EstadoPqrsf(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static EstadoPqrsf fromString(String text) {
        if (text == null) return RADICADO;
        for (EstadoPqrsf b : EstadoPqrsf.values()) {
            if (b.valor.equalsIgnoreCase(text.trim()) || b.name().equalsIgnoreCase(text.trim())) {
                return b;
            }
        }
        return RADICADO;
    }
}
