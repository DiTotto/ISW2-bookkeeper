package models;

import org.eclipse.jgit.revwalk.RevCommit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Release {

    private String name;
    private int id;
    private LocalDateTime releaseDate;
    private int index;

    private List<FileJava> files;

    private List<RevCommit> commits;

    public Release(String name, int id, LocalDateTime releaseDate, int index) {
        this.name = name;
        this.id = id;
        this.releaseDate = releaseDate;
        this.index = index;
        this.files = new ArrayList<>();
        this.commits = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public int getIndex() {
        return index;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setReleaseDate(LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<FileJava> getFiles() {
        return files;
    }

    public void addFile(FileJava file) {
        this.files.add(file);
    }

    public List<RevCommit> getCommits() {
        return commits;
    }

    public void addCommit(RevCommit commit) {
        this.commits.add(commit);
    }

    public FileJava getJavaFileByName(String fileName) {
        for (FileJava file : files) {
            if (file.getName().equals(fileName)) {
                return file;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Release{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", releaseDate=" + releaseDate +
                ", index=" + index +
                ", files=" + files +
                ", commits=" + commits +
                '}';
    }
}