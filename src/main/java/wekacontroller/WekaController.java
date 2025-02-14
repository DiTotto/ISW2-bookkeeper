package wekacontroller;

import models.ClassifierMetrics;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.CostMatrix;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.AbstractClassifier;
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
import java.util.logging.Logger;

import static acumecontroller.AcumeController.retrieveNpofb;
import utils.WriteCSV;
public class WekaController {

    private WekaController() {
        throw new IllegalStateException("Utility class");
    }
    private static final Logger logger = Logger.getLogger(WekaController.class.getName());

    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_RESET = "\u001B[0m";

    private static final String ARFF_EXTENSION = ".arff";

    public static void convertCSVtoARFF(String csvFile, String arffFile) {

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
                String arffFile = csvFile.substring(0, csvFile.length() - 4) + ARFF_EXTENSION;
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
                trainingFilePath = Paths.get(path1, "training_step_" + walkIteration + ARFF_EXTENSION).toAbsolutePath().toString();
                testingFilePath = Paths.get(path2, "testing_step_" + walkIteration + ARFF_EXTENSION).toAbsolutePath().toString();

                //carico i dati da ARFF
                ConverterUtils.DataSource trainingSource = new ConverterUtils.DataSource(trainingFilePath);
                ConverterUtils.DataSource testingSource = new ConverterUtils.DataSource(testingFilePath);

                Instances trainingData = trainingSource.getDataSet();
                Instances testingData = testingSource.getDataSet();

                trainingData.setClassIndex(trainingData.numAttributes() - 1);
                testingData.setClassIndex(testingData.numAttributes() - 1);

                /* BISOGNA FARE PRIMA FEATURE SELECTION E POI SAMPLING! */

                logger.log(java.util.logging.Level.INFO,  ANSI_WHITE + "Iterazione: {0}" + ANSI_RESET, walkIteration);


                // ---- RUN SENZA SELECTION - SEMPLICE ----
                runSimpleClassifier(nameProj, walkIteration, trainingData, testingData, metricOfClassifierList, classifiers);

                // ---- RUN CON FUTURE SELECTION (BEST FIRST) SENZA SAMPLING ----

                runWithFeatureSelection(nameProj, walkIteration, trainingData, testingData, metricOfClassifierList, classifiers);

                // ---- RUN CON FUTURE SELECTION E UNDER-SAMPLING ----
                runWithFeatureSelectionAndUnderSampling(nameProj, walkIteration, trainingData, testingData, metricOfClassifierList, classifiers);

                // ---- RUN CON FUTURE SELECTION E OVER-SAMPLING ----
                runWithFeatureSelectionAndOverSampling(nameProj, walkIteration, trainingData, testingData, metricOfClassifierList, classifiers);

                // ---- RUN CON FUTURE SELECTION E COST-SENSITIVE ----
                runWithFeatureSelectionAndCostSensitive(nameProj, walkIteration, trainingData, testingData, metricOfClassifierList, classifiers);


            }
            WriteCSV.writeWekaCalculation(metricOfClassifierList);

