/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2021 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.cloudsimplus.com.traces;

import ch.qos.logback.classic.Level;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.TextTableColumn;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.core.CloudSimTag;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.traces.TraceReaderAbstract;
import org.cloudsimplus.traces.google.BrokerManager;
import org.cloudsimplus.traces.google.GoogleTaskEventsTraceReader;
import org.cloudsimplus.traces.google.GoogleTaskUsageTraceReader;
import org.cloudsimplus.traces.google.TaskEvent;
import org.cloudsimplus.util.BytesConversion;
import org.cloudsimplus.util.Conversion;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.util.TimeUtil;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.cloudsimplus.util.BytesConversion.megaBytesToBytes;
import static org.cloudsimplus.util.MathUtil.positive;


public class TaskEvents {
    private static final String TASK_EVENTS_FILE = "/Users/ankitkumar/Desktop/testFiles/task-events-sample-1.csv";
    private static final String TASK_USAGE_FILE = "/Users/ankitkumar/Desktop/testFiles/task-usage-sample-1.csv";

    private static final int HOSTS = 32;
    private static final int VMS = 32;
    private static final int HOST_PES = 16;
    private static final long HOST_RAM = 4096; //in Megabytes
    private static final long HOST_BW = 10000; //in Megabits/s
    private static final long HOST_STORAGE = 1000000; //in Megabytes
    private static final double HOST_MIPS = 1000;

    private static final int  CLOUDLET_LENGTH = -10_000;


    private static final int MAX_CLOUDLETS = 32;

    private static final long VM_PES = 4;
    private static final int  VM_MIPS = 1000;
    private static final long VM_RAM = 1000; //in Megabytes
    private static final long VM_BW = 100; //in Megabits/s
    private static final long VM_SIZE_MB = 1000; //in Megabytes

    private final CloudSimPlus simulation;
    private List<DatacenterBroker> brokers;
    private Datacenter datacenter;
    private Collection<Cloudlet> cloudlets;
    private GoogleTaskEventsTraceReader taskEventsReader;

    public static void main(String[] args) {
        new TaskEvents();
    }

    private TaskEvents() {
        final double startSecs = TimeUtil.currentTimeSecs();
        System.out.printf("Simulation started at %s%n%n", LocalTime.now());
        Log.setLevel(Level.TRACE);

        simulation = new CloudSimPlus();
        datacenter = createDatacenter();

        createCloudletsAndBrokersFromTraceFile();
        brokers.forEach(broker -> broker.submitVmList(createVms()));
        readTaskUsageTraceFile();

        System.out.println("Brokers:");
        brokers.stream().sorted().forEach(b -> System.out.printf("\t%d - %s%n", b.getId(), b.getName()));
        System.out.println("Cloudlets:");
        cloudlets.stream().sorted().forEach(c -> System.out.printf("\t%s (job %d)%n", c, c.getJobId()));

        simulation.start();

        System.out.printf("Total number of created Cloudlets: %d%n", getTotalCreatedCloudletsNumber());
        brokers.stream().sorted().forEach(this::printCloudlets);
        System.out.print("EEEE"+brokers.size());
        System.out.printf(
            "Simulation finished at %s. Execution time: %.2f seconds%n",
            LocalTime.now(), TimeUtil.elapsedSeconds(startSecs));
    }

    private int getTotalCreatedCloudletsNumber() {
        return brokers.stream().mapToInt(b -> b.getCloudletCreatedList().size()).sum();
    }

    
    private void createCloudletsAndBrokersFromTraceFile() {
    	
        taskEventsReader =
            GoogleTaskEventsTraceReader
                .getInstance(simulation, TASK_EVENTS_FILE, this::createCloudlet)
                .setMaxCloudletsToCreate(MAX_CLOUDLETS);
        System.out.println();
        // By default, created Cloudlets are automatically submitted to their respective brokers.
        cloudlets = taskEventsReader.process();
        brokers = taskEventsReader.getBrokerManager().getBrokers();
        System.out.printf(
            "%d Cloudlets and %d Brokers created from the %s trace file.%n",
            cloudlets.size(), brokers.size(), TASK_EVENTS_FILE);
    }


