package playground.shivam.signals.writer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;

public class CSVWriter {
    public static void writeResult(String filename, List<String> values, boolean append) {
        FileWriter csvwriter;
        BufferedWriter bufferedWriter = null;
        try {
            csvwriter = new FileWriter(filename, append);
            bufferedWriter = new BufferedWriter(csvwriter);
            StringJoiner stringJoiner = new StringJoiner(",");
            for (var value: values) {
                stringJoiner.add(value);
            }
            bufferedWriter.write(stringJoiner.toString());
            bufferedWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert bufferedWriter != null;
                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
