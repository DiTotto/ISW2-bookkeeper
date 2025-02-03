package utils;


import jiracontroller.JiraRelease;
import jiracontroller.JiraTicket;
import models.Release;
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
        System.out.println("Proportion: " + prop);
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
        List<Ticket> tickets = new ArrayList<>();
        List<Double> propCalc = new ArrayList<>();
        List<Release> releases = new ArrayList<>();



        //calcolo la proportion per ogni progetto e lo inserisco nella lista
        //utilizzo la media delle proporzioni per fare il cold start

        for(String proj : projForColdStart) {
            int counter = 0;
            double p = 0;
            tickets.clear();
            releases = JiraRelease.getRelease(proj);
            tickets = JiraTicket.getTickets(proj, releases);
            for(Ticket ticket: tickets){
                // verifico che il ticket abbia le informazioni necessarie
                if(ticket.getInjectedVersion() != null && ticket.getOpeningVersion() != null && ticket.getFixedVersion() != null
                        && (!Objects.equals(ticket.getOpeningVersion(), ticket.getFixedVersion()))){
                    double proportion = (double) (ticket.getFixedVersion() - ticket.getInjectedVersion()) / (ticket.getFixedVersion() - ticket.getOpeningVersion());
                    p += (proportion > 1) ? proportion : 1;
                    counter++;
                }
            }
            if(counter != 0) {
                p = p / counter;
                propCalc.add(p);
            }
            logger.log(java.util.logging.Level.INFO, ANSI_WHITE+"Proportion calculated for project {0}: {1}" + ANSI_RESET, new Object[]{proj, p});
        }

        //////////////////////////
        // STUDIARE ASSUNZIONE, è piu giusto usare la media o usare la mediana?
        // dipende da quanto sono simili tra loro  i progetti considerati
        //////////////////////////

        //restituisco la mediana delle proportion
        //se dispari restituisco il valore centrale
        //se pari restituisco la media dei due valori centrali

        //Il codice seguente è necessario nel caso in cui debba essere usata la mediana piuttosto che la media


        // Se la lista è vuota, restituisco 0 per evitare errori
        if (propCalc.isEmpty()) {
            return 0;
        }
        // Ordino la lista in ordine crescente
        Collections.sort(propCalc);

       // Calcolo la mediana
        int size = propCalc.size();
        if (size % 2 == 1) {
            return propCalc.get(size / 2);  // Dispari: prendo l'elemento centrale
        } else {
            return (propCalc.get(size / 2 - 1) + propCalc.get(size / 2)) / 2.0;  // Pari: media dei due centrali
        }

        // restituisco la media dei valori di proportion calcolati
        // dato che non ci sono valori outlier, utilizzare la media invece che la mediana può risultare
        // in una stima più accurata e più corretta
        // Calcolo della media delle proportion


    }

}