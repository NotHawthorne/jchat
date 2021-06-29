package main.java.JChat;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please designate either server or client to launch!");
            return ;
        }
        else {
            System.out.println(args[0]);
            if (args[0].equals("server"))
            {
                System.out.println("hi");
                main.java.JChat.Server s = new main.java.JChat.Server();
            }
            else if (args[0].equals("client"))
            {
                main.java.JChat.Client c = new main.java.JChat.Client(args[1], Integer.valueOf(args[2]));
            }
        }
    }
}
