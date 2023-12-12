package org.cloudsimplus.com.traces;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileProcessor {

    public static void main(String[] args) {
        String filePath = "/task-usage-sample-1.csv";

        // Read file content
        String fileContent = readFile(filePath);
        int jobid = 7;
        double meancpu = 0.70;
        // Add a new row
        String newRow = "10000000\t50000000\t"+jobid+"\t2\t0\t0.7\t0.25\t0.09\t0.0015\t0.0025\t0.08\t0.00386\t0.00029\t0.0497\t0.0006\t2.85\t0.0082\t0\t1\t0\n";
        fileContent += newRow;

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
