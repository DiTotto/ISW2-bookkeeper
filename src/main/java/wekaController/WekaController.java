package wekaController;

import models.ClassifierMetrics;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

import weka.core.converters.ConverterUtils;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class WekaController {

    private WekaController() {
        throw new IllegalStateException("Utility class");
    }

    public static void convertCSVtoARFF(String csvFile, String arffFile) {
        /*try {
            String pythonScriptPath = Paths.get("convert.py").toAbsolutePath().toString();

            ProcessBuilder pb = new ProcessBuilder("python", pythonScriptPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Exit Code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }*/

        try {
            CSVLoader loader = new CSVLoader();
            loader.setSource(new File(csvFile));
            Instances data = loader.getDataSet();

            data.deleteAttributeAt(1);
            data.deleteAttributeAt(0);

            // save ARFF
            ArffSaver saver = new ArffSaver();
            saver.setInstances(data);
            saver.setFile(new File(arffFile));
            //saver.setDestination(new File(arffFile));
            saver.writeBatch();

            Path path = Paths.get(arffFile);
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                if (line.contains("@attribute BUGGY")) {
                    lines.set(lines.indexOf(line), "@attribute BUGGY {true, false}");
                }
            }
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    public static void convertAllCsvInFolder(String folderPath) {
        File folder = new File(folderPath);
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".csv")) {
                String csvFile = file.getAbsolutePath();
                String arffFile = csvFile.substring(0, csvFile.length() - 4) + ".arff";
                convertCSVtoARFF(csvFile, arffFile);
            }
        }
    }

    public static void calculateWeka(String nameProj, int numReleases) {

        List<ClassifierMetrics> metricOfClassifierList = new ArrayList<>();

        //lista di classificatori da utilizzare
        Classifier[] classifiers = new Classifier[]{
                new RandomForest(),
                new NaiveBayes(),
                new IBk()
        };

        try {
            String path1 = nameProj + "/fileCSV/training/";
            String path2 = nameProj + "/fileCSV/testing/";
            String trainingFilePath;
            String testingFilePath;

            for (int walkIteration = 1; walkIteration <= numReleases -1 ; walkIteration++) {
                trainingFilePath = Paths.get(path1, "training_step_" + walkIteration + ".arff").toAbsolutePath().toString();
                testingFilePath = Paths.get(path2, "testing_step_" + walkIteration + ".arff").toAbsolutePath().toString();

                //carico i dati da ARFF
                ConverterUtils.DataSource trainingSource = new ConverterUtils.DataSource(trainingFilePath);
                ConverterUtils.DataSource testingSource = new ConverterUtils.DataSource(testingFilePath);

                Instances trainingData = trainingSource.getDataSet();
                Instances testingData = testingSource.getDataSet();

                trainingData.setClassIndex(trainingData.numAttributes() - 1);
                testingData.setClassIndex(testingData.numAttributes() - 1);

                /* BISOGNA FARE PRIMA FEATURE SELECTION E POI SAMPLING! */

                System.out.println("Iterazione: " + walkIteration);

                System.out.println("");
                // ---- RUN SENZA SELECTION - SEMPLICE ----
                runSimpleClassifier(nameProj, walkIteration, trainingData, testingData, metricOfClassifierList, classifiers);

                // ---- RUN CON FUTURE SELECTION (BEST FIRST) SENZA SAMPLING ----
                runWithFeatureSelection(nameProj, walkIteration, trainingData, testingData, metricOfClassifierList, classifiers, false, false);

                // ---- RUN CON FUTURE SELECTION E UNDER-SAMPLING ----
                runWithFeatureSelectionAndUnderSampling(nameProj, walkIteration, trainingData, testingData, metricOfClassifierList, classifiers);

                // ---- RUN CON FUTURE SELECTION E OVER-SAMPLING ----
                runWithFeatureSelectionAndOverSampling(nameProj, walkIteration, trainingData, testingData, metricOfClassifierList, classifiers);




            }
            for(int j = 0; j < metricOfClassifierList.size(); j++) {
                System.out.println(metricOfClassifierList.get(j).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void runSimpleClassifier(String nameProj, int walkIteration, Instances trainingData, Instances testingData,
                                            List<ClassifierMetrics> metricOfClassifierList, Classifier[] classifiers) throws Exception {
        // ---- RUN SENZA SELECTION - SEMPLICE ----

        for (Classifier classifier : classifiers) {
            Evaluation eval = new Evaluation(testingData);
            classifier.buildClassifier(trainingData);

            eval.evaluateModel(classifier, testingData);


            ClassifierMetrics classifierEval = new ClassifierMetrics(nameProj, walkIteration,
                    classifier.getClass().getSimpleName(), false, false, false);

            setValueinTheClassifier(classifierEval, eval, trainingData.numInstances(), testingData.numInstances());
            metricOfClassifierList.add(classifierEval);
        }
    }


    private static void runWithFeatureSelection(String nameProj, int walkIteration, Instances trainingData, Instances testingData,
                                                List<ClassifierMetrics> metricOfClassifierList, Classifier[] classifiers, boolean isUnderSampling, boolean isOverSampling) throws Exception {

        // ---- FUTURE SELECTION ----
        AttributeSelection attributeSelection = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        BestFirst search = new BestFirst();

        attributeSelection.setEvaluator(eval);
        attributeSelection.setSearch(search);
        attributeSelection.setInputFormat(trainingData);

        //applico il filtro
        Instances filteredTrainingData = Filter.useFilter(trainingData, attributeSelection);
        Instances filteredTestingData = Filter.useFilter(testingData, attributeSelection);

        filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);
        //filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);


        for (Classifier classifier : classifiers) {
            classifier.buildClassifier(filteredTrainingData);
            Evaluation evalModel = new Evaluation(filteredTestingData);
            evalModel.evaluateModel(classifier, filteredTestingData);

            ClassifierMetrics classifierEval = new ClassifierMetrics(nameProj, walkIteration,
                    classifier.getClass().getSimpleName(), true, (isUnderSampling || isOverSampling), false);
            setValueinTheClassifier(classifierEval, evalModel, filteredTrainingData.numInstances(), filteredTestingData.numInstances());
            metricOfClassifierList.add(classifierEval);
        }
    }

    private static void runWithFeatureSelectionAndUnderSampling(String nameProj, int walkIteration, Instances trainingData, Instances testingData,
                                                                List<ClassifierMetrics> metricOfClassifierList, Classifier[] classifiers) throws Exception {

        // ---- FUTURE SELECTION ----
        AttributeSelection attributeSelection = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        BestFirst search = new BestFirst();

        attributeSelection.setEvaluator(eval);
        attributeSelection.setSearch(search);
        attributeSelection.setInputFormat(trainingData);

        // applico il filtro al training set
        Instances filteredTrainingData = Filter.useFilter(trainingData, attributeSelection);
        filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);

        // ---- UNDER-SAMPLING ----
        SpreadSubsample underSampler = new SpreadSubsample();
        underSampler.setInputFormat(trainingData);
        // -M 1.0 = undersampling 1:1 --> il filtro rimuoverà abbastanza istanze della classe maggioritaria per mantenere un rapporto di 1:1
        // Bilanciamento classi di maggioranza
        underSampler.setOptions(Utils.splitOptions("-M 1.0"));
        Instances underSampledTrainingData = Filter.useFilter(trainingData, underSampler);

        // ---- RUN CON I DATI UNDER-SAMPLED ----
        for (Classifier classifier : classifiers) {
            classifier.buildClassifier(underSampledTrainingData);
            Evaluation evalModel = new Evaluation(testingData);
            evalModel.evaluateModel(classifier, testingData);

            ClassifierMetrics classifierEval = new ClassifierMetrics(nameProj, walkIteration,
                    classifier.getClass().getSimpleName(), true, true, false);
            setValueinTheClassifier(classifierEval, evalModel, underSampledTrainingData.numInstances(), testingData.numInstances());
            metricOfClassifierList.add(classifierEval);
        }

        //System.out.println("Numero di istanze prima del campionamento: " + trainingData.numInstances());
        //System.out.println("Numero di istanze dopo under-sampling: " + underSampledTrainingData.numInstances());
    }

    private static double calculateMajorityClassPercentage(Instances data) {

        int[] classCounts = new int[data.numClasses()];

        for (int i = 0; i < data.numInstances(); i++) {
            int classIndex = (int) data.instance(i).classValue();
            classCounts[classIndex]++;
        }

        int majorityCount = 0;
        for (int count : classCounts) {
            if (count > majorityCount) {
                majorityCount = count;
            }
        }
        double majorityClassPercentage = (double) majorityCount / data.numInstances() * 100.0;

        return majorityClassPercentage;
    }


    private static void runWithFeatureSelectionAndOverSampling(String nameProj, int walkIteration, Instances trainingData, Instances testingData,
                                                               List<ClassifierMetrics> metricOfClassifierList, Classifier[] classifiers) throws Exception {
        // ---- OVER-SAMPLING ----
        Resample overSampler = new Resample();
        overSampler.setInputFormat(trainingData);
        double majorityClassPercentage = calculateMajorityClassPercentage(trainingData);
        //double majorityClassPercentage = 65.0;
        overSampler.setOptions(Utils.splitOptions("-B 1.0 -Z " + (majorityClassPercentage*2)));

        Instances overSampledTrainingData = Filter.useFilter(trainingData, overSampler);

        System.out.println("Iterazione: " + walkIteration + ", numero di istanze prima del campionamento: " + trainingData.numInstances());
        System.out.println("Iterazione: " + walkIteration + ", numero di istanze dopo over-sampling: " + overSampledTrainingData.numInstances());

        // ---- RUN CON FUTURE SELECTION ----
        runWithFeatureSelection(nameProj, walkIteration, overSampledTrainingData, testingData, metricOfClassifierList, classifiers, false, true);
    }


    private static void setValueinTheClassifier(ClassifierMetrics classifier, Evaluation eval, int trainingSet, int testingSet) {

        classifier.setPrecision(eval.precision(0));
        classifier.setRecall(eval.recall(0));
        classifier.setAuc(eval.areaUnderROC(0));
        classifier.setKappa(eval.kappa());
        classifier.setTp(eval.numTruePositives(0));
        classifier.setFp(eval.numFalsePositives(0));
        classifier.setTn(eval.numTrueNegatives(0));
        classifier.setFn(eval.numFalseNegatives(0));
        classifier.setPercentOfTheTraining(100.0 * trainingSet / (trainingSet + testingSet));



    }





}