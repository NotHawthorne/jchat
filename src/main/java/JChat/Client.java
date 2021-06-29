package main.java.JChat;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

public class Client {
    private Socket socket               = null;
    private BufferedInputStream input   = null;
    private static PrintWriter output     = null;
    private BufferedReader reader       = null;
    static public BufferedReader serverReader = null;
    public static String buf = "";

    private String address              = "127.0.0.1";
    private int port                    = 4242;

    public String getAddress() { return address; }
    public int getPort() { return port; }

    UIManager uiManager = null;

    //tui
    private class UIManager implements Runnable{
        private static Terminal terminal = null;
        private static Screen screen = null;
        private Panel panel = null;
        private BasicWindow window = null;
        private static WindowBasedTextGUI gui = null;

        private static TextBox inputBox = null;
        private static TextBox displayLog = null;

        private static int ColCount = 80;

        public UIManager() throws IOException {
            this.terminal = new DefaultTerminalFactory().createTerminal();
            this.screen = new TerminalScreen(this.terminal);
            this.screen.startScreen();
            this.panel = new Panel();
            this.panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
            this.inputBox = new TextBox(new TerminalSize(800, 1));
            this.displayLog = new TextBox(new TerminalSize(this.ColCount, 30), TextBox.Style.MULTI_LINE);
            this.displayLog.setReadOnly(true);
            this.displayLog.setEnabled(false);
            this.panel.addComponent(displayLog);
            this.panel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
            this.panel.addComponent(this.inputBox);
            this.window = new BasicWindow();
            this.window.setComponent(this.panel);
            this.panel.setPreferredSize(new TerminalSize(800, 600));
            this.window.setHints(Arrays.asList(Window.Hint.FIT_TERMINAL_WINDOW));
            this.gui = new MultiWindowTextGUI(this.screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLACK));
            this.gui.addWindow(this.window);
            this.gui.updateScreen();
        }

        public static void displayMessage(String s) {
            int breaks = s.length() / (displayLog.getSize().getColumns() - 1);
            int colsize = displayLog.getSize().getColumns();
            System.out.println(s.length());
            System.out.println(displayLog.getSize().getColumns());
            for (int i = 0; i <= breaks && i + (i * colsize) < s.length(); i++) {
                if (displayLog.getLineCount() > 18)
                    displayLog.removeLine(0);
                displayLog.addLine(s.substring(i + (i * colsize), i + ((i + 1) * colsize) > s.length() ? s.length(): i + ((i + 1) * colsize)));
                System.out.println("[" +s.substring(i + (i * colsize)));

            }
        }

        public static String getInput() {
            return inputBox.getText();
        }

        public static void refresh() throws IOException {
            gui.updateScreen();
        }

        public void run() {
            System.out.println("Waiting for input...");
            while (true) {
                try {
                    KeyStroke ks = null;
                    if ((ks = screen.readInput()).getKeyType() != KeyType.Enter) {
                        inputBox.handleKeyStroke(ks);
                        inputBox.setText(inputBox.getText());
                    } else {
                        System.out.println("Sending " + inputBox.getText());
                        output.print(inputBox.getText() + "\n");
                        inputBox.setText("");
                        output.flush();
                    }
                    this.gui.updateScreen();
                }
                catch (IOException i)
                {
                    System.out.println(i);
                }
            }
        }
    }

    public Client(String address, int port)
    {
        try
        {
            this.socket = new Socket(this.getAddress(), this.getPort());
            System.out.println("Connected.");
            this.input = new BufferedInputStream(System.in);
            this.output = new PrintWriter(socket.getOutputStream());
            this.serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // reader for text input -> data stream
            this.reader = new BufferedReader(new InputStreamReader(this.input, StandardCharsets.UTF_8));
            new Thread(new MessageHandler()).start();
            new Thread(new UIManager()).start();
        }
        catch (UnknownHostException u)
        {
            System.out.println(u);
        }
        catch (IOException i)
        {
            System.out.println(i);
        }
    }

    public int exit() {
        try {
            input.close();
            output.close();
            socket.close();
        }
        catch (IOException i)
        {
            System.out.println(i);
        }
        return (-1);
    }

    private static class MessageHandler implements Runnable
    {
        public void run() {
            try {
                while (true) {
                    String line = null;
                    while ((line = Client.serverReader.readLine()) != null) {
                        UIManager.displayMessage(line);
                        UIManager.refresh();

                        line = null;
                    }
                }
            }
            catch (IOException i)
            {
                System.out.println(i);
            }
        }
    }
}
