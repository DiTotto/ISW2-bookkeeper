package common;

import acumecontroller.AcumeController;
import exceptions.GitOperationException;
import gitcontroller.GitController;
import jiracontroller.JiraRelease;
import jiracontroller.JiraTicket;
import models.*;
import org.eclipse.jgit.revwalk.RevCommit;
import utils.WriteCSV;
import wekacontroller.WekaController;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static gitcontroller.GitController.calculateMetric;
import static jiracontroller.JiraTicket.*;
import static utils.Proportion.*;
import static wekacontroller.WekaController.calculateWeka;



public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

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
    public static void main(String[] args) {
        try {

            logger.log(java.util.logging.Level.INFO, "\u001B[32mStarting...\u001B[0m");
            Properties properties = loadConfiguration();
            String project = properties.getProperty("main.projectB", "defaultProject");
            String path = properties.getProperty("main.pathB", "defaultPath");


            List<Release> releases;
            List<Ticket> tickets;
            List<RevCommit> commits;

            //get the releases of the project from 1 to max number of releases / 2
            releases = JiraRelease.getRelease(project);

            // retrieve the tickets of the project based on the considered releases
            tickets = JiraTicket.getTickets(project, releases);

            //calculate the metrics of the files of the project in the considered releases
            calculateMetric(releases, path);

            // retrieve the commits of the project
            commits = GitController.retrieveCommits(path);

            // associate the commits to the tickets. If the commit contains the ticket id, the commit is associated
            // to that specified ticket
            commitsOfTheticket(commits, tickets);

            // remove the tickets that do not have any commit associated with them
            removeTicketWithoutCommit(tickets);

            // fix the ticket list. If the ticket doesn't have the fixed version or the opening version or the
            // opening version is greater than the fixed version, the ticket is removed.
            // Also, if the ticket has the same opening and injected version, the ticket is removed.
            // Also, if the ticket has the injected version greater than the opening version, the injected version is
            // set to null
            fixTicketList(tickets);

            //PROPORTION
            Collections.reverse(tickets);

            // calculate the proportion of the tickets. It is done with cold start approach in the first attempt, and
            // then when the number of considered ticket is reached to can fill the window size, the proportion is
            // calculated with the moving window approach
            getProportion(tickets, project);

            // for every ticket calculate which are the affected version
            for(Ticket ticket: tickets){
                calculateAV(ticket);
            }

            //CALCOLO DELLA BUGGY
            // la bugginess ora è controllato dalla classe Bugginess invocata da WriteCSV



            /* ----- SCRITTURA ---- */

            WriteCSV.writeReleasesForWalkForward(releases, tickets, project + "/fileCSV/training/", project + "/fileCSV/testing/", path);


            /* ----- WEKA ----- */

            WekaController.convertAllCsvInFolder(project + "/fileCSV/training");
            WekaController.convertAllCsvInFolder(project + "/fileCSV/testing");

            calculateWeka(project, releases.size());

            logger.log(java.util.logging.Level.INFO, "\u001B[32mDone!\u001B[0m");

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}