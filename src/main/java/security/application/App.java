package security.application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.lang.Runtime;

/**
 * Archivo con vulnerabilidades INTENCIONALES para demostrar el pipeline.
 *
 * Vulnerabilidades incluidas:
 *   1. Credenciales hardcodeadas (usuario, contraseña, URL de BD)
 *   2. Inyección SQL (concatenación directa de entrada del usuario)
 *   3. Ejecución de comandos del sistema sin validación
 *   4. Manejo inseguro de excepciones (expone stack trace al usuario)
 */
public class App {

    // Credenciales hardcodeadas directamente en el código
    private static final String DB_URL  = "jdbc:mysql://prod-server:3306/appdb";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "superSecreta123!";

    // Clave de API hardcodeada (Gitleaks la detectará)
    private static final String API_KEY = "sk-prod-aK93mX02bLpQ77zR";

    public static void main(String[] args) {
        String usuarioInput = args.length > 0 ? args[0] : "test";

        buscarUsuario(usuarioInput);
        ejecutarComando(usuarioInput);
    }

    /**
     * Consulta SQL construida con concatenación directa.
     * Un atacante puede pasar: ' OR '1'='1 para extraer toda la tabla.
     * FindSecBugs detecta esto como SQL_INJECTION.
     */
    public static void buscarUsuario(String nombre) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            Statement stmt = conn.createStatement();

            // Vulnerabilidad: SQL Injection
            String query = "SELECT * FROM usuarios WHERE nombre = '" + nombre + "'";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                System.out.println("Usuario: " + rs.getString("nombre"));
            }

        } catch (Exception e) {
            // Exponer el stack trace completo puede revelar
            // rutas internas, versiones de librerías y estructura de la BD
            e.printStackTrace();
        }
    }

    /**
     * Ejecutar comandos del sistema con entrada del usuario
     * sin ningún tipo de validación o sanitización.
     * FindSecBugs detecta esto como COMMAND_INJECTION.
     */
    public static void ejecutarComando(String parametro) {
        try {
            Runtime rt = Runtime.getRuntime();
            // Vulnerabilidad: Command Injection
            rt.exec("ls -la " + parametro);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}