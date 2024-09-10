package Client;
import javax.swing.*;
import java.io.*;
import java.net.*;

public class GameClient {
    private static String SERVER_IP;
    private static int PORT;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public static void main(String[] args) {
        new GameClient().start();
    }

    public void start() {
        try {
            // Solicitar la IP y el puerto del servidor mediante JOptionPane
            SERVER_IP = JOptionPane.showInputDialog("Ingresa la dirección IP del servidor:");
            String portInput = JOptionPane.showInputDialog("Ingresa el puerto del servidor:");
            PORT = (portInput == null || portInput.isEmpty()) ? 12345 : Integer.parseInt(portInput);

            // Conectar al servidor usando la IP y el puerto proporcionados
            socket = new Socket(SERVER_IP, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Conectado al servidor en " + SERVER_IP + ":" + PORT);

            // Ciclo principal para recibir instrucciones del servidor
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                System.out.println("Servidor: " + serverMessage);
                
                if (serverMessage.equals("Juega")) {
                    // Mostrar opciones de juego: Piedra, Papel, Tijera
                    String[] options = {"Piedra", "Papel", "Tijera"};
                    int choice = JOptionPane.showOptionDialog(
                            null,
                            "Selecciona tu jugada:",
                            "Juego Piedra, Papel o Tijera",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            options,
                            options[0]);

                    String selection = options[choice];
                    out.println(selection); // Enviar la selección al servidor
                } else if (serverMessage.equals("Salir")) {
                    JOptionPane.showMessageDialog(null, "El juego ha terminado.");
                    break; // Termina el juego
                }
            }
        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }
}
