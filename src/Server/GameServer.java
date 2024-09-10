package Server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

public class GameServer {
    private static final int PORT = 12345;
    private static List<ClientHandler> players = new ArrayList<>();
    private static Queue<ClientHandler> waitingQueue = new LinkedList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            JOptionPane.showMessageDialog(null, "Servidor de Piedra, Papel o Tijera iniciado.");

            // Escuchar conexiones entrantes
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler newPlayer = new ClientHandler(socket);
                waitingQueue.add(newPlayer);

                // Si hay al menos 3 jugadores esperando, iniciar una partida
                if (waitingQueue.size() >= 3) {
                    for (int i = 0; i < 3; i++) {
                        players.add(waitingQueue.poll());
                    }
                    // Iniciar el juego en un nuevo hilo
                    new Thread(new Game(players)).start();
                    players = new ArrayList<>();  // Reiniciar la lista de jugadores
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error en el servidor: " + e.getMessage());
        }
    }
}

class Game implements Runnable {
    private List<ClientHandler> players;

    public Game(List<ClientHandler> players) {
        this.players = players;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Pedir a los jugadores que jueguen
                for (ClientHandler player : players) {
                    player.sendMessage("Juega");
                }

                // Obtener las respuestas de los jugadores
                Map<ClientHandler, String> moves = new HashMap<>();
                for (ClientHandler player : players) {
                    String move = player.receiveMessage();
                    if (move.equalsIgnoreCase("Salir")) {
                        player.closeConnection();
                        return;
                    }
                    moves.put(player, move);
                }

                // Determinar el ganador
                String result = determineWinner(moves);

                // Enviar el resultado a todos los jugadores
                for (ClientHandler player : players) {
                    player.sendMessage("Resultado de la partida: " + result);
                }

                // Preguntar si desean continuar
                boolean allWantToContinue = true;
                for (ClientHandler player : players) {
                    player.sendMessage("¿Deseas continuar jugando? (sí/no)");
                    String response = player.receiveMessage();
                    if (!response.equalsIgnoreCase("sí")) {
                        allWantToContinue = false;
                    }
                }

                // Si algún jugador no quiere continuar, cerrar las conexiones
                if (!allWantToContinue) {
                    for (ClientHandler player : players) {
                        player.closeConnection();
                    }
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Lógica para determinar el ganador
    private String determineWinner(Map<ClientHandler, String> moves) {
        String[] options = {"Piedra", "Papel", "Tijera"};
        Map<String, String> rules = new HashMap<>();
        rules.put("Piedra", "Tijera");  // Piedra vence a Tijera
        rules.put("Papel", "Piedra");   // Papel vence a Piedra
        rules.put("Tijera", "Papel");   // Tijera vence a Papel

        Map<String, Integer> countMoves = new HashMap<>();
        for (String move : moves.values()) {
            countMoves.put(move, countMoves.getOrDefault(move, 0) + 1);
        }

        // Si todos hicieron la misma jugada o hay una triple combinación diferente
        if (countMoves.size() == 1 || countMoves.size() == 3) {
            return "Empate entre todos los jugadores";
        }

        // Encontrar la jugada ganadora
        String winningMove = "";
        for (String move : countMoves.keySet()) {
            if (countMoves.get(move) == 1 && rules.get(move) != null && countMoves.get(rules.get(move)) != null) {
                winningMove = move;
            }
        }

        // Obtener el/los ganador(es)
        List<ClientHandler> winners = new ArrayList<>();
        for (Map.Entry<ClientHandler, String> entry : moves.entrySet()) {
            if (entry.getValue().equals(winningMove)) {
                winners.add(entry.getKey());
            }
        }

        if (winners.size() == 1) {
            return "El ganador es: Jugador " + winners.get(0).getId();
        } else {
        	return "Empate entre los jugadores: " + winners.stream().map(player -> String.valueOf(player.getId())).collect(Collectors.joining(", "));

        }
    }
}

class ClientHandler {
    private static int idCounter = 1;
    private int id;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) throws IOException {
        this.id = idCounter++;
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public int getId() {
        return id;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String receiveMessage() throws IOException {
        return in.readLine();
    }

    public void closeConnection() throws IOException {
        socket.close();
        in.close();
        out.close();
    }
}
