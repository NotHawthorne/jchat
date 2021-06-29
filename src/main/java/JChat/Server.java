package main.java.JChat;

import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class Server {
    private ServerSocket server = null;
    private BufferedInputStream in = null;

    private int port = 4242;
    private String defaultChannel = "Homeroom";
    private static Map<String, ClientHandler> ConnectedClients = new HashMap<String, ClientHandler>();
    private static Map<String, ArrayList<ClientHandler>> Channels = new HashMap<String, ArrayList<ClientHandler>>();

    public Server() {
        try
        {
            server = new ServerSocket(port);
            server.setReuseAddress(true);
            System.out.println("JChat.Server started");
            this.run();
        }
        catch (IOException i)
        {
            System.out.println(i);
        }
        finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException i) {
                    i.printStackTrace();
                }
            }
        }
    }

    public void run() {
        try {
            while (true) {
                Socket cli = server.accept();
                System.out.println("JChat.Client accepted." + cli.getInetAddress().getHostAddress());
                ClientHandler clientSock = new ClientHandler(cli);
                new Thread(clientSock).start();
                ConnectedClients.put(clientSock.getUsername(), clientSock);
                System.out.println(ConnectedClients.get(clientSock.getUsername()).getUsername());
            }
        }
        catch (IOException i)
        {
            System.out.println(i);
        }
    }

    public static void sendGlobalMessage(String sender, String msg) {
        for (String key : Server.ConnectedClients.keySet())
        {
            Server.ConnectedClients.get(key).send(sender, msg, "Global");
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private String username = "New User";
        private PrintWriter out = null;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try
            {
                this.username = UUID.randomUUID().toString();
                this.out = new PrintWriter(socket.getOutputStream(), true);
                Server.sendGlobalMessage("Server", this.username + " joins the room.");
            }
            catch (IOException i)
            {
                System.out.println(i);
            }
        }

        public void run()
        {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                String line = "";
                boolean quit = false;
                while ((line = in.readLine()) != null && quit != true && line.length() > 0) {
                    System.out.println("RECV " + line);
                    String[] splitStr = line.split(" ", 2);
                    switch (splitStr[0]) {
                        case "/username":
                            // must be unique and have no spaces
                            if (Server.ConnectedClients.containsKey(splitStr[1]) || splitStr[1].contains(" ")) {
                                this.send("Server", "Invalid username.", null);
                                break ;
                            }
                            Server.sendGlobalMessage("JChat.Server", String.format("%s changes their name to %s.", this.username, splitStr[1]));
                            Server.ConnectedClients.remove(this.getUsername());
                            this.setUsername(splitStr[1]);
                            Server.ConnectedClients.put(this.getUsername(), this);
                            break ;
                        case "/quit":
                            quit = true;
                            break ;
                        case "/tell":
                            String[] msg = splitStr[1].split(" ", 2);
                            Server.ConnectedClients.get(msg[0]).send(this.getUsername(), msg[1], null);
                            this.send(this.username, "to " + msg[0] + ": " + msg[1], null);
                            break ;
                        case "/join":
                            // no spaces in server names
                            if (splitStr[1].contains(" ")) {
                                this.send("Server", "Invalid channel name.", null);
                            }
                            else {
                                // channel doesn't exist
                                if (Server.Channels.get(splitStr[1]) == null) {
                                    Server.Channels.put(splitStr[1], new ArrayList<ClientHandler>());
                                }
                                Server.Channels.get(splitStr[1]).add(this);
                                this.send("Server", "Joined channel " + splitStr[1], null);
                            }
                            break ;
                        case "/who":
                            String list = "Online Users";
                            if (splitStr.length > 1) {
                                list += " in room " + splitStr[1];
                                for (int i = 0; i != Server.Channels.get(splitStr[1]).size(); i++) {
                                    list += "\n" + Server.Channels.get(splitStr[1]).get(i).getUsername();
                                }
                                list = String.format("%d %s", Server.Channels.get(splitStr[1]).size(), list);
                            }
                            else
                            {
                                for (String client : Server.ConnectedClients.keySet()) {
                                    list += "\n" + client;
                                }
                                list = String.format("%d %s", Server.ConnectedClients.size(), list);
                            }
                            this.send("Server", list, null);
                        default:
                            if (splitStr[0].charAt(0) == '/') {
                                String chan = splitStr[0].substring(1);
                                ArrayList<ClientHandler> userList = null;
                                if ((userList = Server.Channels.get(chan)) != null) {
                                    for (int i = 0; i != userList.size(); i++) {
                                        // iterate through clients, see if we are in the desired channel
                                        if (userList.get(i).getUsername() == this.getUsername()) {
                                            //echo message
                                            for (int x = 0; x != userList.size(); x++) {
                                                userList.get(x).send(this.username, splitStr[1], chan);
                                            }
                                        }
                                    }
                                }
                            }
                            else {
                                Server.sendGlobalMessage(this.username, line);
                            }
                            break ;
                    }
                }
            }
            catch (IOException i)
            {
                System.out.println(i);
            }
            finally {
                try {
                    if (out != null)
                        out.close();
                    if (in != null) {
                        in.close();
                        clientSocket.close();
                    }
                    Server.sendGlobalMessage("Server", this.getUsername() + " leaves the room.");
                    Server.ConnectedClients.remove(this.username);
                    for (String key : Server.Channels.keySet()) {
                        for (int i = 0; i != Server.Channels.get(key).size(); i++) {
                            if (Server.Channels.get(key).get(i) == this) {
                                Server.Channels.get(key).remove(i);
                            }
                        }
                    }
                }
                catch (IOException i) {
                    i.printStackTrace();
                }
                System.out.println("Closing.");
            }
        }

        public void send(String sender, String msg, String channel) {
            // if we received a message without an associated channel, it's a private message
            Date d = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
            String formattedDate = sdf.format(d);
            if (channel == null)
                if (sender.equals(this.getUsername()))
                    this.out.println(String.format("[%s] %s", formattedDate, msg));
                else
                    this.out.println(String.format("[%s] from %s: %s", formattedDate, sender, msg));
            else
                this.out.println(String.format("[%s] [%s] %s: %s", formattedDate, channel, sender, msg));
        }

        public void setUsername(String s) { this.username = s; }
        public String getUsername() { return this.username; }
    }
}

