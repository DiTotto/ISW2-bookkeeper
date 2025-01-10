package utils;

import com.opencsv.CSVWriter;
import models.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;


public class WriteCSV {
    private WriteCSV() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Scrive i dati delle release in più CSV, uno per ogni step del walk forward.
     * Per ogni step, viene creato un file CSV per il training (con tutte le release fino a quella corrente)
     * e un file per il test (con la release successiva).
     * @param releases Lista delle release da usare per generare i CSV.
     * @param baseCsvFilePathForTraining Percorso base del file CSV (es. "walk_forward_step").
     * @param baseCsvFilePathForTesting Percorso base del file CSV (es. "walk_forward_step").
     */
    public static void writeReleasesForWalkForward(List<Release> releases, List<Ticket> tickets, String baseCsvFilePathForTraining, String baseCsvFilePathForTesting, String repoPath) {
        // Itera per ogni step del walk forward
        for (int i = 1; i < releases.size(); i++) {
            List<Release> releaseList = new ArrayList<>();
            List<Ticket> ticketList = new ArrayList<>();
            for (Release release : releases) {
                if (release.getIndex() <= i) {
                    releaseList.add(release);
                }
            }
            for (Ticket ticket : tickets) {
                if (ticket.getFixedVersion() < i) {
                    ticketList.add(ticket);
                }
            }
            Bugginess.markBuggyFilesUsingAffectedVersions(ticketList, releaseList, repoPath);


            // File di training: contiene tutte le release fino alla i-esima
            String trainingCsvFilePath = baseCsvFilePathForTraining + "training_step_" + i + ".csv";



            writeReleasesToCsv(releaseList, trainingCsvFilePath);
            // File di test: contiene la release successiva alla i-esima
            Bugginess.markBuggyFilesUsingAffectedVersions(tickets, releases.subList(i, i + 1), repoPath);
            String testingCsvFilePath = baseCsvFilePathForTesting + "testing_step_" + i + ".csv";
            writeReleasesToCsv(releases.subList(i, i + 1), testingCsvFilePath);
        }
    }

    /**
     * Scrive le release in un singolo file CSV.
     * @param releases Lista delle release da scrivere nel CSV.
     * @param csvFilePath Percorso del file CSV.
     */
    public static void writeReleasesToCsv(List<Release> releases, String csvFilePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath))) {
            // Intestazione del CSV
            String[] header = { "VERSION", "FILE_NAME", "LOC", "LOC_TOUCHED", "NUMBER_OF_REVISIONS",
                    "LOC_ADDED", "AVG_LOC_ADDED", "NUMBER_OF_AUTHORS", "MAX_LOC_ADDED",
                    "TOTAL_LOC_REMOVED", "MAX_LOC_REMOVED", "AVG_LOC_TOUCHED", "BUGGY" };
            writer.writeNext(header);

            // Itera sulle release e sui file per scrivere i dati
            for (Release release : releases) {
                for (FileJava file : release.getFiles()) {
                    String[] fileData = {
                            String.valueOf(release.getIndex()),               // VERSION
                            file.getName(),                                   // FILE_NAME
                            String.valueOf(file.getLoc()),                    // LOC
                            String.valueOf(file.getLocTouched()),             // LOC_TOUCHED
                            String.valueOf(file.getNr()),                     // NUMBER_OF_REVISIONS
                            String.valueOf(file.getLocAdded()),               // LOC_ADDED
                            String.valueOf(file.getAvgLocAdded()),            // AVG_LOC_ADDED
                            String.valueOf(file.getNauth()),                  // NUMBER_OF_AUTHORS
                            String.valueOf(file.getMaxLocAdded()),            // MAX_LOC_ADDED
                            String.valueOf(file.getTotalLocRemoved()),        // TOTAL_LOC_REMOVED
                            String.valueOf(file.getMaxLocRemoved()),          // MAX_LOC_REMOVED
                            String.valueOf(file.getAvgLocTouched()),          // AVG_LOC_TOUCHED
                            String.valueOf(file.isBuggy())                    // BUGGY
                    };

                    writer.writeNext(fileData);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeWekaCalculation(List<ClassifierMetrics> metrics) throws IOException {
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");
        String featureSelection;
        String costSensitive;

        try( CSVWriter writer = new CSVWriter(new FileWriter(metrics.get(0).getNameProject() + "/fileCSV/weka_metrics.csv"))) {
            // Intestazione del CSV
            String[] header = { "PROJ", "CLASSIFIER", "ITERATION", "FEATURE_SELECTION", "SAMPLING", "COST_SENSITIVE", "PRECISION", "RECALL", "AUC", "KAPPA", "NPOFB", "TP", "FP", "TN", "FN", "%_OF_TRAINING" };
            writer.writeNext(header);

            for (ClassifierMetrics metric : metrics){
                if(metric.isFeatureSelection()){
                    featureSelection = "BEST_FIRST";
                }else {
                    featureSelection = "NONE";
                }
                if(metric.isCostSensitive()){
                    costSensitive = "SENSITIVE_LEARNING";
                }else {
                    costSensitive = "NONE";
                }

                String[] metricData = {
                        metric.getNameProject(),
                        metric.getClassifier(),
                        String.valueOf(metric.getIteration()),
                        featureSelection,
                        metric.getSamplingType(),
                        costSensitive,
                        decimalFormat.format(metric.getPrecision()),
                        decimalFormat.format(metric.getRecall()),
                        decimalFormat.format(metric.getAuc()),
                        decimalFormat.format(metric.getKappa()),
                        decimalFormat.format(metric.getNpofb()),
                        String.valueOf(metric.getTp()),
                        String.valueOf(metric.getFp()),
                        String.valueOf(metric.getTn()),
                        String.valueOf(metric.getFn()),
                        decimalFormat.format(metric.getPercentageOfTraining())};

                writer.writeNext(metricData);

            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeOnAcumeCSV(List<AcumeModel> acumeList) {
        try (CSVWriter writer = new CSVWriter(new FileWriter("ACUME-master/csv/acume.csv"))) {
            // Intestazione del CSV
            String[] header = { "ID", "Size", "Probability", "Value" };
            writer.writeNext(header);

            // Itera sulle release e sui file per scrivere i dati
            for (AcumeModel acume : acumeList) {
                String[] acumeData = {
                        String.valueOf(acume.getId()),               // ID
                        String.valueOf(acume.getSize()),             // SIZE
                        String.valueOf(acume.getProbability()),      // PROBABILITY
                        acume.getValue()                              // VALUE
                };

                writer.writeNext(acumeData);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}