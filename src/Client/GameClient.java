package Client;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class GameClient {
    private static String serverIP;
    private static int serverPort;
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;

    public static void main(String[] args) {
        try {
            // Pedir al usuario la IP y el puerto del servidor
            serverIP = JOptionPane.showInputDialog("Ingresa la IP del servidor:");
            String portInput = JOptionPane.showInputDialog("Ingresa el puerto del servidor:");
            serverPort = (portInput == null || portInput.isEmpty()) ? 12345 : Integer.parseInt(portInput);

            // Conectarse al servidor
            socket = new Socket(serverIP, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            JOptionPane.showMessageDialog(null, "Conectado al servidor de Piedra, Papel o Tijera.");

            // Ejecutar la lógica del juego
            runGame();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error al conectarse al servidor: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private static void runGame() {
        try {
            String serverMessage;

            while ((serverMessage = in.readLine()) != null) {
                if (serverMessage.equals("Juega")) {
                    // Mostrar opciones de Piedra, Papel o Tijera
                    String[] options = {"Piedra", "Papel", "Tijera"};
                    String choice = (String) JOptionPane.showInputDialog(
                            null,
                            "Elige tu opción:",
                            "Piedra, Papel o Tijera",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]
                    );

                    // Enviar la elección al servidor
                    if (choice != null) {
                        out.println(choice);
                    } else {
                        out.println("Salir");
                        break;
                    }
                } else if (serverMessage.contains("Resultado de la partida")) {
                    // Mostrar el resultado de la partida
                    JOptionPane.showMessageDialog(null, serverMessage);
                } else if (serverMessage.equals("¿Deseas continuar jugando? (sí/no)")) {
                    // Preguntar al usuario si desea continuar
                    int response = JOptionPane.showConfirmDialog(
                            null,
                            "¿Deseas continuar jugando?",
                            "Continuar jugando",
                            JOptionPane.YES_NO_OPTION
                    );

                    // Enviar la respuesta al servidor
                    if (response == JOptionPane.YES_OPTION) {
                        out.println("sí");
                    } else {
                        out.println("no");
                        break; // Salir del bucle si el jugador no desea continuar
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error en la comunicación con el servidor: " + e.getMessage());
        }
    }

    // Cerrar la conexión con el servidor
    private static void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error al cerrar la conexión: " + e.getMessage());
        }
    }
}
