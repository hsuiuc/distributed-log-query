import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * Created by haosun on 10/27/17.
 */
public class MyClient {
    private ExecutorService executorService;
    private int NUM_CONNECTIONS = 5;

    /**
     * class constructor
     */
    MyClient() {}
    MyClient(ExecutorService executorService, int numConnections) {
        this.executorService = executorService;
        this.NUM_CONNECTIONS = numConnections;
    }

    private List<Socket> connect() {
        List<Future<Socket>> socketFutures = new ArrayList<Future<Socket>>();
        for (int i = 0; i < NUM_CONNECTIONS; i++) {
            int serverPort = 3000 + i;
            Callable<Socket> socketToServerTask = createSocketToServerTask(serverPort);
            Future<Socket> socketFuture = executorService.submit(socketToServerTask);
            socketFutures.add(socketFuture);
        }

        List<Socket> socketsToServers = new ArrayList<>();
        while (socketFutures.size() > 0) {
            List<Future<Socket>> done = new ArrayList<>();
            for (Future<Socket> socketFuture : socketFutures) {
                if (socketFuture.isDone()) {
                    try {
                        Socket socket = socketFuture.get();
                        if (socket != null)
                            socketsToServers.add(socket);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    } finally {
                        done.add(socketFuture);
                    }
                }
            }
            socketFutures.removeAll(done);
        }

        return socketsToServers;
    }

    private Callable<Socket> createSocketToServerTask(final int serverPort) {
        return () -> new Socket(InetAddress.getLocalHost(), serverPort);
    }

    private void grep(List<Socket> socketsToServers) {
        Scanner reader = new Scanner(System.in);
        String command;
        long start = 0, end = 0;
        List<Future<Integer>> countFutures = new ArrayList<>();

        while (true) {
            System.out.println("input a command !");
            command = reader.nextLine();
            start = System.currentTimeMillis();

            countFutures.clear();
            for (Socket socketToServer : socketsToServers) {
                Callable<Integer> countFromServerTask = createCountFromServerTask(socketToServer, command);
                countFutures.add(executorService.submit(countFromServerTask));
            }

            int totalGrepRecords = 0;
            while (countFutures.size() > 0) {
                List<Future<Integer>> done = new ArrayList<>();
                for (Future<Integer> countFuture : countFutures) {
                    if (countFuture.isDone()) {
                        try {
                            int size = countFuture.get();
                            totalGrepRecords += size;
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        } finally {
                            done.add(countFuture);
                        }
                    }
                }
                countFutures.removeAll(done);
            }

            end = System.currentTimeMillis();
            System.out.println("total records : " + totalGrepRecords);
            System.out.println("total time : " + (end - start));
        }
    }

    private Callable<Integer> createCountFromServerTask(Socket socketToServer, String command) {
        return () -> {
            OutputStream outputStream = socketToServer.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();
            //dataOutputStream.close();

            InputStream inputStream = socketToServer.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            List<String> grepResultsFromServer = (List<String>) objectInputStream.readObject();
            //objectInputStream.wait();
            //objectInputStream.close();

            String hostName = socketToServer.getInetAddress().getHostName();
            int portOnServer = socketToServer.getPort();
            System.out.println(String.format("hostname : %s, port number : %d", hostName, portOnServer));

            FileWriter fileWriter = new FileWriter(hostName + portOnServer + ".log", true);
            for (String line : grepResultsFromServer) {
                fileWriter.write(line + "\n");
            }
            fileWriter.close();
            return grepResultsFromServer.size();
        };
    }

    public static void main(String[] args) {
        int numConnections = Integer.parseInt(args[0]);
        ExecutorService executorService = Executors.newFixedThreadPool(numConnections);
        MyClient myClient = new MyClient(executorService, numConnections);

        myClient.grep(myClient.connect());
    }
}
