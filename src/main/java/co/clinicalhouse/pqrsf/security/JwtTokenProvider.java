package co.clinicalhouse.pqrsf.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;

/**
 * Emite y valida los JWT del sistema. Hay dos tipos de token y no son intercambiables:
 *  - SESION: sesión normal del funcionario (Authorization: Bearer ...).
 *  - ACCESO_USUARIOS: autorización temporal del módulo de Usuarios, emitida tras validar
 *    credenciales de un Super Administrador y ligada a la sesión que la solicitó.
 */
@Component
public class JwtTokenProvider {

    public static final String TIPO_SESION = "SESION";
    public static final String TIPO_ACCESO_USUARIOS = "ACCESO_USUARIOS";

    private final Key key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generarToken(String correo, String rol, Long usuarioId) {
        Date ahora = new Date();
        return Jwts.builder()
                .setSubject(correo)
                .claim("tipo", TIPO_SESION)
                .claim("rol", rol)
                .claim("usuarioId", usuarioId)
                .setIssuedAt(ahora)
                .setExpiration(new Date(ahora.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Autorización temporal del módulo de Usuarios. */
    public String generarTokenAccesoUsuarios(String superAdminCorreo, String sesionCorreo, long minutos) {
        Date ahora = new Date();
        return Jwts.builder()
                .setSubject(superAdminCorreo)
                .claim("tipo", TIPO_ACCESO_USUARIOS)
                .claim("sesion", sesionCorreo)
                .setIssuedAt(ahora)
                .setExpiration(new Date(ahora.getTime() + minutos * 60_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Optional<Claims> leerClaims(String token) {
        try {
            return Optional.of(Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Token de sesión válido (los emitidos antes de existir el claim "tipo" también se aceptan). */
    public boolean esTokenDeSesion(String token) {
        return leerClaims(token)
                .map(c -> c.get("tipo", String.class))
                .map(tipo -> TIPO_SESION.equals(tipo))
                .orElseGet(() -> leerClaims(token).isPresent()); // sin claim "tipo" → token de sesión antiguo
    }

    public String obtenerCorreoDelToken(String token) {
        return leerClaims(token).map(Claims::getSubject).orElse(null);
    }

    public String obtenerRolDelToken(String token) {
        return leerClaims(token).map(c -> c.get("rol", String.class)).orElse(null);
    }

    public boolean validarToken(String token) {
        return leerClaims(token).isPresent();
    }
}
