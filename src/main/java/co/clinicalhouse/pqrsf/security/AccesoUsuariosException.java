package co.clinicalhouse.pqrsf.security;

/** Rechazo de acceso al módulo de Usuarios, con un motivo que el frontend interpreta. */
public class AccesoUsuariosException extends RuntimeException {

    public enum Motivo {
        /** Correo o contraseña incorrectos. */
        CREDENCIALES_INVALIDAS,
        /** Credenciales correctas pero la cuenta no es Super Administrador. */
        NO_SUPER_ADMIN,
        /** Demasiados intentos fallidos: bloqueo temporal. */
        BLOQUEADO,
        /** Falta la autorización del módulo o expiró. */
        ACCESO_REQUERIDO
    }

    private final Motivo motivo;

    public AccesoUsuariosException(Motivo motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public Motivo getMotivo() { return motivo; }
}
