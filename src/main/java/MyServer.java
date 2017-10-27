import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by haosun on 10/24/17.
 */
public class MyServer {
    private ServerSocket serverSocket;

    private static int serverPort = 3000;
    private static final int BUFF_SIZE = 20480000;

    MyServer(int serverPort) {
        this.serverPort = serverPort;
    }

    private void start() throws IOException, InterruptedException {
        serverSocket = new ServerSocket(serverPort);
        System.out.println("listening on port : " + serverPort);

        communicate(serverSocket.accept());
    }

    private void communicate(Socket socket) throws IOException, InterruptedException {
        String command;
        while (true) {
            if (socket.isClosed()) {
                System.out.println("socket closed");
            } else {
                System.out.println("socket open");
            }
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                command = in.readUTF();

                System.out.println(command);
                long start = System.currentTimeMillis();
                List<String> result = grep(command);
                send(out, result);
                long end = System.currentTimeMillis();
                System.out.println("Total time : " + (end - start) + " ms");

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //socket.close();
            }
        }
    }

    private List<String> grep(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        BufferedInputStream inputStream = new BufferedInputStream(process.getInputStream(), BUFF_SIZE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), BUFF_SIZE);
        List<String> output = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            output.add(line);
        }
        process.waitFor();
        return output;
    }

    private void send(ObjectOutputStream outputStream, List<String> result) throws IOException {
        outputStream.writeObject(result);
        outputStream.flush();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int serverPort = Integer.parseInt(args[0]);
        MyServer myServer = new MyServer(serverPort);
        myServer.start();
    }
}
