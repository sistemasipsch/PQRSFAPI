package co.clinicalhouse.pqrsf.service;

import co.clinicalhouse.pqrsf.dto.ActualizarPerfilDTO;
import co.clinicalhouse.pqrsf.dto.CrearUsuarioRequestDTO;
import co.clinicalhouse.pqrsf.dto.LoginRequestDTO;
import co.clinicalhouse.pqrsf.dto.LoginResponseDTO;
import co.clinicalhouse.pqrsf.model.Usuario;

import java.util.List;

public interface AuthService {
    LoginResponseDTO autenticar(LoginRequestDTO request, String ipOrigen, String userAgent);
    void inicializarUsuarioJefeSiNoExiste();
    String solicitarRecuperacion(String correo, String ipOrigen, String userAgent);
    void confirmarRecuperacion(String correo, String codigo, String nuevoPassword, String ipOrigen, String userAgent);
    List<Usuario> listarUsuarios();

    /** Crea un usuario. `autorizadoPor` es el Super Admin que habilitó el módulo de Usuarios. */
    Usuario crearUsuario(CrearUsuarioRequestDTO request, String autorizadoPor, String usuarioSesion, String ipOrigen);

    Usuario cambiarEstadoUsuario(Long id, boolean activo, String adminUsuario, String ipOrigen);

    /** Elimina un usuario permanentemente. Reasigna cualquier PQRSF a su cargo a 'Sin asignar'. */
    void eliminarUsuario(Long id, String superAdmin, String usuarioSesion, String ipOrigen);

    /** Asigna una nueva contraseña a un usuario (desde el módulo de Usuarios). */
    Usuario cambiarPasswordUsuario(Long id, String nuevoPassword, String adminUsuario, String ipOrigen);

    /** Actualiza nombre, correo, cargo y/o contraseña del usuario autenticado. Devuelve nueva sesión. */
    LoginResponseDTO actualizarPerfil(String correoActual, ActualizarPerfilDTO dto, String ipOrigen, String userAgent);

    /** Datos y permisos vigentes de la sesión (sin token nuevo). */
    LoginResponseDTO sesionActual(String correo);

    /** Modifica los datos de un usuario (nombre, correo, cargo, área y/o rol) desde el módulo de Usuarios. */
    Usuario editarUsuario(Long id, String nombre, String correo, String cargo, String area, String rol, String adminUsuario, String ipOrigen);
}
