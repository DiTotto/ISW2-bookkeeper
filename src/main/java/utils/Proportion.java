package utils;


import jiraController.JiraTicket;
import models.FileJava;
import models.Release;
import models.Ticket;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Proportion {

    private static List<String> projForColdStart = new ArrayList<>();

    private static int movWinSize;
    private static double prop;
    private static final List<Ticket> ticketofProportion=new ArrayList<>();

    private Proportion() {
        throw new IllegalStateException("Utility class");
    }

    public static void getProportion(List<Ticket> tickets) throws IOException {

        //List<String> projectsToAdd = List.of("AVRO", "OPENJPA", "ZOOKEEPER", "STORM", "TAJO");
        List<String> projectsToAdd = List.of("AVRO");
        projForColdStart.addAll(projectsToAdd);

        int numTickets=tickets.size();
        System.out.println("Numero di ticket: " + numTickets);
        movWinSize=Math.max(1, numTickets / 10); //uso il 10% dei ticket, ma almeno 1 ticket
        prop=0;


        for (Ticket ticket: tickets) {
            if (ticket.getInjectedVersion() != null ) {
                //per rispettare la grandezza della finestra mobile
                //rimuovo il ticket più vecchio e inserisco
                if(ticketofProportion.size() > movWinSize){
                    ticketofProportion.remove(0);
                }
                ticketofProportion.add(ticket);
            } else {
                //sono all'inizio e uso cold start
                if (ticketofProportion.size() < movWinSize) {
                    if (prop==0) {
                        //prop = coldStart();
                        System.out.println("Cold Start");
                        prop = coldStart();
                    }
                } else {
                    System.out.println("moving window");
                    prop = movingWindow();

                }

                System.out.println("Proportion: " + prop);

                setInjectedVersion(ticket);
            }
        }
    }

    public static void setInjectedVersion(Ticket ticket) {

        if (ticket.getFixedVersion() == ticket.getOpeningVersion()) {
            ticket.setInjectedVersion((int) Math.floor(ticket.getFixedVersion()-prop));
        }else {
            ticket.setInjectedVersion((int) Math.floor((ticket.getFixedVersion() - (ticket.getFixedVersion() - ticket.getOpeningVersion()) * prop)));
        }
        if (ticket.getInjectedVersion()<=0) {
            ticket.setInjectedVersion(1);
        }
    }

    public static double movingWindow() {
        int k=0;
        double p = 0;
        for(Ticket t : ticketofProportion) {
            if(t.getFixedVersion() != t.getOpeningVersion()) {
                p += (double) (t.getFixedVersion() - t.getInjectedVersion()) / (t.getFixedVersion() - t.getOpeningVersion());
            }else{
                //evito la divisione per zero
                p+= (t.getFixedVersion() - t.getInjectedVersion());
            }
            k++;
        }

        if(k!=0) {
            p = p / k;
        }

        return p;
    }

    public static double coldStart() throws IOException {
        //utilizzo i ticket di progetti diversi per fare il cold start
        List<Ticket> tickets = new ArrayList<>();
        List<Double> prop_calc = new ArrayList<>();
        double p = 0;
        int counter = 0;

        //calcolo la proportion per ogni progetto e lo inserisco nella lista
        //utilizzo la media delle proporzioni per fare il cold start

        /*for(String proj : projForColdStart) {
            counter = 0;
            tickets.clear();
            tickets = JiraTicket.getTickets(proj);
            for(Ticket ticket: tickets){
                if(ticket.getInjectedVersion() != null && ticket.getOpeningVersion() != null && ticket.getFixedVersion() != null
                        && ticket.getOpeningVersion() != ticket.getFixedVersion()){
                    if((double) (ticket.getFixedVersion() - ticket.getInjectedVersion()) / (ticket.getFixedVersion() - ticket.getOpeningVersion()) > 1 ) {
                        p += (double) (ticket.getFixedVersion() - ticket.getInjectedVersion()) / (ticket.getFixedVersion() - ticket.getOpeningVersion());
                    }else{
                        p += 1;
                    }
                    counter++;
                }
            }
            if(counter != 0) {
                p = p / counter;
                prop_calc.add(prop);
            }

        }*/

        tickets = JiraTicket.getTickets("AVRO", null);
        for(Ticket ticket: tickets){
            if(ticket.getInjectedVersion() != null && ticket.getOpeningVersion() != null && ticket.getFixedVersion() != null
                    && ticket.getOpeningVersion() != ticket.getFixedVersion()){
                if((double) (ticket.getFixedVersion() - ticket.getInjectedVersion()) / (ticket.getFixedVersion() - ticket.getOpeningVersion()) > 1 ) {
                    p += (double) (ticket.getFixedVersion() - ticket.getInjectedVersion()) / (ticket.getFixedVersion() - ticket.getOpeningVersion());
                }else{
                    p += 1;
                }
                counter++;
            }
        }
        if(counter != 0) {
            p = p / counter;
            prop_calc.add(p);
        }

        return p;



        //restituisco la media delle proportion
        //se dispari restituisco il valore centrale
        //se pari restituisco la media dei due valori centrali
        /*prop_calc.sort(Comparator.naturalOrder());
        if(prop_calc.size() % 2 == 0) {
            return ((prop_calc.get(prop_calc.size() / 2) + prop_calc.get(prop_calc.size() / 2 - 1)) / 2);
        } else {
            return (prop_calc.get(prop_calc.size() / 2));
        }*/
    }


    /* ---------------- CALCOLO ISBUGGY ----------------------- */

    //ottengo i file modificati per ogni commit
    public static List<String> getModifiedJavaFiles(RevCommit commit, String repoPath) throws IOException {
        List<String> javaFiles = new ArrayList<>();
        try (Git git = Git.open(new File(repoPath))) {
            List<DiffEntry> diffs = getDiffEntries(git, commit);
            for (DiffEntry diff : diffs) {
                //String filePath = diff.getNewPath();
                String filePath = diff.getChangeType() == DiffEntry.ChangeType.DELETE ? diff.getOldPath() : diff.getNewPath();
                if (filePath.endsWith(".java")) {
                    javaFiles.add(filePath);
                }
            }
        }
        return javaFiles;
    }

    public static List<DiffEntry> getDiffEntries(Git git, RevCommit commit) throws IOException {
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(git.getRepository());
            List<DiffEntry> diffs;
            if (commit.getParentCount() > 0) {
                diffs = diffFormatter.scan(commit.getParent(0), commit);
            } else {
                diffs = diffFormatter.scan(null, commit); // Nessun genitore
            }
            return diffs;
        }
    }


    /*public static void processTickets(List<Ticket> tickets, String repoPath, List<Release> releases) {
        for (Ticket ticket : tickets) {
            for (RevCommit commit : ticket.getCommits()) {
                try {
                    List<String> modifiedFiles = getModifiedJavaFiles(commit, repoPath);
                    markBuggyFiles(ticket, modifiedFiles, releases, commit);
                } catch (IOException e) {
                    e.printStackTrace(); // Gestione delle eccezioni
                }
            }
        }
    }

    public static void markBuggyFiles(Ticket ticket, List<String> modifiedFiles, List<Release> releases, RevCommit commit) {
        Set<Integer> affectedVersions = new HashSet<>(ticket.getAffectedVersion());

        for (Release release : releases) {
            if (isCommitInRelease(release, commit) && affectedVersions.contains(release.getIndex())) {
                for (String file : modifiedFiles) {
                    for (FileJava releaseFile : release.getFiles()) {
                        if (releaseFile.getName().equals(file)) {
                            releaseFile.setBuggy(true);
                        }
                    }
                }
            }
        }
    }


    public static boolean isCommitInRelease(Release release, RevCommit commit) {
        return release.getCommits().contains(commit);
    }*/

    //sennò devo prendere tutti gli AV di un ticket, prendere i commit in quel ticket e vedere i file toccati
    //andare nella release indicata dal AV e mettere i file a isBuggy = true

    /*public static void processReleasesAndMarkBuggyFiles(List<Release> releases, List<Ticket> tickets, String repoPath) throws IOException {
        // Per ogni release
        for (Release release : releases) {
            List<RevCommit> releaseCommits = release.getCommits(); // Ottieni i commit associati alla release

            // Itera sui commit della release
            for (RevCommit commit : releaseCommits) {
                Ticket associatedTicket = findTicketByCommit(commit, tickets); // Trova il ticket associato al commit

                if (associatedTicket != null) { // Se esiste un ticket associato
                    // Verifica se la release corrente è nelle affected version del ticket
                    if (associatedTicket.getAffectedVersion().contains(release.getIndex())) {
                        // Ottieni i file modificati dal commit
                        System.out.println("AV " + associatedTicket.getAffectedVersion());
                        System.out.println("index: "+ release.getIndex());
                        List<String> modifiedFiles = getModifiedJavaFiles(commit, repoPath);

                        // Marca i file come buggy
                        markFilesAsBuggy(modifiedFiles, release);
                    }
                }
            }
        }
    }

    public static Ticket findTicketByCommit(RevCommit commit, List<Ticket> tickets) {
        // Cerca un ticket che contenga il commit nella sua lista
        for (Ticket ticket : tickets) {
            if (ticket.getCommits().contains(commit)) {
                return ticket; // Ritorna il ticket trovato
            }
        }
        return null; // Nessun ticket trovato
    }*/


    public static void markBuggyFilesUsingAffectedVersions(List<Ticket> tickets, List<Release> releases, String repoPath) {
        for (Ticket ticket : tickets) {
            List<Integer> affectedVersions = ticket.getAffectedVersion();

            // Per ogni versione affetta (AV)
            for (Integer affectedVersion : affectedVersions) {
                Release affectedRelease = getReleaseByVersion(releases, affectedVersion);

                if (affectedRelease != null) {
                    // Per ogni commit associato al ticket
                    for (RevCommit commit : ticket.getCommits()) {
                        try {
                            List<String> modifiedFiles = getModifiedJavaFiles(commit, repoPath);
                            // Marca i file come buggy nella release affetta
                            markFilesAsBuggy(modifiedFiles, affectedRelease);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public static Release getReleaseByVersion(List<Release> releases, int version) {
        for (Release release : releases) {
            if (release.getIndex() == version) {
                return release;
            }
        }
        return null; // Nessuna release trovata
    }

    public static void markFilesAsBuggy(List<String> modifiedFiles, Release release) {
        // Segna i file modificati nella release come buggy
        for (String modifiedFile : modifiedFiles) {
            for (FileJava releaseFile : release.getFiles()) {
                if (releaseFile.getName().equals(modifiedFile)) {
                    releaseFile.setBuggy(true); // Marca il file come buggy
                    //break;
                }
            }
        }
    }
}