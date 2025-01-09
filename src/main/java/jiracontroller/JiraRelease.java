package jiracontroller;


import models.Release;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


import static utils.JSON.readJsonFromUrl;

public class JiraRelease {

    public static final HashMap<LocalDateTime, String> releaseNames = new HashMap<>();
    public static final HashMap<LocalDateTime, String> releaseID = new HashMap<>();
    public static ArrayList<LocalDateTime> releases;
    public static Integer numVersions;

    private JiraRelease() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Release> getRelease(String project) throws IOException {
        List<Release> releaseList = new ArrayList<>();
        releases = new ArrayList<LocalDateTime>();
        Integer i;
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + project;

        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        for (i = 0; i < versions.length(); i++ ) {
            String name = "";
            String id = "";
            if(versions.getJSONObject(i).has("releaseDate")) {
                if (versions.getJSONObject(i).has("name"))
                    name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id"))
                    id = versions.getJSONObject(i).get("id").toString();
                addRelease(versions.getJSONObject(i).get("releaseDate").toString(),
                        name,id);
            }
        }

        //order releases by date
        Collections.sort(releases, new Comparator<LocalDateTime>(){
            //@Override
            public int compare(LocalDateTime o1, LocalDateTime o2) {
                return o1.compareTo(o2);
            }
        });
        if (releases.size() < 6)
            return null;

        //delete half of the releases
        int halfSize = (releases.size() / 2);
        releases = new ArrayList<>(releases.subList(0, halfSize));

        int index = 1;
        for (LocalDateTime release : releases) {
            Release r  = new Release(
                    releaseNames.get(release),
                    Integer.parseInt(releaseID.get(release)),
                    release,
                    index
            );

            releaseList.add(r);
            index++;

        }

        //calculateMetric(releaseList, "\\Users\\lucad\\Documents\\bookkeeper_fork"); //this is done two times
        //one times i the main when we do releases = JiraRelease.getRelease("BOOKKEEPER");
        // then second time in the JiraTicket when is called the getTicket method, but in this case is needed only the list of the releases, so it is not necessary to calculate the metrics
        return releaseList;
    }


    public static void addRelease(String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        if (!releases.contains(dateTime))
            releases.add(dateTime);
        releaseNames.put(dateTime, name);
        releaseID.put(dateTime, id);
        return;
    }

    public static Map<LocalDateTime, String> getReleaseNames() {
        return releaseNames;
    }

    // Getter per releaseID
    public static Map<LocalDateTime, String> getReleaseID() {
        return releaseID;
    }



}