package utils;

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
import java.util.ArrayList;
import java.util.List;

public class Bugginess {

    /* ---------------- CALCOLO ISBUGGY ----------------------- */

    //ottengo i file modificati per ogni commit
    public static List<String> getModifiedJavaFiles(RevCommit commit, String repoPath) throws IOException {
        List<String> javaFiles = new ArrayList<>();
        try (Git git = Git.open(new File(repoPath))) {
            List<DiffEntry> diffs = getDiffEntries(git, commit);
            for (DiffEntry diff : diffs) {

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
                diffs = diffFormatter.scan(null, commit);
            }
            return diffs;
        }
    }


    public static void markBuggyFilesUsingAffectedVersions(List<Ticket> tickets, List<Release> releases, String repoPath) {
        for (Ticket ticket : tickets) {
            List<Integer> affectedVersions = ticket.getAffectedVersion();

            for (Integer affectedVersion : affectedVersions) {
                Release affectedRelease = getReleaseByVersion(releases, affectedVersion);

                if (affectedRelease != null) {

                    for (RevCommit commit : ticket.getCommits()) {
                        try {
                            List<String> modifiedFiles = getModifiedJavaFiles(commit, repoPath);
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
        return null;
    }

    public static void markFilesAsBuggy(List<String> modifiedFiles, Release release) {

        for (String modifiedFile : modifiedFiles) {
            for (FileJava releaseFile : release.getFiles()) {
                if (releaseFile.getName().equals(modifiedFile)) {
                    releaseFile.setBuggy(true);
                    break;
                }
            }
        }
    }
}