package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.helpernovis.OnRateSolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.vcell.messaging.VCellMessaging;
import org.vcell.messaging.VCellMessagingLocal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Arrays;

public class SimulationOptionsTest {

    String inputFileContents =
            """
            Total time: 0.0025
            dt: 1.0E-8
            dt_data: 5.0E-4
            dt_spring: 1.0E-9
            dt_image: 1.0E-4
            
            *** SYSTEM INFORMATION ***
            L_x: 0.1
            L_y: 0.1
            L_z_out: 0.01
            L_z_in: 0.09
            Partition Nx: 10
            Partition Ny: 10
            Partition Nz: 10
            
            *** MOLECULES ***
            
            MOLECULE: "MT0" Intracellular Number 20 Site_Types 2 Total_Sites 2 Total_Links 1 is2D false
            {
                 TYPE: Name "Site0" Radius 1.00000 D 1.000 Color RED STATES "state0"
                 TYPE: Name "Site1" Radius 1.00000 D 1.000 Color RED STATES "state0" "state1"
            
                 SITE 0 : Intracellular : Initial State 'state0'
                      TYPE: Name "Site0" Radius 1.00000 D 1.000 Color RED STATES "state0"
                      x 0.00000 y 0.00000 z 0.00000
                 SITE 1 : Intracellular : Initial State 'state0'
                      TYPE: Name "Site1" Radius 1.00000 D 1.000 Color RED STATES "state0" "state1"
                      x 0.00000 y 0.00000 z 0.00000
            
                 LINK: Site 0 ::: Site 1
            
                 Initial_Positions: Random
            }
            
            *** MOLECULE FILES ***
            
            MOLECULE: MT0 null
            
            *** CREATION/DECAY REACTIONS ***
            
            'MT0' : kcreate  0  kdecay  0
            
            *** STATE TRANSITION REACTIONS ***
            
            'r0' ::     'MT0' : 'Site1' : 'state0' --> 'state1'  Rate 50.0  Condition Free
            
            *** ALLOSTERIC REACTIONS ***
            
            
            *** BIMOLECULAR BINDING REACTIONS ***
            
            'r1'       'MT0' : 'Site0' : 'Any_State'  +  'MT0' : 'Site0' : 'Any_State'  kon  10.0  koff 23.0  Bond_Length 1.0
            
            *** MOLECULE COUNTERS ***
            
            'MT0' : Measure Total Free Bound
            
            *** STATE COUNTERS ***
            
            'MT0' : 'Site0' : 'state0' : Measure Total Free Bound
            'MT0' : 'Site1' : 'state0' : Measure Total Free Bound
            'MT0' : 'Site1' : 'state1' : Measure Total Free Bound
            
            *** BOND COUNTERS ***
            
            'r1' : Counted
            
            *** SITE PROPERTY COUNTERS ***
            
            'MT0' Site 0 : Track Properties true
            'MT0' Site 1 : Track Properties true
            
            *** CLUSTER COUNTERS ***
            
            Track_Clusters: true
            
            *** SYSTEM ANNOTATIONS ***
            
            
            *** MOLECULE ANNOTATIONS ***
            
            
            *** REACTION ANNOTATIONS ***
            
            
            *** SIMULATION OPTIONS ***
            
            RandomSeed: 164200191287356961681
            
            """;

    String expected_idafile_contents_2_lines = """
            t:r1:TOTAL_MT0:FREE_MT0:BOUND_MT0:TOTAL_MT0__Site0__state0:FREE_MT0__Site0__state0:BOUND_MT0__Site0__state0:TOTAL_MT0__Site1__state0:FREE_MT0__Site1__state0:BOUND_MT0__Site1__state0:TOTAL_MT0__Site1__state1:FREE_MT0__Site1__state1:BOUND_MT0__Site1__state1
            0.0 0 20 20 0 20 20 0 20 20 0 0 0 0
            """;

