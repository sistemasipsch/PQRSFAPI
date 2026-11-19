package co.clinicalhouse.pqrsf.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final co.clinicalhouse.pqrsf.repository.UsuarioRepository usuarioRepository;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   co.clinicalhouse.pqrsf.repository.UsuarioRepository usuarioRepository) {
        this.tokenProvider = tokenProvider;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = obtenerTokenDeSolicitud(request);

        // Solo los tokens de SESIÓN autentican; el token de acceso a Usuarios no sirve como sesión
        if (StringUtils.hasText(token) && tokenProvider.esTokenDeSesion(token)) {
            String correo = tokenProvider.obtenerCorreoDelToken(token);
            // La cuenta debe seguir existiendo y activa: un usuario desactivado o eliminado pierde el acceso de inmediato,
            // y el rol se toma de la base de datos (si se le cambió, aplica sin esperar a que venza el token)
            var usuario = correo == null ? null : usuarioRepository.findByCorreo(correo).orElse(null);
            if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
                filterChain.doFilter(request, response);
                return;
            }
            String rol = usuario.getRol();
            String authority = "ROLE_" + (rol != null ? rol : "JEFE_SIAU");

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    correo,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(authority))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String obtenerTokenDeSolicitud(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
