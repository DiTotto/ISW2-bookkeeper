package models;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class Ticket {

    private List<RevCommit> commits=new ArrayList<>();
    private String id;
    private LocalDateTime openingDate;
    private LocalDateTime resolutionDate;
    private Integer injectedVersion;
    private Integer openingVersion;
    private Integer fixedVersion;
    private List<Integer> affectedVersion = new ArrayList<>();

    public Ticket(String id, LocalDateTime openingDate, LocalDateTime resolutionDate, Integer injectedVersion) {
        this.id=id;
        this.openingDate=openingDate;
        this.injectedVersion=injectedVersion;
        this.resolutionDate = resolutionDate;
    }

    /* --- GETTERS --- */
    public List<RevCommit> getCommits() {
        return commits;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getOpeningDate() {
        return openingDate;
    }

    public LocalDateTime getResolutionDate() {
        return resolutionDate;
    }

    public Integer getInjectedVersion() {
        return injectedVersion;
    }

    public Integer getOpeningVersion() {
        return openingVersion;
    }

    public Integer getFixedVersion() {
        return fixedVersion;
    }

    public List<Integer> getAffectedVersion() {
        return affectedVersion;
    }

    /* --- SETTERS --- */

    public void setCommits(List<RevCommit> commits) {
        this.commits = commits;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setOpeningDate(LocalDateTime openingDate) {
        this.openingDate = openingDate;
    }

    public void setResolutionDate(LocalDateTime resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public void setInjectedVersion(Integer injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    public void setOpeningVersion(Integer openingVersion) {
        this.openingVersion = openingVersion;
    }

    public void setFixedVersion(Integer fixedVersion) {
        this.fixedVersion = fixedVersion;
    }

    public void setAffectedVersion(List<Integer> affectedVersion) {
        this.affectedVersion = affectedVersion;
    }

    public void addCommit(RevCommit commit) {
        this.commits.add(commit);
    }

    public void addAffectedVersion(Integer av) {
        this.affectedVersion.add(av);
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "commits=" + commits +
                ", id='" + id + '\'' +
                ", openingDate=" + openingDate +
                ", resolutionDate=" + resolutionDate +
                ", injectedVersion=" + injectedVersion +
                ", openingVersion=" + openingVersion +
                ", fixedVersion=" + fixedVersion +
                ", affectedVersion=" + affectedVersion +
                '}';
    }
}