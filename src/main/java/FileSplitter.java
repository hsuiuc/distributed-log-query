import java.io.*;

/**
 * Created by haosun on 10/26/17.
 */
public class FileSplitter {
    private File file;

    FileSplitter(String file) {
        this.file = new File(file);
    }

    public void split() throws IOException {
        int indicator = 0;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            File[] outputFile = new File[5];
            BufferedWriter[] outputFileWriter = new BufferedWriter[5];
            for (int i = 0; i < 5; i++) {
                outputFile[i] = new File(String.format("file_%d.log", i));
                if (!outputFile[i].exists())
                    outputFile[i].createNewFile();
                outputFileWriter[i] = new BufferedWriter(new FileWriter(outputFile[i]));
            }

            String line = "";
            System.out.println("Reading file using Buffered Reader");

            while ((line = bufferedReader.readLine()) != null) {
                outputFileWriter[indicator].write(line);
                outputFileWriter[indicator].newLine();
                indicator = (indicator + 1) % 5;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String file = args[0];
        FileSplitter fileSplitter = new FileSplitter(file);
        fileSplitter.split();
    }
}
