package co.clinicalhouse.pqrsf.dto;

/** Datos de la nueva cuenta. La autorización viaja en el header X-Acceso-Usuarios. */
public class CrearUsuarioRequestDTO {

    private String nombre;
    private String correo;
    private String cargo;
    private String rol;
    private String password;
    private String area;

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }


    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

}
