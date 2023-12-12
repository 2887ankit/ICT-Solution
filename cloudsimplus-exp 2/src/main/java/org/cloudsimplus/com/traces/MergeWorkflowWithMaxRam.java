package org.cloudsimplus.com.traces;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

class JobEntry {
    // Define fields based on the columns in your data
    int jobID;
    int taskIndex;
    int machineID;
    double ram; // Add RAM field

    // Constructor
    public JobEntry(int jobID, int taskIndex, int machineID, double ram /*, other fields*/) {
        this.jobID = jobID;
        this.taskIndex = taskIndex;
        this.machineID = machineID;
        this.ram = ram;
    }
    // Implement equals and hashCode methods based on the criteria to identify duplicates
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        JobEntry jobEntry = (JobEntry) obj;
        return jobID == jobEntry.jobID;
        // Add other conditions if needed
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobID);
    }

    // Override toString to print the entry as CSV
    @Override
    public String toString() {
        return jobID + "," + taskIndex + "," + machineID + "," + ram;
        // Add other fields if needed
    }
}

public class MergeWorkflowWithMaxRam {

    public static void main(String[] args) {
        String inputFilePath = "/Users/ankitkumar/Desktop/testFiles/inputjob.csv";
        String outputFilePath = "output.csv";

        List<JobEntry> jobEntries = readDataFromCSV(inputFilePath);
        System.out.println("------------------Input----------------");
        System.out.println("jobID	taskIndex	machineID	RAM\n"+ "");
        for(JobEntry je:jobEntries) {
            System.out.println(je.toString());
        }
        List<JobEntry> uniqueEntries = removeDuplicates(jobEntries);
        List<JobEntry> resultEntries = findMaxRamForJob(uniqueEntries);
        System.out.println("--------------------output---------------");
        System.out.println("jobID	taskIndex	machineID	RAM\n"
        		+ "");
        for(JobEntry je:resultEntries) {
            System.out.println(je.toString());
        }
        writeDataToCSV(resultEntries, outputFilePath);
    }

    private static List<JobEntry> readDataFromCSV(String filePath) {
        List<JobEntry> jobEntries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                int jobID = Integer.parseInt(parts[0]);
                int taskIndex = Integer.parseInt(parts[1]);
                int machineID = Integer.parseInt(parts[2]);
                double ram = Double.parseDouble(parts[3]);

                jobEntries.add(new JobEntry(jobID, taskIndex, machineID, ram));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jobEntries;
    }

    private static List<JobEntry> removeDuplicates(List<JobEntry> jobEntries) {
        Set<JobEntry> uniqueSet = new HashSet<>(jobEntries);
        return new ArrayList<>(uniqueSet);
    }

    private static List<JobEntry> findMaxRamForJob(List<JobEntry> jobEntries) {
        Map<Integer, JobEntry> maxRamMap = new HashMap<>();

        for (JobEntry entry : jobEntries) {
            int jobID = entry.jobID;
            if (!maxRamMap.containsKey(jobID) || entry.ram > maxRamMap.get(jobID).ram) {
                maxRamMap.put(jobID, entry);
            }
        }

        return new ArrayList<>(maxRamMap.values());
    }

    private static void writeDataToCSV(List<JobEntry> jobEntries, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (JobEntry entry : jobEntries) {
                writer.write(entry.toString());
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
