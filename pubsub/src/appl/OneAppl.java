package appl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import core.Message;

import javax.swing.plaf.synth.SynthOptionPaneUI;

public class OneAppl {

    public OneAppl() {
        PubSubClient client = new PubSubClient();
        client.startConsole();
    }

    public OneAppl(boolean flag) {
        //Lendo a porta e endereço do client e broker
        /*
        Scanner reader = new Scanner(System.in);  // Reading from System.in
        System.out.print("Enter the client address: ");
        String clientAddress = reader.next();
        System.out.print("Enter the client port number: ");
        int clientPort = reader.nextInt(); // Scans the next token of the input as an int.
        System.out.print("Enter the broker address: ");
        String brokerAddress = reader.next();
        System.out.print("Enter the broker port number: ");
        int brokerPort = reader.nextInt(); // Scans the next token of the input as an int.
        */


        String brokerAddress = "localhost";
        int brokerPort = 8090;
        String clientAddress = "localhost";
        int clientPort = 8092;

        //Criando client
        //O client é composto de um name e internamente possui um contador de operações.
        //A primeira interação com o broker é a operação 1, a segunda, operação 2...
        //Isso é usado para identificar conjuntos de lock, publish e unlock pertencentes a mesma operação
        PubSubClient client1 = new PubSubClient("client1", clientAddress, 8092);
        //PubSubClient client2 = new PubSubClient("client2", clientAddress, 8093);
        //PubSubClient client3 = new PubSubClient("client3", clientAddress, 8094);

        //Subscrevendo client
        client1.subscribe(brokerAddress, brokerPort);
        //client2.subscribe(brokerAddress, brokerPort);
        //client3.subscribe(brokerAddress, brokerPort);

        //Criando a thread que vai tentar dar lock no topico, publicar e então dar unlock
        Thread accessOne = new ThreadLockPubUnlock(client1, "varX", brokerAddress, brokerPort);
        //Thread accessTwo = new ThreadLockPubUnlock(client2, "varX", brokerAddress, brokerPort);
        //Thread accessThree = new ThreadLockPubUnlock(client3, "varX", brokerAddress, brokerPort);

        //Executando o metodo run da thread
        accessOne.start();
        //accessTwo.start();
        //accessThree.start();

        //Aguardando a conclusão da execução das threads
        try {
            //accessTwo.join();
            accessOne.join();
            //accessThree.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //desinscrevendo client
        //client1.unsubscribe(brokerAddress, brokerPort);
        //client2.unsubscribe(brokerAddress, brokerPort);
        //client3.unsubscribe(brokerAddress, brokerPort);

        //client1.stopPubSubClient();
        //client2.stopPubSubClient();
        //client3.stopPubSubClient();

       // System.exit(0);
    }

    public static void main(String[] args) {
        new OneAppl(true);
    }

    //A partir da mensagem recebida, essa thread decide se vai fazer um lock, unlock ou publish em um topico
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
            if(msg.equals("lock")){
                c.intentLock(host, port);
            }else if(msg.equals("unlock")){
                c.unlock(host, port);
            }else{
                c.publish(msg, host, port);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //Thread responsavel por identificar quando um lock para o client foi feito e ele pode realizar o publish
    class ThreadLockPubUnlock extends Thread {
        PubSubClient c;
        String topic;
        String clientName;
        String host;
        int port;

        public ThreadLockPubUnlock(PubSubClient c, String topic, String host, int port) {
            this.c = c;
            this.topic = topic;
            this.clientName = c.getClientName()+" Op"+Integer.toString(c.getOperationId())+" ";
            this.host = host;
            this.port = port;
        }
        public void run() {
            //No inicio da execução, todos os clients desejam escrever no topico, então todos apresentam a requisição de lock
            Thread lock = new ThreadWrapper(c, "lock", host, port);
            lock.start();
            try {
                lock.join();
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<Message> log = c.getLogMessages();
            //Análisando o log, decidimos se é a hora ou não do client publicar na variavel
            log = c.getLogMessages();
            Iterator<Message> it2 = log.iterator();

            while (it2.hasNext()) {
                Message aux = it2.next();
                System.out.print(aux.getContent() + " || ");
            }
            System.out.println();


            //Impressão dos logs de operação para cada client
            log = c.getLogMessages();
            Iterator<Message> it = log.iterator();
            it = log.iterator();
            System.out.print("Log "+ c.getClientName()+" Op "+Integer.toString(c.getOperationId())+" -> ");
            while (it.hasNext()) {
                Message aux = it.next();
                System.out.print(aux.getContent() + aux.getLogId() + " | ");
            }
            System.out.println();
        }

    }

}

