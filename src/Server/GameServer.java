package Server;import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class GameServer {
    private static int PORT;
    private static List<PlayerHandler> playersQueue = new ArrayList<>();
    private static boolean serverRunning = true;
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        try {
            // Solicitar el puerto para el servidor mediante JOptionPane
            String portInput = JOptionPane.showInputDialog("Ingresa el puerto del servidor:");
            PORT = (portInput == null || portInput.isEmpty()) ? 12345 : Integer.parseInt(portInput);

            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor de Piedra, Papel o Tijera iniciado en el puerto " + PORT);

            // Mostrar ventana de confirmación para cerrar el servidor
            Thread serverControlThread = new Thread(GameServer::showServerControlWindow);
            serverControlThread.start();

            // Aceptar conexiones de jugadores mientras el servidor está corriendo
            while (serverRunning) {
                try {
                    Socket playerSocket = serverSocket.accept();
                    String clientIp = playerSocket.getInetAddress().getHostAddress();
                    System.out.println("Nuevo jugador conectado desde: " + clientIp);

                    PlayerHandler playerHandler = new PlayerHandler(playerSocket);
                    playersQueue.add(playerHandler);

                    // Si hay al menos 3 jugadores, inicia una partida
                    if (playersQueue.size() >= 3) {
                        List<PlayerHandler> gamePlayers = new ArrayList<>(playersQueue.subList(0, 3));
                        playersQueue.removeAll(gamePlayers);
                        new Thread(new GameSession(gamePlayers)).start();
                    }
                } catch (IOException e) {
                    if (serverRunning) {
                        System.err.println("Error al aceptar un jugador: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
        } finally {
            closeServer();
        }
    }

    // Mostrar una ventana para controlar el servidor (iniciar y detener)
    private static void showServerControlWindow() {
        int option = JOptionPane.showConfirmDialog(
                null,
                "El servidor está activo en el puerto " + PORT + ".\n¿Deseas detener el servidor?",
                "Servidor de Piedra, Papel o Tijera",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        if (option == JOptionPane.YES_OPTION) {
            stopServer();
        }
    }

    // Detener el servidor y cerrar todas las conexiones
    public static void stopServer() {
        serverRunning = false;
        closeServer();
        System.out.println("Servidor detenido.");
    }

    // Cerrar el servidor y todos los jugadores conectados
    private static void closeServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (PlayerHandler player : playersQueue) {
                player.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar el servidor: " + e.getMessage());
        }
    }

    // Clase para manejar cada jugador conectado al servidor
    static class PlayerHandler {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private boolean wantsToContinue = true;

        public PlayerHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(this::handlePlayer).start();
        }

        // Manejar la comunicación con el jugador
        private void handlePlayer() {
            try {
                out.println("Bienvenido al juego de Piedra, Papel o Tijera. Esperando más jugadores...");
                while (serverRunning && !socket.isClosed()) {
                    // Mantener al jugador esperando hasta que comience una partida
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                System.err.println("Error con el jugador: " + e.getMessage());
            } finally {
                close();
            }
        }

        // Enviar mensaje al jugador
        public void sendMessage(String message) {
            out.println(message);
        }

        // Recibir respuesta del jugador
        public String receiveMessage() throws IOException {
            return in.readLine();
        }

        // Determinar si el jugador quiere continuar jugando
        public boolean wantsToContinue() {
            return wantsToContinue;
        }

        // Actualizar el estado de continuar jugando
        public void setWantsToContinue(boolean value) {
            this.wantsToContinue = value;
        }

        // Cerrar la conexión con el jugador
        public void close() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error al cerrar la conexión con el jugador: " + e.getMessage());
            }
        }
    }

    // Clase para manejar una sesión de juego entre 3 jugadores
    static class GameSession implements Runnable {
        private List<PlayerHandler> players;

        public GameSession(List<PlayerHandler> players) {
            this.players = players;
        }

        // Lógica principal del juego
        @Override
        public void run() {
            boolean continuePlaying = true;

            while (continuePlaying) {
                try {
                    // Solicitar a los jugadores que jueguen
                    for (PlayerHandler player : players) {
                        player.sendMessage("Juega"); // Enviar instrucción para jugar
                    }

                    // Recibir las elecciones de los jugadores
                    List<String> choices = new ArrayList<>();
                    for (PlayerHandler player : players) {
                        String choice = player.receiveMessage();
                        choices.add(choice);
                        System.out.println("Jugador eligió: " + choice);
                    }

                    // Lógica para determinar el ganador (simulada)
                    String resultado = "Resultado de la partida: " + String.join(", ", choices);
                    System.out.println(resultado);

                    // Mostrar resultado a los jugadores y preguntar si quieren continuar
                    for (PlayerHandler player : players) {
                        player.sendMessage(resultado);
                        player.sendMessage("¿Deseas continuar jugando? (sí/no)");

                        String response = player.receiveMessage().trim().toLowerCase();
                        if (!response.equals("sí")) {
                            player.setWantsToContinue(false);
                        }
                    }

                    // Verificar si todos los jugadores quieren continuar
                    continuePlaying = players.stream().allMatch(PlayerHandler::wantsToContinue);

                } catch (Exception e) {
                    System.err.println("Error en la sesión de juego: " + e.getMessage());
                    break;
                }
            }

            // Terminar la sesión y notificar a los jugadores
            for (PlayerHandler player : players) {
                player.sendMessage("Gracias por jugar. ¡Hasta luego!");
                player.close();
            }
        }
    }
}
