package edu.uchc.cam.langevin.counter;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import edu.uchc.cam.langevin.g.reaction.*;
import edu.uchc.cam.langevin.langevinnovis01.Global;
import edu.uchc.cam.langevin.langevinnovis01.MySystem;


public class ReactionCounter {

    private final MySystem sys;

    private final double dtdata;    // time interval between data points
    private final int totalCount;   // number of data points

    // A counter to tell us how many times we've taken data (data point index)
    // counter == 0  is the initial condition before any reactions have occurred,
    // counter == 1 is the first data point after the first dtdata time interval, etc.
    private int counter = 1;
    private final double [] time;

    private final ArrayList<GReactionInterface> reactionList = new ArrayList<>();

    // key=reaction names, value= array of reaction occurrences for each datapoint
    private final HashMap<String, int[]> totals = new LinkedHashMap<>();
    private final HashMap<String, int[]> creations = new LinkedHashMap<>();
    private final HashMap<String, int[]> decays = new LinkedHashMap<>();
    private final HashMap<String, int[]> bindings = new LinkedHashMap<>();
    private final HashMap<String, int[]> dissociations = new LinkedHashMap<>();
    private final HashMap<String, int[]> transitions = new LinkedHashMap<>();
    private final HashMap<String, int[]> allosterics = new LinkedHashMap<>();

    public ReactionCounter(Global g, MySystem sys) {
        this.sys = sys;
        dtdata = g.getdtdata();
        totalCount = 1 + (int)Math.floor(g.getTotalTime() / g.getdtdata());
        time = new double[totalCount];

        for (GDecayReaction r : g.getDecayReactions()) {
            String name = r.getName();
            totals.put(name, new int[totalCount]);
            creations.put(name, new int[totalCount]);
            decays.put(name, new int[totalCount]);
        }
        for (GBindingReaction r : g.getBindingReactions()) {
            String name = r.getName();
            totals.put(name, new int[totalCount]);
            bindings.put(name, new int[totalCount]);
            dissociations.put(name, new int[totalCount]);
        }
        for (GTransitionReaction r : g.getTransitionReactions()) {
            String name = r.getName();
            totals.put(name, new int[totalCount]);
            transitions.put(name, new int[totalCount]);
        }
        for (GAllostericReaction r : g.getAllostericReactions()) {
            String name = r.getName();
            totals.put(name, new int[totalCount]);
            allosterics.put(name, new int[totalCount]);
        }
    }

    // we refresh the content of the count hashmap in real time, every time a reaction happens
    // here we just set the index of the current datapoint (count) which will be used to store the number
    // of occurrences for each reaction in the count hashmap
    public void initDatapoint() {
        if(counter < time.length) {
            double ttt = sys.getTime();    // error accumulates
            time[counter] = counter * dtdata;   // error doesn't accumulate, although even counter * dtdata had a small error at times
            counter++;
        }
    }

    // utility method, we display the total number of occurrences for each reaction type at the end of the simulation
    public void printCounts() {
        System.out.println("=== AllReactions ===");
        for (String name : totals.keySet()) {
            int[] arr = totals.get(name);
            int total = 0;
            for (int v : arr) {
                total += v;
            }
            System.out.println(name + " " + total);
        }
    }
    public void printDetailedCounts() {
        System.out.println("=== " + ReactionType.CREATION.longName + "s ===");
        for (String name : creations.keySet()) {
            int sum = 0;
            for (int v : creations.get(name)) sum += v;
            System.out.println(name + " " + sum);
        }
        System.out.println("=== " + ReactionType.DECAY.longName + "s ===");
        for (String name : decays.keySet()) {
            int sum = 0;
            for (int v : decays.get(name)) sum += v;
            System.out.println(name + " " + sum);
        }
        System.out.println("=== " + ReactionType.BINDING.longName + "s ===");
        for (String name : bindings.keySet()) {
            int sum = 0;
            for (int v : bindings.get(name)) sum += v;
            System.out.println(name + " " + sum);
        }
        System.out.println("=== " + ReactionType.DISSOCIATION.longName + "s ===");
        for (String name : dissociations.keySet()) {
            int sum = 0;
            for (int v : dissociations.get(name)) sum += v;
            System.out.println(name + " " + sum);
        }
        System.out.println("=== " + ReactionType.TRANSITION.longName + "s ===");
        for (String name : transitions.keySet()) {
            int sum = 0;
            for (int v : transitions.get(name)) sum += v;
            System.out.println(name + " " + sum);
        }
        System.out.println("=== " + ReactionType.ALLOSTERIC.longName + "s ===");
        for (String name : allosterics.keySet()) {
            int sum = 0;
            for (int v : allosterics.get(name)) sum += v;
            System.out.println(name + " " + sum);
        }
    }


