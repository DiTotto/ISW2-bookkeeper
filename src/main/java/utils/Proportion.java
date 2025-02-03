package utils;


import jiracontroller.JiraRelease;
import jiracontroller.JiraTicket;
import models.Ticket;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class Proportion {

    private static List<String> projForColdStart = new ArrayList<>();

    private static int movWinSize;
    private static double prop;
    private static final List<Ticket> ticketofProportion=new ArrayList<>();
    private static final Logger logger = Logger.getLogger(Proportion.class.getName());

    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_RESET = "\u001B[0m";

    private Proportion() {
        throw new IllegalStateException("Utility class");
    }


    public static void getProportion(List<Ticket> tickets, String projectName) throws IOException {

        // Utilizza una mappa per gestire le associazioni projectName -> projectsToAdd
        Map<String, List<String>> projectMap = Map.of(
                "BOOKKEEPER", List.of("AVRO", "ZOOKEEPER"),
                "SYNCOPE", List.of("OPENJPA", "PROTON")
        );

        // Aggiunge progetti di default se il projectName non è presente nella mappa
        List<String> projectsToAdd = projectMap.getOrDefault(projectName,
                List.of("AVRO", "OPENJPA", "ZOOKEEPER", "STORM", "TAJO"));

        projForColdStart.addAll(projectsToAdd);

        int numTickets=tickets.size();
        logger.log(java.util.logging.Level.INFO, ANSI_WHITE + "Numero di ticket: {0}" + ANSI_RESET, numTickets);
        //movWinSize=Math.max(1, numTickets / 100); //uso il 10% dei ticket, ma almeno 1 ticket
        movWinSize=Math.max(1, 5); //uso il 10% dei ticket, ma almeno 1 ticket
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
                        logger.log(java.util.logging.Level.INFO,ANSI_WHITE + "Cold Start" + ANSI_RESET);
                        prop = coldStart();
                        logger.log(java.util.logging.Level.INFO,ANSI_WHITE + "Moving window" + ANSI_RESET);
                    }
                    //non faccio nulla e aspetto di riempire la finestra mobile
                } else {
                    prop = movingWindow();

                }

                setInjectedVersion(ticket);
            }
        }
        logger.log(java.util.logging.Level.INFO, ANSI_WHITE+"Proportion calculated for project {0}: {1}" + ANSI_RESET, new Object[]{projectName, prop});
    }

    public static void setInjectedVersion(Ticket ticket) {

        if (Objects.equals(ticket.getFixedVersion(), ticket.getOpeningVersion())) {
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
            if(!Objects.equals(t.getFixedVersion(), t.getOpeningVersion())) {
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
        List<Double> propCalc = new ArrayList<>();
        //calcolo la proportion per ogni progetto e lo inserisco nella lista
        //utilizzo la mediana delle proporzioni per fare il cold start

        for(String proj : projForColdStart) {

            double p = calculateProportionForProject(proj);
            if (p != -1) {
                propCalc.add(p);
            }
            logger.log(java.util.logging.Level.INFO, ANSI_WHITE+"Proportion calculated for project {0}: {1}" + ANSI_RESET, new Object[]{proj, p});
        }

        //restituisco la mediana delle proportion
        return calculateMedian(propCalc);

        //////////////////////////
        // STUDIARE ASSUNZIONE, è piu giusto usare la media o usare la mediana?
        // dipende da quanto sono simili tra loro  i progetti considerati
        //////////////////////////

        //restituisco la mediana delle proportion
        //se dispari restituisco il valore centrale
        //se pari restituisco la media dei due valori centrali

        //Il codice seguente è necessario nel caso in cui debba essere usata la mediana piuttosto che la media


        // Se la lista è vuota, restituisco 0 per evitare errori


        // quando non ci sono valori outlier, utilizzare la media invece che la mediana può risultare
        // in una stima più accurata e più corretta
        // Calcolo della media delle proportion


    }

    private static double calculateProportionForProject(String proj) throws IOException {
        List<Ticket> tickets = JiraTicket.getTickets(proj, JiraRelease.getRelease(proj));
        double totalProportion = 0;
        int counter = 0;

        for (Ticket ticket : tickets) {
            if (isValidTicket(ticket)) {
                double proportion = (double) (ticket.getFixedVersion() - ticket.getInjectedVersion()) /
                        (ticket.getFixedVersion() - ticket.getOpeningVersion());
                totalProportion += (proportion > 1) ? proportion : 1;
                counter++;
            }
        }
        return (counter != 0) ? totalProportion / counter : -1;
    }

    private static boolean isValidTicket(Ticket ticket) {
        return ticket.getInjectedVersion() != null &&
                ticket.getOpeningVersion() != null &&
                ticket.getFixedVersion() != null &&
                !Objects.equals(ticket.getOpeningVersion(), ticket.getFixedVersion());
    }

    private static double calculateMedian(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        Collections.sort(values);
        int size = values.size();
        return (size % 2 == 1) ? values.get(size / 2) :
                (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
    }
}