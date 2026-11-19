package co.clinicalhouse.pqrsf.dto;

/**
 * Actualización del perfil del usuario autenticado.
 * Los campos nulos o vacíos no se modifican. Siempre se exige la contraseña actual.
 */
public class ActualizarPerfilDTO {

    private String nombre;
    private String correo;
    private String cargo;
    private String passwordActual;
    private String nuevoPassword;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getPasswordActual() { return passwordActual; }
    public void setPasswordActual(String passwordActual) { this.passwordActual = passwordActual; }

    public String getNuevoPassword() { return nuevoPassword; }
    public void setNuevoPassword(String nuevoPassword) { this.nuevoPassword = nuevoPassword; }
}
