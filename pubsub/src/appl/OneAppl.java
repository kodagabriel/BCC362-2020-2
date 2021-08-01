package appl;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import core.Message;

public class OneAppl {

    public OneAppl() {
        PubSubClient client = new PubSubClient();
        client.startConsole();
    }

    public OneAppl(boolean flag, String nameClient, String addressClient, int portClient, String addressHost, int portHost) {
        PubSubClient joubert = new PubSubClient(addressClient, portClient);

        joubert.subscribe(addressHost, portHost);
        Thread accessOne = new ThreadWrapper(joubert, "access " + nameClient + " - var X", addressHost, portHost);

        //accessOne = new ThreadWrapper(joubert, "access Joubert- var X", "localhost", 8080);
        accessOne.start();

        try {
            accessOne.join();
        } catch (Exception e) {

        }

        List<Message> logJoubert = joubert.getLogMessages();

        Iterator<Message> it = logJoubert.iterator();
        System.out.print("Log " + nameClient + " itens: ");
        while (it.hasNext()) {
            Message aux = it.next();
            if (!aux.getContent().contains(addressClient)) {
                System.out.print(aux.getContent() + aux.getLogId() + " | ");
            }
        }
        System.out.println();

        joubert.unsubscribe("localhost", 8080);

        joubert.stopPubSubClient();
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Scanner reader = new Scanner(System.in);  // Reading from System.in
        System.out.print("Enter the name for client: ");
        String nameClient = reader.next();
        System.out.print("Enter the address for client: ");
        String IPClient = reader.next();
        System.out.print("Enter the port for client: ");
        int portClient = reader.nextInt();

        System.out.print("Enter the address for host: ");
        String IPHost = reader.next();
        System.out.print("Enter the port for host: ");
        int portHost = reader.nextInt();

        new OneAppl(true, nameClient, IPClient, portClient, IPHost, portHost);
    }

    class ThreadWrapper extends Thread {
        PubSubClient c;
        String msg;
        String host;
        int port;

        public ThreadWrapper(PubSubClient c, String msg, String host, int port) {
            this.c = c;
            this.msg = msg;
            this.host = host;
            this.port = port;
        }

        public void run() {
            c.publish(msg, host, port);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