            for(int j = 0; j < metricOfClassifierList.size(); j++) {
                if (logger.isLoggable(java.util.logging.Level.INFO)) {
                    String message = String.format("%s%s%s", ANSI_WHITE, metricOfClassifierList.get(j), ANSI_RESET);
                    logger.log(java.util.logging.Level.INFO, message);
                }
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

            setValueinTheClassifier(classifierEval, eval, trainingData.numInstances(), testingData.numInstances(), testingData, (AbstractClassifier) classifier);
            metricOfClassifierList.add(classifierEval);
        }
    }


    private static void runWithFeatureSelection(String nameProj, int walkIteration, Instances trainingData, Instances testingData,
                                                List<ClassifierMetrics> metricOfClassifierList, Classifier[] classifiers) throws Exception {

        // ---- FEATURE SELECTION ----
        AttributeSelection attributeSelection = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        BestFirst search = new BestFirst();

        //BEST FIRST BI-DIREZIONALE (se non specifico è unidirezionale)
        search.setOptions(Utils.splitOptions("-D 2"));

        attributeSelection.setEvaluator(eval);
        attributeSelection.setSearch(search);
        attributeSelection.setInputFormat(trainingData);

        //applico il filtro
        Instances filteredTrainingData = Filter.useFilter(trainingData, attributeSelection);
        Instances filteredTestingData = Filter.useFilter(testingData, attributeSelection);

        filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);
        filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);


        for (Classifier classifier : classifiers) {
            classifier.buildClassifier(filteredTrainingData);
            Evaluation evalModel = new Evaluation(testingData);
            evalModel.evaluateModel(classifier, filteredTestingData);

            ClassifierMetrics classifierEval = new ClassifierMetrics(nameProj, walkIteration,
                    classifier.getClass().getSimpleName(), true, false, false);
            setValueinTheClassifier(classifierEval, evalModel, filteredTrainingData.numInstances(), filteredTestingData.numInstances(), filteredTestingData, (AbstractClassifier) classifier);
            metricOfClassifierList.add(classifierEval);
        }
    }

    private static void runWithFeatureSelectionAndUnderSampling(String nameProj, int walkIteration, Instances trainingData, Instances testingData,
                                                                List<ClassifierMetrics> metricOfClassifierList, Classifier[] classifiers) throws Exception {

        // ---- FEATURE SELECTION ----
        AttributeSelection attributeSelection = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        BestFirst search = new BestFirst();

        //BEST FIRST BI-DIREZIONALE (se non specifico è unidirezionale)
        search.setOptions(Utils.splitOptions("-D 2"));

        attributeSelection.setEvaluator(eval);
        attributeSelection.setSearch(search);
        attributeSelection.setInputFormat(trainingData);

        // applico il filtro al training set
        Instances filteredTrainingData = Filter.useFilter(trainingData, attributeSelection);
        filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);

        Instances filteredTestingData = Filter.useFilter(testingData, attributeSelection);
        filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);

        // ---- UNDER-SAMPLING ----
        SpreadSubsample underSampler = new SpreadSubsample();
        underSampler.setInputFormat(filteredTrainingData);
        // -M 1.0 = undersampling 1:1 --> il filtro rimuoverà abbastanza istanze della classe maggioritaria per mantenere un rapporto di 1:1
        // Bilanciamento classi di maggioranza
        underSampler.setOptions(Utils.splitOptions("-M 1.0"));


        // ---- RUN CON I DATI UNDER-SAMPLED ----
        for (Classifier classifier : classifiers) {
            FilteredClassifier fc = new FilteredClassifier();
            fc.setFilter(underSampler);
            fc.setClassifier(classifier);
            fc.buildClassifier(filteredTrainingData);
            Evaluation evalModel = new Evaluation(testingData);
            evalModel.evaluateModel(fc, filteredTestingData);



            ClassifierMetrics classifierEval = new ClassifierMetrics(nameProj, walkIteration,
                    classifier.getClass().getSimpleName(), true, true, false);
            setValueinTheClassifier(classifierEval, evalModel, filteredTrainingData.numInstances(), filteredTestingData.numInstances(), filteredTestingData, (AbstractClassifier) classifier);
            classifierEval.setSamplingType("UNDER_SAMPLING");
            metricOfClassifierList.add(classifierEval);
        }


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

        return (double) majorityCount / data.numInstances() * 100.0;
    }


    private static void runWithFeatureSelectionAndOverSampling(String nameProj, int walkIteration, Instances trainingData, Instances testingData,
                                                               List<ClassifierMetrics> metricOfClassifierList, Classifier[] classifiers) throws Exception {

        // ---- FEATURE SELECTION ----
        AttributeSelection attributeSelection = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        BestFirst search = new BestFirst();

        //BEST FIRST BI-DIREZIONALE (se non specifico è unidirezionale)
        search.setOptions(Utils.splitOptions("-D 2"));


        attributeSelection.setEvaluator(eval);
        attributeSelection.setSearch(search);
        attributeSelection.setInputFormat(trainingData);

        // applico il filtro al training set
        Instances filteredTrainingData = Filter.useFilter(trainingData, attributeSelection);
        filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);

        Instances filteredTestingData = Filter.useFilter(testingData, attributeSelection);
        filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);

        // ---- OVER-SAMPLING ----
        Resample overSampler = new Resample();
        overSampler.setInputFormat(filteredTrainingData);
        double majorityClassPercentage = calculateMajorityClassPercentage(filteredTrainingData);
        double sampleSize = majorityClassPercentage * 2;
        overSampler.setOptions(Utils.splitOptions("-B 1.0 -Z " + sampleSize));
        // -B 1.0 = oversampling 1:1 --> il filtro aggiungerà un numero sufficiente di istanze della classe minoritaria per mantenere un rapporto 1:1




        // ---- RUN CON I DATI OVER-SAMPLED ----
        for (Classifier classifier : classifiers) {
            FilteredClassifier fc = new FilteredClassifier();
            fc.setFilter(overSampler);
            fc.setClassifier(classifier);
            fc.buildClassifier(filteredTrainingData);

            Evaluation evalModel = new Evaluation(testingData);
            evalModel.evaluateModel(fc, filteredTestingData);

            ClassifierMetrics classifierEval = new ClassifierMetrics(nameProj, walkIteration,
                    classifier.getClass().getSimpleName(), true, true, false);
            setValueinTheClassifier(classifierEval, evalModel, filteredTrainingData.numInstances(), filteredTestingData.numInstances(), filteredTestingData, (AbstractClassifier) classifier);
            classifierEval.setSamplingType("OVER_SAMPLING");
            metricOfClassifierList.add(classifierEval);
        }

        logger.log(java.util.logging.Level.INFO, "Iterazione: {0}, numero di istanze prima del campionamento: {1}", new Object[]{walkIteration, filteredTrainingData.numInstances()});
        logger.log(java.util.logging.Level.INFO, "Iterazione: {0}, numero di istanze dopo over-sampling: {1}", new Object[]{walkIteration, filteredTestingData.numInstances()});


    }


    private static void runWithFeatureSelectionAndCostSensitive(String nameProj, int walkIteration, Instances trainingData, Instances testingData,
                                                               List<ClassifierMetrics> metricOfClassifierList, Classifier[] classifiers) throws Exception {
        // -- FEATURE SELECTION --
        AttributeSelection attributeSelection = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        BestFirst search = new BestFirst();

        //BEST FIRST BI-DIREZIONALE (se non specifico è unidirezionale)
        search.setOptions(Utils.splitOptions("-D 2"));

        attributeSelection.setEvaluator(eval);
        attributeSelection.setSearch(search);
        attributeSelection.setInputFormat(trainingData);

        // applico il filtro al training set
        Instances filteredTrainingData = Filter.useFilter(trainingData, attributeSelection);
        filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);

        // applico il filtro al testing set
        Instances filteredTestingData = Filter.useFilter(testingData, attributeSelection);
        filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);

        // -- COST-SENSITIVE --
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setElement(0,0,0.0); //Costo del true negative
        costMatrix.setElement(0,1,10.0); //Costo del false positive
        costMatrix.setElement(1,0,1.0); //Costo del false negative
        costMatrix.setElement(1,1,0.0); //Costo del true positive

        for (Classifier classifier : classifiers) {
            CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
            costSensitiveClassifier.setCostMatrix(costMatrix);
            costSensitiveClassifier.setClassifier(classifier);
            costSensitiveClassifier.buildClassifier(filteredTrainingData);

            Evaluation evalModel = new Evaluation(testingData);
            evalModel.evaluateModel(costSensitiveClassifier, filteredTestingData);

            ClassifierMetrics classifierEval = new ClassifierMetrics(nameProj, walkIteration,
                    classifier.getClass().getSimpleName(), true, false, true);

            setValueinTheClassifier(classifierEval, evalModel, filteredTrainingData.numInstances(), filteredTestingData.numInstances(), filteredTestingData, (AbstractClassifier) classifier);
            metricOfClassifierList.add(classifierEval);
        }

    }

    private static void setValueinTheClassifier(ClassifierMetrics classifier, Evaluation eval, int trainingSet, int testingSet, Instances testingData, AbstractClassifier absClassifier) {

        classifier.setPrecision(eval.precision(0));
        classifier.setRecall(eval.recall(0));
        classifier.setAuc(eval.areaUnderROC(0));
        classifier.setKappa(eval.kappa());
        classifier.setTp(eval.numTruePositives(0));
        classifier.setFp(eval.numFalsePositives(0));
        classifier.setTn(eval.numTrueNegatives(0));
        classifier.setFn(eval.numFalseNegatives(0));
        classifier.setPercentOfTheTraining(100.0 * trainingSet / (trainingSet + testingSet));
        classifier.setNpofb(retrieveNpofb(testingData, absClassifier));


    }





}