    public enum ReactionType {

        CREATION("CreationReaction", "CREATION"),
        DECAY("DecayReaction", "DECAY"),
        BINDING("BindingReaction", "BINDING"),
        DISSOCIATION("DissociationReaction", "UNBINDING"),
        TRANSITION("TransitionReaction", "TRANSITION"),
        ALLOSTERIC("AllostericReaction", "ALLOSTERIC");

        public final String longName;
        public final String shortName;
        ReactionType(String longName, String shortName) {
            this.longName = longName;
            this.shortName = shortName;
        }
        public String formatColumnName(String reactionName) {
            return shortName + "_" + reactionName;
        }
    }

    public void plusCreationReaction(String name) {
//        System.out.println(" --------- " + creationReaction + ": " + name);
        totals.get(name)[counter]++;
        creations.get(name)[counter]++;
    }
    public void plusDecayReaction(String name) {
//        System.out.println(" --------- " + decayReaction + ": " + name);
        totals.get(name)[counter]++;
        decays.get(name)[counter]++;
    }
    public void plusBindingReaction(String name) {
//        System.out.println(" --------- " + bindingReaction + ": " + name);
        totals.get(name)[counter]++;
        bindings.get(name)[counter]++;
    }
    public void plusDissociationReaction(String name) {
//        System.out.println(" --------- " + dissociationReaction + ": " + name);
        totals.get(name)[counter]++;
        dissociations.get(name)[counter]++;
    }
    public void plusTransitionReaction(String name) {
//        System.out.println(" --------- " + transitionReaction + ": " + name);
        totals.get(name)[counter]++;
        transitions.get(name)[counter]++;
    }
    public void plusAllostericReaction(String name) {
//        System.out.println(" --------- " + allostericReaction + ": " + name);
        totals.get(name)[counter]++;
        allosterics.get(name)[counter]++;
    }

    /******************************************************************\
     *                         WRITE DATA                             *
     * @param path                                                    *
    \******************************************************************/
    public void writeFullData(File path) {

        File out = new File(path, "FullTotalReactionsCountData.csv");
        try (PrintWriter p = new PrintWriter(new FileWriter(out), true)) {
            //
            // ---------- HEADER
            //
            p.print("Time,");
            for (String name : creations.keySet()) {
                p.print(ReactionType.CREATION.formatColumnName(name) + ",");
            }
            for (String name : decays.keySet()) {
                p.print(ReactionType.DECAY.formatColumnName(name) + ",");
            }
            for (String name : bindings.keySet()) {
                p.print(ReactionType.BINDING.formatColumnName(name) + ",");
            }
            for (String name : dissociations.keySet()) {
                p.print(ReactionType.DISSOCIATION.formatColumnName(name) + ",");
            }
            for (String name : transitions.keySet()) {
                p.print(ReactionType.TRANSITION.formatColumnName(name) + ",");
            }
            for (String name : allosterics.keySet()) {
                p.print(ReactionType.ALLOSTERIC.formatColumnName(name) + ",");
            }
            p.println();
            //
            // ---------- DATA ROWS
            //
            for (int i = 0; i < totalCount; i++) {

                double t = Math.round(time[i] * 1e12) / 1e12;   // Clean time rounding
                p.print(t + ",");

                for (String name : creations.keySet()) {
                    p.print(creations.get(name)[i] + ",");
                }
                for (String name : decays.keySet()) {
                    p.print(decays.get(name)[i] + ",");
                }
                for (String name : bindings.keySet()) {
                    p.print(bindings.get(name)[i] + ",");
                }
                for (String name : dissociations.keySet()) {
                    p.print(dissociations.get(name)[i] + ",");
                }
                for (String name : transitions.keySet()) {
                    p.print(transitions.get(name)[i] + ",");
                }
                for (String name : allosterics.keySet()) {
                    p.print(allosterics.get(name)[i] + ",");
                }
                p.println();
            }

        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }



}
