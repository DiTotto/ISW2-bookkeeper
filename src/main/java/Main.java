import gitController.GitController;
import jiraController.JiraRelease;
import jiraController.JiraTicket;
import models.*;
import org.eclipse.jgit.revwalk.DepthWalk;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static gitController.GitController.calculateMetric;
import static jiraController.JiraTicket.*;
import static utils.Proportion.*;


public class Main {

    public static void main(String[] args) {
        try {
            System.out.println("Starting...");
            List<Release> releases;
            List<Ticket> tickets;
            List<RevCommit> commits;

            //get the releases of the project from 1 to max number of releases / 2
            releases = JiraRelease.getRelease("BOOKKEEPER");

            tickets = JiraTicket.getTickets("BOOKKEEPER", releases);
            calculateMetric(releases, "\\Users\\lucad\\Documents\\bookkeeper_fork");
            commits = GitController.retrieveCommits("\\Users\\lucad\\Documents\\bookkeeper_fork");

            commitsOfTheticket(commits, tickets);
            removeTicketWithoutCommit(tickets);
            fixTicketList(tickets);

            //PROPORTION
            Collections.reverse(tickets);
            getProportion(tickets);
            for(Ticket ticket: tickets){
                calculateAV(ticket);
            }

            //CALCOLO DELLA BUGGY
            //processTickets(tickets, "/Users/lucadimarco/Desktop/bookkeeper/bookkeeper", releases);
            //processReleasesAndMarkBuggyFiles(releases, tickets, "\\Users\\lucad\\Documents\\bookkeeper_fork");
            markBuggyFilesUsingAffectedVersions(tickets, releases, "\\Users\\lucad\\Documents\\bookkeeper_fork");

            for(Ticket ticket: tickets){
                System.out.println("Ticket: " + ticket);
            }

            //RIATTIVARE IL CALCOLO DELLE METRICHE

            for(Release release: releases){
                for(FileJava file: release.getFiles()){
                    if(file.isBuggy()){
                        System.out.println("File: " + file.getName() + " is buggy");
                    }
                }
            }

            /* ----- SCRITTURA ---- */

            FileWriter fileWriter = new FileWriter("releases.csv");

            // Scrivi l'intestazione del CSV
            fileWriter.append("VERSION, FILE_NAME, LOC, LOC_TOUCHED, NUMBER_OF_REVISIONS, LOC_ADDED, AVG_LOC_ADDED, NUMBER_OF_AUTHORS, MAX_LOC_ADDED, TOTAL_LOC_REMOVED, MAX_LOC_REMOVED, AVG_LOC_TOUCHED, BUGGY\n");

            //int i = 0;

            // Itera attraverso le release e scrivi i dati
            for (Release release : releases) {
                for (FileJava file : release.getFiles()) {
                    // Scrivi i dati nel formato CSV
                    fileWriter.append(release.getIndex() + ",");
                    //fileWriter.append(release.getReleaseDate().toString() + ",");
                    fileWriter.append(file.getName() + ",");
                    fileWriter.append(file.getLoc() + ",");
                    fileWriter.append(file.getLocTouched() + ",");
                    fileWriter.append(file.getNr() + ",");
                    fileWriter.append(file.getLocAdded() + ",");
                    fileWriter.append(file.getAvgLocAdded() + ",");
                    fileWriter.append(file.getNauth() + ",");
                    fileWriter.append(file.getMaxLocAdded() + ",");
                    fileWriter.append(file.getTotalLocRemoved() + ",");
                    fileWriter.append(file.getMaxLocRemoved() + ",");
                    fileWriter.append(file.getAvgLocTouched() + ",");
                    if(file.isBuggy()){
                        fileWriter.append("YES\n");
                    } else {
                        fileWriter.append("NO\n");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}