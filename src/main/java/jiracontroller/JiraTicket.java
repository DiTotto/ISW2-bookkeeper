package jiracontroller;

import models.Release;
import models.Ticket;
import org.eclipse.jgit.revwalk.RevCommit;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

import static jiracontroller.JiraRelease.*;
import static utils.JSON.readJsonFromUrl;

public class JiraTicket {

    private JiraTicket() {
        throw new IllegalStateException("Utility class");
    }

    private static final String FIELDS_KEY = "fields";

    public static List<Ticket> getTickets(String project, List<Release> releaseList) throws IOException {

        List<Ticket> tickets = new ArrayList<>();

        Map<Integer, Release> releaseIDMap = createReleaseIDMap(project, releaseList);
        fetchAndProcessIssues(project, releaseIDMap, tickets); // recupero i bug e li processa, recuperando IV, data di apertura e di risoluzione del  ticket
        processTickets(tickets, releaseList); //viene recuperata IV e calcolo OV e FV

        return tickets;
    }

    // Metodo per creare la mappa delle release
    private static Map<Integer, Release> createReleaseIDMap(String project, List<Release> releaseList) throws IOException {
        if (releaseList == null) {
            releaseList = getRelease(project);
        }

        //inserisco tutte le release nella mappa e uso come chiave il loro id,
        //utile per recuperare le AV
        Map<Integer, Release> releaseIDMap = new HashMap<>();
        for (Release release : releaseList) {
            releaseIDMap.put(release.getId(), release);
        }
        return releaseIDMap;
    }

    // Metodo per recuperare e processare i bug
    private static void fetchAndProcessIssues(String project, Map<Integer, Release> releaseIDMap, List<Ticket> tickets) throws IOException {
        int total = 1;
        int i = 0;

        //Get JSON API for closed bugs w/ AV in the project
        do {
            //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
            int j = i + 1000;
            String url = buildJiraQueryURL(project, i, j);
            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");

            //Iterate through each bug
            for (int index = i; index < total && index < j; index++) {
                JSONObject issue = issues.getJSONObject(index % 1000);
                Ticket ticket = createTicketFromIssue(issue, releaseIDMap);
                tickets.add(ticket);
            }

            i = j;
        } while (i < total);

    }

    // Metodo per costruire la URL di query
    private static String buildJiraQueryURL(String project, int startAt, int maxResults) {
        return String.format(
                "https://issues.apache.org/jira/rest/api/2/search?jql=project=%%22%s%%22AND%%22issueType%%22=%%22Bug%%22AND(%%22status%%22=%%22closed%%22OR"
                        + "%%22status%%22=%%22resolved%%22)AND%%22resolution%%22=%%22fixed%%22&fields=key,resolutiondate,versions,created&startAt=%d&maxResults=%d",
                project, startAt, maxResults
        );
    }

    // Metodo per creare un oggetto Ticket da un JSONObject
    private static Ticket createTicketFromIssue(JSONObject issue, Map<Integer, Release> releaseIDMap) {
        String key = issue.get("key").toString();
        LocalDateTime openDate = LocalDateTime.parse(issue.getJSONObject(FIELDS_KEY).getString("created").substring(0, 16));
        LocalDateTime resolutionDate = LocalDateTime.parse(issue.getJSONObject(FIELDS_KEY).getString("resolutiondate").substring(0, 16));
        Integer injectionVersion = getInjectionVersion(issue, releaseIDMap);

        return new Ticket(key, openDate, resolutionDate, injectionVersion);
    }

    // Metodo per recuperare la injection version
    private static Integer getInjectionVersion(JSONObject issue, Map<Integer, Release> releaseIDMap) {
        JSONArray affectedVersions = issue.getJSONObject(FIELDS_KEY).getJSONArray("versions");
        if (affectedVersions.length() > 0) {
            //abbiamo informazioni sulle AV --> la prima AV è presumibilmente la IV
            //se ho AV allora la IV sarà la prima versione
            //l'id non ci va bene, a noi serve l'indice (1, 2, ...)
            Integer injectionVersionID = affectedVersions.getJSONObject(0).getInt("id");
            if (releaseIDMap.containsKey(injectionVersionID)) {
                //l'id non ci va bene, a noi serve l'indice (1, 2, ...)
                return releaseIDMap.get(injectionVersionID).getIndex();
            }
        }
        return null;
    }

    // Metodo per processare i ticket
    private static void processTickets(List<Ticket> tickets, List<Release> releaseList) {
        for (Ticket ticket : tickets) {
            ticket.setOpeningVersion(getOV(ticket, releaseList));
            ticket.setFixedVersion(getFV(ticket, releaseList));
        }
    }



    public static Integer getOV(Ticket ticket, List<Release> releases) {
        //per ogni release, se la data dell'apertura del ticket è più piccola della data della release,
        //allora è la OV che cerco
        for (Release release : releases) {
            if(ticket.getOpeningDate().compareTo(release.getReleaseDate()) < 0) {
                return release.getIndex();
            }
        }
        return null;
    }

    public static Integer getFV(Ticket ticket, List<Release> releases) {
        //come prima, se la resolution date è minore della release date allora è la FV che voglio
        for (Release release : releases) {
            if(ticket.getResolutionDate().compareTo(release.getReleaseDate()) < 0) {
                return release.getIndex();
            }
        }
        return null;
    }

    public static void commitsOfTheticket(List<RevCommit> commits, List<Ticket> tickets){
        for (Ticket ticket: tickets) {
            for (RevCommit commit: commits) {
                //--> \\b indica “word boundary”, quindi l’ID del ticket deve essere una parola separata e non parte di un’altra parola
                if (commit.getShortMessage().matches(".*\\b" + Pattern.quote(ticket.getId()) + "\\b.*")){
                    ticket.getCommits().add(commit);
                }
            }
        }
    }

    public static void removeTicketWithoutCommit(List<Ticket> tickets) {
        List<Ticket> ticketsToRemove = new ArrayList<>();
        for (Ticket ticket: tickets) {
            if (ticket.getCommits().isEmpty()){
                ticketsToRemove.add(ticket);
            }
        }
        tickets.removeAll(ticketsToRemove);
    }

    public static void fixTicketList(List<Ticket> tickets) {
        Iterator<Ticket> ticketIterator = tickets.iterator();
        while(ticketIterator.hasNext()) {
            Ticket ticket = ticketIterator.next();
            if(ticket.getFixedVersion() == null || ticket.getOpeningVersion() == null || ticket.getOpeningVersion() > ticket.getFixedVersion()) {
                ticketIterator.remove();
            } else if(ticket.getInjectedVersion() != null && ticket.getInjectedVersion() > ticket.getOpeningVersion()) {
                ticket.setInjectedVersion(null);
            } else if(ticket.getInjectedVersion() != null && ticket.getInjectedVersion().equals(ticket.getFixedVersion())) {
                ticketIterator.remove();
            }
        }
    }

    public static void calculateAV(Ticket ticket) {
        if (ticket.getInjectedVersion() != null && ticket.getFixedVersion() != null) {
            //l'AV sarà il range tra IV e FV-1
            List<Integer> affectedVersions = new ArrayList<>();
            for (int version = ticket.getInjectedVersion(); version < ticket.getFixedVersion(); version++) {
                affectedVersions.add(version);
            }
            ticket.setAffectedVersion(affectedVersions);
        }
    }



}