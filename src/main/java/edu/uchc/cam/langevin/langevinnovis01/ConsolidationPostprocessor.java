/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.counter.*;
import edu.uchc.cam.langevin.g.object.GMolecule;
import edu.uchc.cam.langevin.g.object.GState;
import edu.uchc.cam.langevin.g.reaction.GDecayReaction;
import edu.uchc.cam.langevin.helpernovis.IOHelp;
import edu.uchc.cam.langevin.helpernovis.Location;
import edu.uchc.cam.langevin.helpernovis.Rand;
import edu.uchc.cam.langevin.object.*;
import edu.uchc.cam.langevin.reaction.AllostericReactions;
import edu.uchc.cam.langevin.reaction.BindingReactions;
import edu.uchc.cam.langevin.reaction.TransitionReactions;
import org.vcell.data.LangevinPostprocessor;
import org.vcell.messaging.VCellMessaging;
import org.vcell.messaging.WorkerEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ConsolidationPostprocessor {

    private Global g;
    private final VCellMessaging vcellMessaging;
    private int numRuns;
    private final boolean useOutputFile;

    private String simulationName;       // model / simulation name (without extension)
    private File simulationFolder;       // top folder, where the input file is (and also the .ida and ,json files are)

    public ConsolidationPostprocessor(Global g, int numRuns, boolean useOutputFile, VCellMessaging vcellMessaging) {

        System.out.println("Constructor of consolidation postprocessor.");

        this.g = g;
        this.numRuns = numRuns;
        this.useOutputFile = useOutputFile;
        this.vcellMessaging = vcellMessaging;

        folderSetup();
    }

    private void folderSetup() {
        // <editor-fold defaultstate="collapsed" desc="Method Code">
        File inputFile = g.getInputFile();      // model / simulation input file (the .langevininput file)
        simulationName = inputFile.getName();

        String filePath = inputFile.getAbsolutePath();
        filePath = filePath.substring(0, filePath.length() - simulationName.length());  // Strip the file name off of the path
        simulationFolder = new File(filePath);

        int dotIndex = simulationName.lastIndexOf('.');     // Strip extension off the file name
        if (dotIndex > 0) {
            simulationName = simulationName.substring(0, dotIndex);
        } else {
            System.out.println("Expected an extension for the input file: " + simulationName);
        }
    }


        public void runConsolidation() throws InterruptedException {

            System.out.println("Running consolidation for " + numRuns + " tasks");
            TimeUnit.SECONDS.sleep(3);

            System.out.println("Done!");
    }

}
