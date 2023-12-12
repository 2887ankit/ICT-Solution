package org.cloudsimplus.com.traces;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileProcessor1 {

    public static void main(String[] args) {
        String filePath = "/task-events-sample-1.csv";

        // Read file content
        String fileContent = readFile(filePath);
        int i = 7;
        // Add a new row
        String newRow = "10000000\t\t"+i+"\t"+1+"\t1\t"+1+"\tRyCO/1PCdI6fV/w+5a72xg=1\t2\t7\t0.03125\t0.08691\t0.22745\t0\n";
        fileContent += newRow;

        // Write the modified content back to the file
        writeFile(filePath, fileContent);
        // Add a new row
        String newRow1 = "0\t\t"+i+"\t"+1+"\t1\t"+0+"\tRyCO/1PCdI6fV/w+5a72xg=1\t2\t7\t0.03125\t0.08691\t0.22745\t0\n";
        fileContent += newRow1;

        // Write the modified content back to the file
        writeFile(filePath, fileContent);
        
        String newRow2 = "50000000\t\t"+i+"\t"+1+"\t1\t"+4+"\tRyCO/1PCdI6fV/w+5a72xg=1\t2\t7\t0.03125\t0.08691\t0.22745\t0\n";
        fileContent += newRow2;

        // Write the modified content back to the file
        writeFile(filePath, fileContent);
    }

    private static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content.toString();
    }

    private static void writeFile(String filePath, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

