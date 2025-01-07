package utils;


import jiracontroller.JiraRelease;
import jiracontroller.JiraTicket;
import models.Release;
import models.Ticket;

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

    public static void getProportion(List<Ticket> tickets, String projectName) throws IOException {

        List<String> projectsToAdd;
        if (projectName.equals("BOOKKEEPER")){
            projectsToAdd = List.of("AVRO", "ZOOKEEPER");
        }else if (projectName.equals("SYNCOPE")){
            projectsToAdd = List.of("OPENJPA", "PROTON");
        }else{
            projectsToAdd = List.of("AVRO", "OPENJPA", "ZOOKEEPER", "STORM", "TAJO");
        }
        //List<String> projectsToAdd = List.of("AVRO", "OPENJPA", "ZOOKEEPER", "STORM", "TAJO");
        //List<String> projectsToAdd = List.of("AVRO");
        projForColdStart.addAll(projectsToAdd);

        int numTickets=tickets.size();
        System.out.println("Numero di ticket: " + numTickets);
        movWinSize=Math.max(1, numTickets / 100); //uso il 10% dei ticket, ma almeno 1 ticket
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
                        //System.out.println("Proportion calculated from cold start: " + prop);
                        System.out.println("moving window");
                    }
                    //non faccio nulla e aspetto di riempire la finestra mobile
                } else {
                    //System.out.println("moving window");
                    prop = movingWindow();

                }

                //System.out.println("Proportion: " + prop);

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
        List<Release> releases = new ArrayList<>();
        double p = 0;
        int counter = 0;

        //calcolo la proportion per ogni progetto e lo inserisco nella lista
        //utilizzo la media delle proporzioni per fare il cold start

        for(String proj : projForColdStart) {
            counter = 0;
            tickets.clear();
            releases = JiraRelease.getRelease(proj);
            tickets = JiraTicket.getTickets(proj, releases);
            for(Ticket ticket: tickets){
                if(ticket.getInjectedVersion() != null && ticket.getOpeningVersion() != null && ticket.getFixedVersion() != null
                        && (!Objects.equals(ticket.getOpeningVersion(), ticket.getFixedVersion()))){
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
            System.out.println("Proportion calculated for project " + proj + ": " + p);


        }

        /*tickets = JiraTicket.getTickets("AVRO", null);
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
        */

        //////////////////////////
        // STUDIARE ASSUNZIONE, è piu giusto usare la media o usare la mediana?
        // dipende da quanto sono simili tra loro  i progetti considerati
        //////////////////////////

        //restituisco la mediana delle proportion
        //se dispari restituisco il valore centrale
        //se pari restituisco la media dei due valori centrali
        /*prop_calc.sort(Comparator.naturalOrder());
        if(prop_calc.size() % 2 == 0) {
            return ((prop_calc.get(prop_calc.size() / 2) + prop_calc.get(prop_calc.size() / 2 - 1)) / 2);
        } else {
            return (prop_calc.get(prop_calc.size() / 2));
        }*/

        // restituisco la media dei valori di proportion calcolati
        // dato che non ci sono valori outlier, utilizzare la media invece che la mediana può risultare
        // in una stima più accurata e più corretta
        // Calcolo della media delle proportion
        double sum = 0;
        for (double proportion : prop_calc) {
            sum += proportion;
        }

        // Verifica che prop_calc non sia vuota per evitare divisione per zero
        if (!prop_calc.isEmpty()) {
            return sum / prop_calc.size();
        } else {
            return 0; // oppure un altro valore di default, se non ci sono proporzioni
        }




    }

}