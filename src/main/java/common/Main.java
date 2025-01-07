package common;

import gitController.GitController;
import jiraController.JiraRelease;
import jiraController.JiraTicket;
import models.*;
import org.eclipse.jgit.revwalk.RevCommit;
import utils.WriteCSV;
import wekaController.WekaController;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import static gitController.GitController.calculateMetric;
import static jiraController.JiraTicket.*;
import static utils.Proportion.*;
import static utils.Bugginess.*;
import static wekaController.WekaController.calculateWeka;



public class Main {

    public static void main(String[] args) {
        try {
            System.out.println("Starting...");
            //String project = "BOOKKEEPER";
            String project = "SYNCOPE";
            //String path = "\\Users\\lucad\\Documents\\bookkeeper_fork";
            String path = "\\Users\\lucad\\Documents\\syncope";

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
            //processReleasesAndMarkBuggyFiles(releases, tickets, "\\Users\\lucad\\Documents\\bookkeeper_fork");
            //markBuggyFilesUsingAffectedVersions(tickets, releases, path);

            /*for(Ticket ticket: tickets){
                System.out.println("Ticket: " + ticket);
            }

            for(Release release: releases){
                for(FileJava file: release.getFiles()){
                    if(file.isBuggy()){
                        System.out.println("File: " + file.getName() + " is buggy");
                    }
                }
            }*/

            /* ----- SCRITTURA ---- */

            WriteCSV.writeReleasesForWalkForward(releases, tickets, project + "/fileCSV/training/", project + "/fileCSV/testing/", path);


            /*for(int i = 1; i < releases.size(); i++) {
                WekaController.convertCSVtoARFF();
            }*/

            WekaController.convertAllCsvInFolder(project + "/fileCSV/training");
            WekaController.convertAllCsvInFolder(project + "/fileCSV/testing");

            calculateWeka(project, releases.size());

            System.out.println("Done!");

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}