    void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }

    @Test
    public void testRandomSeed() throws IOException {
        // create a temporary directory
        // create an input file in the temporary directory with the contents of inputFileContents
        String sim_base_name = "sim";
        int runCounter = 0;
        Path tempDirectory = Files.createTempDirectory("test_simulation");
        Path modelFile = tempDirectory.resolve(sim_base_name+".langevinInput");
        Path logFile = tempDirectory.resolve(sim_base_name+".log");
        Path idaFile = tempDirectory.resolve(sim_base_name+".ida");
        Files.writeString(modelFile, inputFileContents);
        VCellMessaging vcellMessaging = new VCellMessagingLocal();
        try {
            // run simulation
            Global g = new Global(modelFile.toFile(), logFile.toFile());
            MySystem sys = new MySystem(g, runCounter, true, vcellMessaging);
            sys.runSystem();
            // check that the ida file was created and starts with the expected contents
            Assertions.assertTrue(Files.exists(idaFile));
            String idaFileContents = Files.readString(idaFile);
            Assertions.assertTrue(idaFileContents.startsWith(expected_idafile_contents_2_lines));
            Assertions.assertTrue(g.getStartSeed() != null, "startSeed must be not null");
            Assertions.assertEquals(g.getStartSeed().longValue(),-1820505376029002863L, "Unexpected startSeed value");
            System.out.println(idaFileContents.substring(0,500)+"\n...\n"+idaFileContents.substring(idaFileContents.length()-500));
        } finally {
            deleteDirectory(tempDirectory.toFile());
        }
    }

    @Test
    public void testLambda() {

        double lambdaIrreversible = OnRateSolver.getrootIrreversible(5, 10, 0.08*1000000.0, 5*1660000.0);
        System.out.println(lambdaIrreversible);

        OnRateSolver.checkInequalities(4,6,20,5,Math.pow(10,-9));
        OnRateSolver.checkInequalities(4,6,20,5,Math.pow(10,-9));
        OnRateSolver.checkInequalities(2,2.2,2,5,2*Math.pow(10,-9));

        for(int i=0;i<10;i++){
            System.out.println(OnRateSolver.g(0.5,0.6+i,5,1,0.005));
        }

        // double lambda = OnRateSolver.getrootReversible(0.2, 1.2, 1.2, 20.0*1000000.0, 1000*1660000.0);
        double lambdaReversible = OnRateSolver.getrootReversible(0.2, 1.2, 1.2, 20.0*1000000.0, 1000*1660000.0);
        System.out.println(lambdaReversible);

    }

    @Test
    public void testLambda2() {

        double[] kon = {0.05, 0.1, 0.5, 1.0, 5.0, 10.0 , 40.0};    // uM^-1 * s^-1
//        double[] kon = { 5.0, 10.0 , 15.0, 20.0, 25.0, 30.0, 35.0, 40.0};    // uM^-1 * s^-1
        double koff = 23.0;
        double[] dt = { 1.0E-9, 5.0E-9, 1.0E-8, 5.0E-8 };

        String[] relativeLambdaErrorExpected = { "0.01", "0.02", "0.12", "0.24", "1.32", "2.99", "55.65"};
        String[] relativeLambdaErrorCalculated = new String[kon.length];

        double siteRadius1 = 1.0;       // nm
        double siteRadius2 = 1.0;
        double reactionRadius1 = 1.5;   // nm
        double reactionRadius2 = 1.5;
        double diffusionRate1 = 1.0;    // um^2/s
        double diffusionRate2 = 1.0;

        double p = siteRadius1 + siteRadius2;           // site radius
        double R = reactionRadius1 + reactionRadius2;   // reaction radius
        double D = 1000000.0 * (diffusionRate1 + diffusionRate2);   // diffusion rate

        System.out.println("");
        System.out.println("Kon (uM^-1*s^-1) : % relative error (old lambda vs. new lambda)");
        System.out.println("dt (sec) : % relative error (old probability rate vs. new probability rate)  ...");
        System.out.println("");

        for(int i=0; i<kon.length; i++) {

            double rescalekon = kon[i] * 1660000.0;
            double lambdaOld = OnRateSolver.getrootIrreversible(p, R, D, rescalekon);
            double lambdaNew = getLambdaNew(p, R, D, rescalekon);
            double relativeError1 = Math.abs((lambdaOld - lambdaNew) / lambdaOld * 100);
            String adjustedRelativeLambdaError = adjust(relativeError1);
            relativeLambdaErrorCalculated[i] = adjustedRelativeLambdaError;
            System.out.println(kon[i] + ": " + adjustedRelativeLambdaError + "%");

            // TODO: !!! compute off rates the old way and the new way (with koff intrinsic)
            double offProbOld = koff*dt[3];
            double kOffIntrinsic = getKOffIntrinsic(R, D, koff, rescalekon);
            double offProbNew = 1.0 - Math.pow(Math.E, -(kOffIntrinsic*dt[3]));
            double relativeError = Math.abs((offProbOld - offProbNew) / offProbOld * 100);
            System.out.println("  --- offProb relative error: " + relativeError + "%");


            // compute error over a range of dt, using the old vs new formula
//            for(int j=0; j<dt.length; j++) {
//                double onProbOld = lambdaOld * dt[j];
//                double onProbNew = 1.0 - Math.pow(Math.E, -(lambdaNew * dt[j]));
//                double relativeError2 = Math.abs((onProbOld - onProbNew) / onProbOld * 100);
//                System.out.print(dt[j] + ": " + adjust(relativeError2) + "%    ");
//            }
            System.out.println("");
//            System.out.println(" ------------------- ");
//            System.out.println("");
        }
        Assertions.assertTrue(Arrays.equals(relativeLambdaErrorExpected, relativeLambdaErrorCalculated));
    }

    private static double getLambdaNew(double p, double R, double D, double rescalekon) {
        double volReact = 4.0 * Math.PI * (Math.pow(R, 3) - Math.pow(p, 3)) / 3.0;
        double kD = 4 * Math.PI * R * D;
        double kOnIntrinsic = getKOnIntrinsic(R, D, rescalekon);
        double lambdaNew = kOnIntrinsic / volReact;
        return lambdaNew;
    }

    private static double getKOnIntrinsic(double R, double D, double rescalekon) {
        double kD = 4 * Math.PI * R * D;
        double kOnIntrinsic = (rescalekon * kD) / (kD - rescalekon);    // can't be negative
        if (kOnIntrinsic <= 0.0) {
            // it will fail for kon = 50.0
            throw new RuntimeException("Kon is too large");
        }
        return kOnIntrinsic;
    }

    private static double getKOffIntrinsic(double R, double D, double koff, double rescalekon) {
        double kOnIntrinsic = getKOnIntrinsic(R, D, rescalekon);
        double kOffIntrinsic = koff * kOnIntrinsic / rescalekon;
        return kOffIntrinsic;
    }

    private static String adjust(double number) {
        DecimalFormat standardFormat = new DecimalFormat("0.00");
        DecimalFormat exponentialFormat = new DecimalFormat("0.00E0");

        int exponent = (int) Math.floor(Math.log10(Math.abs(number)));
        String formattedNumber;
        if (exponent < -2 || exponent > 3) {
            formattedNumber = exponentialFormat.format(number);
        } else {
            formattedNumber = standardFormat.format(number);
        }

        return(formattedNumber);

    }

    }
