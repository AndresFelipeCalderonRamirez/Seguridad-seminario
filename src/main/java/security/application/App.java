package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Versión SEGURA — sin vulnerabilidades.
 * Rama: secure
 *
 * Correcciones aplicadas:
 *   1. Credenciales leídas desde variables de entorno (no hardcodeadas)
 *   2. PreparedStatement evita SQL Injection
 *   3. Sin ejecución de comandos del sistema
 *   4. Manejo de excepciones sin exponer stack trace
 */
public class App {

    // Credenciales desde variables de entorno
    private static final String DB_URL  = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASSWORD");

    public static void main(String[] args) {
        String usuarioInput = args.length > 0 ? args[0] : "test";
        buscarUsuario(usuarioInput);
    }

    /**
     * reparedStatement parametrizado — inmune a SQL Injection.
     * El input del usuario nunca se concatena directamente al query.
     */
    public static void buscarUsuario(String nombre) {
        if (DB_URL == null || DB_USER == null || DB_PASS == null) {
            System.err.println("Error: variables de entorno de BD no configuradas.");
            return;
        }

        try (
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT * FROM usuarios WHERE nombre = ?"
                )
        ) {
            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                System.out.println("Usuario: " + rs.getString("nombre"));
            }

        } catch (Exception e) {
            System.err.println("Error al consultar la base de datos.");
        }
    }
}