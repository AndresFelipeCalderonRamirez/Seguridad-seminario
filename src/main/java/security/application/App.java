package security.application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.lang.Runtime;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Random;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Base64;

/**
 * Archivo con vulnerabilidades INTENCIONALES para demostrar el pipeline.
 *
 * Vulnerabilidades incluidas:
 *   1.  Credenciales hardcodeadas (DB, API Key, JWT Secret, AWS)
 *   2.  Inyección SQL
 *   3.  Inyección de comandos
 *   4.  Manejo inseguro de excepciones
 *   5.  Path Traversal
 *   6.  Deserialización insegura
 *   7.  Generador de números aleatorios débil (para tokens de seguridad)
 *   8.  Algoritmo de cifrado débil (DES)
 *   9.  Clave criptográfica hardcodeada
 *   10. SSRF (Server-Side Request Forgery)
 */
public class App {

    // ---------------------------------------------------------------
    // 1. CREDENCIALES HARDCODEADAS — detectado por Gitleaks + CodeQL
    // ---------------------------------------------------------------
    private static final String DB_URL      = "jdbc:mysql://prod-server:3306/appdb";
    private static final String DB_USER     = "admin";
    private static final String DB_PASS     = "superSecreta123!";

    // API Key de OpenAI-style — Gitleaks: generic-api-key
    private static final String API_KEY     = "sk-prod-aK93mX02bLpQ77zR";

    // JWT Secret hardcodeado — Gitleaks: jwt
    private static final String JWT_SECRET  = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.supersecret";

    // Credenciales AWS hardcodeadas — Gitleaks: aws-access-token
    private static final String AWS_KEY     = "AKIAIOSFODNN7EXAMPLE";
    private static final String AWS_SECRET  = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    // Clave de cifrado hardcodeada — CodeQL: hard-coded-key
    private static final byte[] CRYPTO_KEY  = "1234567890123456".getBytes();

    public static void main(String[] args) throws Exception {
        String usuarioInput = args.length > 0 ? args[0] : "test";

        buscarUsuario(usuarioInput);
        ejecutarComando(usuarioInput);
        leerArchivo(usuarioInput);
        generarToken();
        cifrarDato("informacion-sensible");
        hacerPeticion(usuarioInput);
    }

    // ---------------------------------------------------------------
    // 2. SQL INJECTION — detectado por CodeQL + FindSecBugs
    // ---------------------------------------------------------------
    public static void buscarUsuario(String nombre) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            Statement stmt  = conn.createStatement();

            // VULNERABLE: concatenación directa, permite ' OR '1'='1
            String query = "SELECT * FROM usuarios WHERE nombre = '" + nombre + "'";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                System.out.println("Usuario: " + rs.getString("nombre"));
            }
        } catch (Exception e) {
            // VULNERABLE: expone stack trace completo al exterior
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    // 3. COMMAND INJECTION — detectado por CodeQL + FindSecBugs
    // ---------------------------------------------------------------
    public static void ejecutarComando(String parametro) {
        try {
            Runtime rt = Runtime.getRuntime();
            // VULNERABLE: entrada del usuario sin sanitizar en comando del sistema
            rt.exec("ls -la " + parametro);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    // 4. PATH TRAVERSAL — detectado por CodeQL
    //    Un atacante puede pasar "../../etc/passwd" como input
    // ---------------------------------------------------------------
    public static void leerArchivo(String nombreArchivo) {
        try {
            // VULNERABLE: no valida ni normaliza la ruta
            File archivo = new File("/app/uploads/" + nombreArchivo);
            BufferedReader reader = new BufferedReader(new FileReader(archivo));
            String linea;
            while ((linea = reader.readLine()) != null) {
                System.out.println(linea);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    // 5. RANDOM DÉBIL PARA TOKENS — detectado por CodeQL + FindSecBugs
    //    java.util.Random NO es criptográficamente seguro
    // ---------------------------------------------------------------
    public static String generarToken() {
        // VULNERABLE: debe usarse SecureRandom para tokens de seguridad
        Random random = new Random();
        long token = random.nextLong();
        System.out.println("Token generado: " + token);
        return String.valueOf(token);
    }

    // ---------------------------------------------------------------
    // 6. CIFRADO DÉBIL (DES) + CLAVE HARDCODEADA — detectado por CodeQL
    //    DES usa claves de 56 bits, considerado roto desde 1999
    // ---------------------------------------------------------------
    public static String cifrarDato(String dato) throws Exception {
        // VULNERABLE: DES es débil, debe usarse AES-256
        SecretKeySpec keySpec = new SecretKeySpec(
                new byte[]{1,2,3,4,5,6,7,8}, "DES"
        );
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        // VULNERABLE: modo ECB no usa IV, patrones del plaintext son visibles
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(dato.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // ---------------------------------------------------------------
    // 7. SSRF (Server-Side Request Forgery) — detectado por CodeQL
    //    El servidor hace peticiones a URLs controladas por el atacante
    // ---------------------------------------------------------------
    public static void hacerPeticion(String urlUsuario) {
        try {
            // VULNERABLE: no valida la URL antes de hacer la petición
            URL url = new URL(urlUsuario);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            String linea;
            while ((linea = reader.readLine()) != null) {
                System.out.println(linea);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}