package acumecontroller;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import models.AcumeModel;
import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import static java.util.logging.Level.INFO;

import static java.util.logging.Level.SEVERE;
import static utils.WriteCSV.writeOnAcumeCSV;
import java.util.Properties;


public class AcumeController {

    private static final Logger logger = Logger.getLogger(AcumeController.class.getName());

    private AcumeController() {
        throw new IllegalStateException("AcumeController class");
    }


    private static Properties loadConfiguration() {
        Properties properties = new Properties();
        try (InputStream input = AcumeController.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.severe("Configurazione non trovata in resources/config.properties");
                return properties;  // Ritorna null se il file non è stato trovato
            }
            properties.load(input);  // Carica le proprietà dal file
        } catch (IOException e) {
            logger.severe("Errore nel caricare config.properties");
            e.printStackTrace();
            return properties;  // Ritorna null in caso di errore
        }
        return properties;  // Ritorna le proprietà caricate
    }
    public static double retrieveNpofb(Instances data, AbstractClassifier classifier) {
        //Npofb stands for Number of Positive Out of False Positives


        List<AcumeModel> acumeModelList = new ArrayList<>();
        int lastAttributeIndex = data.classIndex();
        for (int i = 0; i < data.numInstances(); i++) {
            try {
                Instance instance = data.instance(i);

                double size = instance.value(0);
                double probability = getProbability(instance, classifier);
                if (probability < 0) {
                    logger.log(SEVERE, "Probabilità non valida per l''istanza {0}", i);
                    continue; // Salta l'istanza in caso di errore
                }
                String actual = instance.stringValue(lastAttributeIndex);
                AcumeModel acumeModel = new AcumeModel(i, size, probability, actual);
                acumeModelList.add(acumeModel);
            } catch (Exception e) {
                logger.log(SEVERE,"Error while retrieving Npofb");
                e.printStackTrace();
            }
        }
        writeOnAcumeCSV(acumeModelList);
        startAcumeScript();
        double npofb20 = -1;
        npofb20 = getNPofB20Value(Paths.get("ACUME-master/EAM_NEAM_output.csv").toAbsolutePath().toString());
        return npofb20;
    }

    public static double getNPofB20Value (String path) {
        try (CSVReader reader = new CSVReader(new FileReader(path))) {
            List<String[]> allData = reader.readAll();
            String[] header = allData.get(0);

            int columnIndex = -1;
            for (int i = 0; i < header.length; i++) {
                if ("Npofb20".equalsIgnoreCase(header[i].trim())) {
                    columnIndex = i;
                    break;
                }
            }
            if (columnIndex == -1) {
                logger.log(SEVERE, "Npofb20 column not found in the file");
                return 0;
            }
            String valueStr = allData.get(2)[columnIndex];
            return Double.parseDouble(valueStr);
        } catch (IOException | CsvException e) {
            logger.log(SEVERE, "Error while reading the file");
            e.printStackTrace();
        } catch (NumberFormatException e) {
            logger.log(SEVERE, "Error while parsing the value");
            e.printStackTrace();
        }
        return 0;
    }

    private static void startAcumeScript() {
        try {

            Properties properties = loadConfiguration();

            // Ottieni il percorso della directory da configurazione
            String acumeDirectoryPath = properties.getProperty("acume.directory");
            if (acumeDirectoryPath == null || acumeDirectoryPath.isEmpty()) {
                logger.severe("ACUME directory non definita in config.properties");
                return;
            }


            String acumeMainPath = Paths.get("ACUME-master/main.py").toAbsolutePath().toString();
            String[] command =  {"python3", acumeMainPath, "NPofB"};
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            //processBuilder.directory(new File("C:/Users/lucad/Documents/ISW2-bookkeeper/ISW2-bookkeeper/ACUME-master")); // Imposta la directory corretta
            processBuilder.directory(new File(acumeDirectoryPath));
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.log(INFO, line);
                }


                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    logger.log(SEVERE, errorLine);
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    logger.log(INFO, "Acume script executed successfully");
                } else {
                    logger.log(SEVERE, "Acume script failed");
                }
            }
        }
        catch (InterruptedException e) {
        // Ripristina lo stato di interruzione
        Thread.currentThread().interrupt();
        logger.log(SEVERE, "Script execution was interrupted");
        }
        catch (Exception e) {
                logger.log(SEVERE, "Error while executing the script");
            e.printStackTrace();
        }
    }

    private static double getProbability(Instance data, AbstractClassifier classifier) throws Exception {
        try{
            int buggyIndex = data.classAttribute().indexOfValue("true");

            if (buggyIndex == -1) {
                logger.log(SEVERE, "Valore 'YES' non trovato tra le classi disponibili");
                return -1; // Indica un errore
            }
            double[] predicted = classifier.distributionForInstance(data);
            return predicted[buggyIndex];
        } catch (Exception e) {
            logger.log(SEVERE, "Error while getting probability");
            e.printStackTrace();
            return 0.0;
        }

    }
}


