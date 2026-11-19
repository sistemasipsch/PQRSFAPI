package co.clinicalhouse.pqrsf.dto;

/**
 * Datos para la edición de una cuenta de usuario existente.
 * La autorización de Super Administrador viaja en el header X-Acceso-Usuarios.
 */
public class EditarUsuarioRequestDTO {

    private String nombre;
    private String correo;
    private String cargo;
    private String area;
    private String rol;

    public EditarUsuarioRequestDTO() {}

    public EditarUsuarioRequestDTO(String nombre, String correo, String cargo, String area, String rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.cargo = cargo;
        this.area = area;
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