    private Cloudlet createCloudlet(final TaskEvent event) {

        final long pesNumber = positive(event.actualCpuCores(VM_PES), VM_PES);

        final double maxRamUsagePercent = positive(event.getResourceRequestForRam(), Conversion.HUNDRED_PERCENT);
        final var utilizationRam = new UtilizationModelDynamic(0, maxRamUsagePercent);

        final double sizeInMB    = event.getResourceRequestForLocalDiskSpace() * VM_SIZE_MB + 1;
        final long   sizeInBytes = (long) Math.ceil(megaBytesToBytes(sizeInMB));
        return new CloudletSimple(CLOUDLET_LENGTH, pesNumber)
            .setFileSize(sizeInBytes)
            .setOutputSize(sizeInBytes)
            .setUtilizationModelCpu(new UtilizationModelFull())
            .setUtilizationModelBw(new UtilizationModelDynamic(0.25))
            .setUtilizationModelRam(utilizationRam);
    }

    private void readTaskUsageTraceFile() {
        final var reader = GoogleTaskUsageTraceReader.getInstance(taskEventsReader, TASK_USAGE_FILE);
        final var cloudletsCollection = reader.process();
        System.out.printf("%d Cloudlets processed from the %s trace file.%n", cloudletsCollection.size(), TASK_USAGE_FILE);
        System.out.println();
    }

    private Datacenter createDatacenter() {
        final var hostList = new ArrayList<Host>(HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            hostList.add(createHost());
        }

        //Uses a VmAllocationPolicySimple by default
        return new DatacenterSimple(simulation, hostList);
    }

    private long getVmSize(final Cloudlet cloudlet) {
        return cloudlet.getVm().getStorage().getCapacity();
    }

    private long getCloudletSizeInMB(final Cloudlet cloudlet) {
        return (long) BytesConversion.bytesToMegaBytes(cloudlet.getFileSize());
    }

    private Host createHost() {
        final var peList = createPesList(HOST_PES);
        //Uses a ResourceProvisionerSimple for RAM and BW
        final Host host = new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
        host.setVmScheduler(new VmSchedulerTimeShared());
        return host;
    }

    private List<Pe> createPesList(final int count) {
        final var peList = new ArrayList<Pe>(count);
        for(int i = 0; i < count; i++){
            //Uses a PeProvisionerSimple by default
            peList.add(new PeSimple(HOST_MIPS));
        }

        return peList;
    }

    private List<Vm> createVms() {
        return IntStream.range(0, VMS).mapToObj(i -> createVm()).toList();
    }

    private Vm createVm() {
        //Uses a CloudletSchedulerTimeShared by default
        return new VmSimple(VM_MIPS, VM_PES).setRam(VM_RAM).setBw(VM_BW).setSize(VM_SIZE_MB);
    }

    private void printCloudlets(final DatacenterBroker broker) {
        final String username = broker.getName().replace("Broker_", "");
        final var cloudletList = broker.getCloudletFinishedList();
        cloudletList.sort(Comparator.comparingLong(Cloudlet::getId));
        new CloudletsTableBuilder(cloudletList)
            .addColumn(new TextTableColumn("Job", "ID"), Cloudlet::getJobId, 0)
            .addColumn(new TextTableColumn("VM Size", "MB"), this::getVmSize, 7)
            .addColumn(new TextTableColumn("Cloudlet Size", "MB"), this::getCloudletSizeInMB, 8)
            .addColumn(new TextTableColumn("Waiting Time", "Seconds").setFormat("%.0f"), Cloudlet::getCreationWaitTime, 10)
            .setTitle("Simulation results for Broker " + broker.getId() + " representing the username " + username)
            .build();
    }
}