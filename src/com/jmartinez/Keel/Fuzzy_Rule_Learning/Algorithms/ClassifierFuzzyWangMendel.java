package com.jmartinez.Keel.Fuzzy_Rule_Learning.Algorithms;


import com.jmartinez.Keel.Shared.Parsing.*;
import com.jmartinez.Keel.Fuzzy_Rule_Learning.Shared.*;
import java.io.*;
import core.*;

/**
 * <p>
 * ClassifierFuzzyWangMendel is intended to generate a Fuzzy Rule Based System
 * (FRBS) classifier using the Wang and Mendel approach.
 * Hence, the algorithm first partitions the input and the output spaces and thus
 * generates a complete Rule Base (RB).
 * Then, for each example in the training dataset the most compatible rule
 * antecedent from the RB is found and assigned with the corresponding output
 * class. Finally, this rule is chosen as a one of the FRBS rules.
 * </p>
 *
 * <p>
 * @author Written by Luciano Sanchez (University of Oviedo) 21/07/2008
 * @author Modified by J.R. Villar (University of Oviedo) 19/12/2008
 * @version 1.0
 * @since JDK1.4
 * </p>
 */
public class ClassifierFuzzyWangMendel {


    //The best suite rules
    private static long [] reglas;
    //The number of best suite rules
    private static int numReglas;

