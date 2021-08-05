package appl;

import core.Message;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class OneAppl {

    public OneAppl() {
        PubSubClient client = new PubSubClient();
        client.startConsole();
    }

    public OneAppl(boolean flag) {
        //Lendo a porta e endereço do client e broker

        Scanner reader = new Scanner(System.in);  // Reading from System.in
        System.out.print("Enter the client address: ");
        String clientAddress = reader.next();
        System.out.print("Enter the client port number: ");
        int clientPort = reader.nextInt(); // Scans the next token of the input as an int.
        System.out.print("Enter the broker address: ");
        String brokerAddress = reader.next();
        System.out.print("Enter the broker port number: ");
        int brokerPort = reader.nextInt(); // Scans the next token of the input as an int.
        //System.out.print("Enter the number of times client should access");
        //int numberOfTimes = reader.nextInt();

        //String brokerAddress = "localhost";
        //int brokerPort = 8080;
        //String clientAddress = "localhost";
        //int clientPort = 8092;



      //  for (int i = 0; i < numberOfTimes; i++) {
            //Criando client
            //O client é composto de um name e internamente possui um contador de operações.
            //A primeira interação com o broker é a operação 1, a segunda, operação 2...
            //Isso é usado para identificar conjuntos de lock, publish e unlock pertencentes a mesma operação
            PubSubClient client1 = new PubSubClient("client1", clientAddress, clientPort);
            PubSubClient client2 = new PubSubClient("client2", clientAddress, clientPort++);
            PubSubClient client3 = new PubSubClient("client3", clientAddress, clientPort++);

            //Subscrevendo client
            client1.subscribe(brokerAddress, brokerPort);
            client2.subscribe(brokerAddress, brokerPort);
            client3.subscribe(brokerAddress, brokerPort);

            //Criando a thread que vai tentar dar lock no topico, publicar e então dar unlock
            Thread accessOne = new ThreadLockPubUnlock(client1, "varX", brokerAddress, brokerPort);
            Thread accessTwo = new ThreadLockPubUnlock(client2, "varX", brokerAddress, brokerPort);
            Thread accessThree = new ThreadLockPubUnlock(client3, "varX", brokerAddress, brokerPort);

            //Executando o metodo run da thread
            accessOne.start();
            accessTwo.start();
            accessThree.start();

            //Aguardando a conclusão da execução das threads
            try {
                accessTwo.join();
                accessOne.join();
                accessThree.join();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //desinscrevendo client
            client1.unsubscribe(brokerAddress, brokerPort);
            client2.unsubscribe(brokerAddress, brokerPort);
            client3.unsubscribe(brokerAddress, brokerPort);

            client1.stopPubSubClient();
            client2.stopPubSubClient();
            client3.stopPubSubClient();
       // }
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
            while (true){
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log = c.getLogMessages();
                //Iterador para buscar no log caso alguem tenha dado unlock
                Iterator<Message> it = log.iterator();
                it = log.iterator();

                while (it.hasNext()) {
                    //Pegamos uma mensagem do log
                    Message i = it.next();
                    String content = i.getContent();
                    try {
                        //Quebramos a mensagem nos " " pois a mensagem vai ter o formato: clientY OpX unlock
                        //String[0] = clientY String[1] = OpX String[2] = unlock
                        if(!content.contains("publishing") && !content.contains(":")){
                            //Queremos saber somente caso alguem tenha feito lock ou unlock. Os publising são removidos do log
                            String[] parts = content.split(" ");
//                            if(parts.length<3){
//                                for (String partesA : parts) {
//                                    System.out.println ("PARTS: "+ partesA + " " +parts.length);
//                                }
//                            }

                            if (parts.length == 3 && parts[2].equals("unlock")) {
                                //Se alguem deu unlock, quer dizer que a operação realizada já foi finalizada, então iremos remover os dados dessa operação do log
                                Iterator<Message> itRem = log.iterator();
                                while (itRem.hasNext()) {
                                    Message j = itRem.next();
                                    String contentRem = j.getContent();
                                    if(!content.contains("publishing")){
                                        String[] partsRem = contentRem.split(" ");
                                        if (partsRem.length == 3 && (partsRem[0]+partsRem[1]).equals((parts[0]+parts[1]))) {
                                            //Removemos tanto a mensagem de unlock para a operação/cliente encontrado quando unlock
                                            log.remove(j);
                                            log.remove(i);
                                            break;
                                        }
                                    }
                                }
                            } else {
                                if (parts.length < 3) {
                                    log.remove(i);
                                }
                            }
                        }else{
                            log.remove(i);
                        }
                       /* Iterator<Message> it2 = log.iterator();
                        while (it2.hasNext()) {
                            System.out.print(it2.next().getContent());
                        }
                        System.out.println();*/
                    } catch (Exception e) {
                        //Se encontramos um transação de unlock sem um par lock, ela tambem é removida, porem isso quer dizer que
                        //alguma desincronização ocorreu.
                        log.remove(i);
                        e.printStackTrace();
                    }
                }

                //Depois de termos removido todas as operações finalizadas do log, verificamos se é a vez do client atual de utilizar a variavel
                String isMyTime = null;
                try {
                    isMyTime = log.get(0).getContent();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //Se o lock estiver no nome do client atual
                if (isMyTime.equals(clientName + "intent_to_lock")) {
                    //Um publish é realizado no topico
                    Thread publishing = new ThreadWrapper(c, "locked to "+c.getClientName()+" Op "+Integer.toString(c.getOperationId())+" - publishing on "+this.topic, host, port);
                    publishing.start();
                    try {
                        publishing.join();
                        Thread.currentThread().sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //Após o publish, um unlock é realizado
                    Thread unlock = new ThreadWrapper(c, "unlock", host, port);
                    unlock.start();
                    try {
                        unlock.join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }

            }
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