    /**
     * <p>
     * This private static method extract the dataset and the method's parameters
     * from the KEEL environment, learns the FRBS classifier using the Wang and Mendel
     * algorithm and print out the results with the validation dataset.
     * </p>
     * @param tty  unused boolean parameter, kept for compatibility
     * @param pc   ProcessConfig object to obtain the train and test datasets
     *             and the method's parameters.
     */
    public static void wangMendelFuzzyClassifier(boolean tty, ProcessConfig pc) {

        try {

            String linea=new String();

            int default_neparticion=0;
            int ncruces=0;

            ProcessDataset pd=new ProcessDataset();

            linea=(String)pc.parInputData.get(ProcessConfig.IndexTrain);

            if (pc.parNewFormat) pd.processClassifierDataset(linea,true);
            else pd.oldClusteringProcess(linea);

            int ndatos=pd.getNdata();           // Number of examples
            int nvariables=pd.getNvariables();   // Number of variables
            int nentradas=pd.getNinputs();     // Number of inputs
            pd.showDatasetStatistics();

            System.out.println("Number of input data="+ndatos);
            System.out.println("Number of output data="+nentradas);

            double[][] X = pd.getX();             // Input data
            int[] C = pd.getC();                  // Output data
            int nclases = pd.getNclasses();        // Number of classes

            double[] emaximo = pd.getImaximum();   // Maximum and Minimum for input data
            double[] eminimo = pd.getIminimum();
            int[] neparticion=new int[nentradas];

            pd.showDatasetStatistics();


            // Partitions definition
            FuzzyPartition[] particione=new FuzzyPartition[nentradas];

            for (int i=0;i<nentradas;i++) {
                System.out.print("Input Variable "+i+": ");
                neparticion[i]=pc.parPartitionLabelNum;
                particione[i]=new FuzzyPartition(eminimo[i],emaximo[i],neparticion[i]);
                System.out.println(particione[i].aString());
            }
            System.out.print("Output Variable:");
            FuzzyPartition particions=new FuzzyPartition(nclases);
            System.out.println(particions.aString());

            // Train results
            int [] Ct=new int[C.length];

            // Rule base
            RuleBase sistema=
                    new RuleBase(particione,particions,
                            RuleBase.product,
                            RuleBase.sum);


            // Wang-Mendel Algorithm
            FuzzyRule [] reglas2 = new FuzzyRule[X.length];
            reglas = new long[X.length];
            numReglas = 0;
            for (int i=0;i<X.length;i++) {

                // For each example, More compatible antecedent is searched
                double compatibilidad=0; long winr=0;
                winr = sistema.codifyAntecents(X[i]);
                double p = sistema.evaluateMembership(winr,X[i]);

                // If rule was not found earlier, it's stored.
                int numeroRegla = ruleSearching(winr);
                if (numeroRegla == -1){
                    reglas[numReglas] = winr;
                    reglas2[numReglas] = new FuzzyRule(C[i],p);
                    numReglas++;
                }
                else if (p > reglas2[numeroRegla].weight) { //If it is better than the previous rule found
                    reglas2[numeroRegla].weight = p;
                    reglas2[numeroRegla].consequent = C[i];
                }

            }
            //  Weights are tranformed to binary code
            for (int i = 0; i < numReglas; i++)
                //sistema.getComponente(reglas[i]).peso = 1;
                reglas2[i].weight = 1;

            long [] nuevasReglas = new long[numReglas];
            FuzzyRule [] nuevasReglas2 = new FuzzyRule[numReglas];

            // Result is printed
            for (int r = 0; r < numReglas; r++){
                System.out.println(
                        "IF "+ sistema.variableNames(reglas[r]) + " THEN " +
                                //"S" + sistema.getComponente(reglas[r]).consecuente
                                "S" + reglas2[r].consequent
                );
                nuevasReglas[r] = reglas[r];
                nuevasReglas2[r] = new FuzzyRule(reglas2[r].consequent,reglas2[r].weight);
            }
            sistema.addRules(nuevasReglas,nuevasReglas2);

            // Test error
            double error_clasificacion=0;
            for (int i=0;i<ndatos;i++) {

                double[] respuesta=sistema.myOutput(X[i]);
                int ganadora=0;
                for (int j=1;j<respuesta.length;j++)
                    if (respuesta[j]>respuesta[ganadora]) { ganadora=j; }

                if (ganadora!=C[i]) error_clasificacion++;
                Ct[i]=ganadora;

            }
            error_clasificacion/=ndatos;
            System.out.println("Train error: "+ error_clasificacion);
            pc.trainingResults(C,Ct);

            ProcessDataset pdt = new ProcessDataset();
            int nprueba,npentradas,npvariables;
            linea=(String)pc.parInputData.get(ProcessConfig.IndexTest);

            if (pc.parNewFormat) pdt.processClassifierDataset(linea,false);
            else pdt.oldClusteringProcess(linea);

            nprueba = pdt.getNdata();
            npvariables = pdt.getNvariables();
            npentradas = pdt.getNinputs();
            pdt.showDatasetStatistics();

            if (npentradas!=nentradas) throw new IOException("Test file error");

            double[][] Xp=pdt.getX(); int [] Cp=pdt.getC(); int [] Co=new int[Cp.length];

            // Test error
            error_clasificacion=0;
            for (int i=0;i<nprueba;i++) {

                double[] respuesta=sistema.myOutput(Xp[i]);
                int ganadora=0;
                for (int j=1;j<respuesta.length;j++)
                    if (respuesta[j]>respuesta[ganadora]) { ganadora=j; }

                if (ganadora!=Cp[i]) error_clasificacion++;
                Co[i]=ganadora;

            }
            error_clasificacion/=nprueba;
            System.out.println("Test set error: " + error_clasificacion);

            pc.results(Cp,Co);

            //We write in an output file the Data Base and Rule Base:
            String rutaSalidaBD = (String)pc.outputData.get(0); //fichero BD
            String rutaSalidaBR = (String)pc.outputData.get(1); //fichero BR
            Fichero fichSalida = new Fichero();
            String cad = new String("");
            cad += "DATA BASE:\n";
            for (int i=0;i<nentradas;i++) {
                cad += "\nInput Variable "+i+": ";
                cad += particione[i].aString();
            }
            cad += "\n\nOutput Variable:";
            cad += particions.aString();
            fichSalida.escribeFichero(rutaSalidaBD, cad);

            cad = "RULE BASE:\n";
            for (int r = 0; r < numReglas; r++){
                cad += "\nRule_"+(r+1)+": IF "+ sistema.variableNames(reglas[r]) + " THEN " + "S" + reglas2[r].consequent;
            }
            fichSalida.escribeFichero(rutaSalidaBR, cad);

        } catch(FileNotFoundException e) {
            System.err.println(e+" Train not found");
        } catch(IOException e) {
            System.err.println(e+" Read Error");
        }

    }

    /**
     * <p>
     * This private static method searchs for a certain rule in the set of best
     * suite rule data base. The value -1 is returned if that rule is not in
     * the rule data base.
     * </p>
     * @param winr  the rule to be searched for.
     * @return the position of the searched rule or -1 if it not found.
     */
    private static int ruleSearching(long winr){
        boolean salir = false;
        int i;
        for (i = 0; (i < numReglas)&&(!salir); i++){
            salir = (winr == reglas[i]);
        }
        if (salir){
            return (i-1);
        }
        return -1;
    }


}
