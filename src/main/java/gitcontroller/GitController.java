package gitcontroller;

import exceptions.GitOperationException;
import models.Release;
import models.FileJava;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.*;
import java.util.logging.Logger;

public class GitController {

    private static final Logger logger = Logger.getLogger(GitController.class.getName());

    private static final String JAVA_FILE_EXTENSION = ".java";
    private GitController() {
        throw new IllegalStateException("Utility class");
    }

    // Common method to retrieve all commits from the repository
    private static List<RevCommit> getAllCommits(String repoPath) {
        List<RevCommit> commitList = new ArrayList<>();
        try (Git git = Git.open(new File(repoPath))) {
            Iterable<RevCommit> commits = git.log().all().call();
            for (RevCommit commit : commits) {
                commitList.add(commit);
            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace(); // Consider logging the error instead of printing
        }
        return commitList;
    }

    public static void calculateMetric(List<Release> releases, String repoPath) {
        associateCommitsWithReleases(releases, repoPath);
        associateFilesWithCommits(releases, repoPath);
        try { calculateLOCForReleaseFiles(releases, repoPath);} catch (GitOperationException e) { e.printStackTrace(); System.exit(1); }
        calculateNumberOfRevisionsPerFile(releases, repoPath);
        calculateTouchedLOCAndRemovedLOCForReleaseFiles(releases, repoPath);
        calculateAddedLOCAndMaxPerFile(releases, repoPath);
        calculateAvgAddedLOC(releases);
        calculateNumberOfAuthorsPerFile(releases, repoPath);

    }

    public static List<RevCommit> retrieveCommits(String path) {
        logger.log(java.util.logging.Level.INFO, "\u001B[37mRetrieving commits from repository...\u001B[0m");
        Iterable<RevCommit> commits;
        List<RevCommit> commitList=new ArrayList<>();
        try (Git git = Git.open((Path.of(path).toFile()))) {
            commits = git.log().all().call();
            for (RevCommit commit : commits) {
                commitList.add(commit);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoHeadException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return commitList;
    }

    //associo i commit alle release
    public static void associateCommitsWithReleases(List<Release> releases, String repoPath) {
        logger.log(java.util.logging.Level.INFO , "\u001B[37mAssociating commits with releases...\u001B[0m");
        List<RevCommit> commits = getAllCommits(repoPath);

        // BUGGY perche in questo modo inserisce in una certa release, tutti i commit associabili a quella release e i
        // commit associati alle release precedenti a livello di date

        for (int i = 0; i < releases.size(); i++) {
            Release currentRelease = releases.get(i);
            long currentReleaseTime = currentRelease.getReleaseDate().toInstant(ZoneOffset.UTC).toEpochMilli();
            long previousReleaseTime = i > 0 ? releases.get(i - 1).getReleaseDate().toInstant(ZoneOffset.UTC).toEpochMilli() : Long.MIN_VALUE;

            for (RevCommit commit : commits) {
                long commitTime = commit.getCommitTime() * 1000L;
                if (commitTime >= previousReleaseTime && commitTime < currentReleaseTime) {
                    currentRelease.addCommit(commit);
                }
            }
        }

        //rimuovo le release senza commit
        releases.removeIf(release -> release.getCommits().isEmpty());

        //aggiorno l'indice di ogni release
        for (int i = 0; i < releases.size(); i++) {
            releases.get(i).setIndex(i + 1);
        }

        // questo perche è risultato che la release 4 non ha commit associati, percio scaliamo l'indice di tutte le
        // release e consideriamo la release 4 come la release 3, ovvero escludiamo la release 4. è ragionevole poiche
        // in quel lasso di tempo non sono state apportate modifiche a nessun file

    }

    private static void processRelease(Repository repository, Release release) {
        for (RevCommit releaseCommit : release.getCommits()) {
            processCommit(repository, release, releaseCommit);
        }
    }

    //se il commit è incluso in quella release e se il commit è associato alla release
    //inizializzo un nuovo TreeWalk per navigare l'albero del commit
    private static void processCommit(Repository repository, Release release, RevCommit releaseCommit) {
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(releaseCommit.getTree());
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                processFile(treeWalk.getPathString(), release);
            }
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, e,
                    () -> "Error processing commit: " + releaseCommit.getName());
        }
    }

    private static void processFile(String filePath, Release release) {
        if (filePath.endsWith(JAVA_FILE_EXTENSION) && !fileExistsInRelease(filePath, release)) {
            FileJava file = new FileJava(filePath);
            release.addFile(file);
        }
    }

    private static boolean fileExistsInRelease(String filePath, Release release) {
        for (FileJava existingFile : release.getFiles()) {
            if (existingFile.getName().equals(filePath)) {
                return true;
            }
        }
        return false;
    }



    //associo i file ai commit --> l'idea base è tirarmi fuori tutti i file del progetto dalla lista dei file
    //toccati dai commit
    public static void associateFilesWithCommits(List<Release> releases, String repoPath) {
        logger.log(java.util.logging.Level.INFO, "\u001B[37mAssociating files with commits...\u001B[0m");
        try (Repository repository = Git.open(new File(repoPath)).getRepository()) {
                //ottengo tutti i commit dal repository

            for (Release release : releases) {
                processRelease(repository, release);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void calculateLOCForReleaseFiles(List<Release> releases, String repoPath) throws GitOperationException{
        logger.log(java.util.logging.Level.INFO, "\u001B[37mCalculating LOC for release files...\u001B[0m");
        try (Repository repository = Git.open(new File(repoPath)).getRepository()) {
            for (Release release : releases) {
                for (RevCommit commit : release.getCommits()) {
                    //for every release and for every commit in that release calculate the LOC for each file

                    List<FileJava> javaFiles = release.getFiles();
                    setFileLOCForCommit(repository, commit, javaFiles);
                }
            }
        } catch (IOException e) {
            throw new GitOperationException("IO error while accessing the repository", e);
        } catch (GitAPIException e) {
            throw new GitOperationException("Git API error during LOC calculation", e);
        }
    }


    //calcolo in maniera cumulativa i LOC totali per ogni file Java per ogni release
    //i LOC sono calcolati tra una release e l'altra

    private static void setFileLOCForCommit(Repository repo, RevCommit commit, List<FileJava> javaFiles) throws IOException, GitAPIException {
        try (Git git = new Git(repo)) {
            // Inizializza il lettore e i parser per ottenere il diff tra il commit corrente e il precedente
            ObjectReader reader = repo.newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            RevCommit parentCommit = commit.getParentCount() > 0 ? commit.getParent(0) : null;

            if (parentCommit != null) {
                oldTreeIter.reset(reader, parentCommit.getTree());
            }

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, commit.getTree());

            // Ottieni i diff tra il commit corrente e il precedente
            // in questo modo non vengono calcolati i LOC per tutti i file ad ogni commit, ma solo per i file modificati
            // in quel commit specifico
            List<DiffEntry> diffs = git.diff()
                    .setNewTree(newTreeIter)
                    .setOldTree(parentCommit != null ? oldTreeIter : null)
                    .call();

            for (DiffEntry entry : diffs) {
                String filePath = entry.getNewPath();
                if (filePath.endsWith(JAVA_FILE_EXTENSION)) {
                    // Se il file modificato è un file Java, calcola i LOC solo per questo file
                    for (FileJava javaFile : javaFiles) {
                        if (javaFile.getName().equals(filePath)) {
                            calculateLOCForFile(repo, entry, javaFile);
                        }
                    }
                }
            }
        }
    }

    private static void calculateLOCForFile(Repository repo, DiffEntry entry, FileJava javaFile) throws IOException {
        ObjectId objectId = entry.getNewId().toObjectId();
        ObjectLoader loader = repo.open(objectId);

        // Legge il contenuto del file e conta le LOC
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(loader.openStream()))) {
            String line;
            int numLines = 0;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty() && !(trimmedLine.startsWith("/*") || trimmedLine.startsWith("*") || trimmedLine.startsWith("//"))) {
                    numLines++;
                }
            }
            // Aggiorna le LOC totali nel JavaFile
            javaFile.setLoc(javaFile.getLoc() + numLines);
        }
    }



    private static AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit) throws IOException {
        RevTree tree = commit.getTree();
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        try (ObjectReader reader = repository.newObjectReader()) {
            treeParser.reset(reader, tree);
        }
        return treeParser;
    }

    //calcolare il numero di revisioni per ogni file Java in ogni release
    public static void calculateNumberOfRevisionsPerFile(List<Release> releases, String repoPath) {
        logger.log(java.util.logging.Level.INFO, "\u001B[37mCalculating number of revisions per file...\u001B[0m");
        try (Repository repository = Git.open(new File(repoPath)).getRepository(); Git git = new Git(repository)) {
                for (Release release : releases) {
                    Map<String, Integer> fileRevisions = new HashMap<>(); //mappa per tracciare numero di revisioni per file Java
                    fileRevisions = calculateRevisionsForRelease(release, repository);
                    updateReleaseFilesWithRevisions(release, fileRevisions);
                }
            }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Integer> calculateRevisionsForRelease(Release release, Repository repository) throws IOException {
        Map<String, Integer> fileRevisions = new HashMap<>();
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(repository);

            //per ogni commit itero sui file modificati
            for (RevCommit commit : release.getCommits()) {
                List<DiffEntry> diffs = getDiffsForCommit(commit, diffFormatter);
                updateFileRevisions(fileRevisions, diffs);
            }
        }
        return fileRevisions;
    }

    private static List<DiffEntry> getDiffsForCommit(RevCommit commit, DiffFormatter diffFormatter) throws IOException {
        //esegue diff tra commit attuale e primo genitore per capire quali file sono stati modificati
        if (commit.getParentCount() > 0) {
            // Compare current commit with its first parent
            return diffFormatter.scan(commit.getParent(0), commit);
        } else {
            //quando ho un commit senza genitore, controllo solo i file modificati
            //uso un albero vuoto per il confronto --> non ho genitori
            // Compare current commit with an empty tree (no parent)
            return diffFormatter.scan(null, commit);
        }
    }

    private static void updateFileRevisions(Map<String, Integer> fileRevisions, List<DiffEntry> diffs) {
        for (DiffEntry diff : diffs) {
            String filePath = diff.getNewPath();
            // Considera solo i file Java
            if (filePath.endsWith(JAVA_FILE_EXTENSION)) {
                fileRevisions.put(filePath, fileRevisions.getOrDefault(filePath, 0) + 1);
            }
        }
    }

    private static void updateReleaseFilesWithRevisions(Release release, Map<String, Integer> fileRevisions) {
        //per ogni file Java, aggiorno il numero di revisioni per ogni release
        for (Map.Entry<String, Integer> entry : fileRevisions.entrySet()) {
            String fileName = entry.getKey();
            int numberOfRevisions = entry.getValue();

            FileJava javaFile = release.getJavaFileByName(fileName);
            if (javaFile == null) {
                javaFile = new FileJava(fileName);
                release.addFile(javaFile);
            }
            javaFile.setNr(numberOfRevisions);
        }
    }

    public static void calculateTouchedLOCAndRemovedLOCForReleaseFiles(List<Release> releases, String repoPath) {
        logger.log(java.util.logging.Level.INFO, "\u001B[37mCalculating touched LOC and removed LOC for release files...\u001B[0m");
        try (Repository repository = Git.open(new File(repoPath)).getRepository()) {
            for (Release release : releases) {
                Map<String, Integer> totalTouchedLocPerFile = new HashMap<>();
                Map<String, Integer> totalRemovedLocPerFile = new HashMap<>();
                Map<String, Integer> maxRemovedLocPerFile = new HashMap<>();

                for (RevCommit commit : release.getCommits()) {
                    List<FileJava> javaFiles = release.getFiles();
                    calculateTouchedLOCAndRemovedLOCForCommit(repository, commit, javaFiles,
                            totalTouchedLocPerFile,
                            totalRemovedLocPerFile,
                            maxRemovedLocPerFile);
                }

                //aggiorno i valori per ogni file
                for (FileJava javaFile : release.getFiles()) {
                    String fileName = javaFile.getName();

                    int revisionCount = javaFile.getNr(); //ottengo le revisioni prima calcolate
                    if (revisionCount > 0) {
                        int totalTouched = totalTouchedLocPerFile.getOrDefault(fileName, 0);
                        javaFile.setAvgLocTouched(totalTouched / revisionCount);
                    }

                    javaFile.setLocTouched(totalTouchedLocPerFile.getOrDefault(fileName, 0));
                    javaFile.setTotalLocRemoved(totalRemovedLocPerFile.getOrDefault(fileName, 0));
                    javaFile.setMaxLocRemoved(maxRemovedLocPerFile.getOrDefault(fileName, 0));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //calcolo delle LOC toccate e rimosse per ogni commit
    private static void calculateTouchedLOCAndRemovedLOCForCommit(Repository repository, RevCommit commit,
                                                                  List<FileJava> javaFiles,
                                                                  Map<String, Integer> totalTouchedLocPerFile,
                                                                  Map<String, Integer> totalRemovedLocPerFile,
                                                                  Map<String, Integer> maxRemovedLocPerFile)
            throws IOException {
        RevCommit parent = commit.getParentCount() > 0 ? commit.getParent(0) : null;

        if (parent == null) {
            return;
        }

        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);

            List<DiffEntry> diffs = getDiffEntries(repository, parent, commit, diffFormatter);

            for (DiffEntry diff : diffs) {
                processDiffEntry(diff, javaFiles, diffFormatter, totalTouchedLocPerFile, totalRemovedLocPerFile, maxRemovedLocPerFile);

            }
        }
    }

    private static List<DiffEntry> getDiffEntries(Repository repository, RevCommit parent, RevCommit commit, DiffFormatter diffFormatter) throws IOException {
        AbstractTreeIterator parentTreeParser = prepareTreeParser(repository, parent);
        AbstractTreeIterator commitTreeParser = prepareTreeParser(repository, commit);
        return diffFormatter.scan(parentTreeParser, commitTreeParser);
    }

    private static void processDiffEntry(DiffEntry diff, List<FileJava> javaFiles, DiffFormatter diffFormatter,
                                         Map<String, Integer> totalTouchedLocPerFile,
                                         Map<String, Integer> totalRemovedLocPerFile,
                                         Map<String, Integer> maxRemovedLocPerFile) throws IOException {
        String filePath = diff.getNewPath();
        if (!filePath.endsWith(JAVA_FILE_EXTENSION)) {
            return;
        }

        for (FileJava javaFile : javaFiles) {
            if (javaFile.getName().equals(filePath)) {
                processFileEdits(diff, diffFormatter, filePath, totalTouchedLocPerFile, totalRemovedLocPerFile, maxRemovedLocPerFile);
            }
        }
    }

    private static void processFileEdits(DiffEntry diff, DiffFormatter diffFormatter, String filePath,
                                         Map<String, Integer> totalTouchedLocPerFile,
                                         Map<String, Integer> totalRemovedLocPerFile,
                                         Map<String, Integer> maxRemovedLocPerFile) throws IOException {
        int addedLines = 0;
        int removedLines = 0;

        for (Edit edit : diffFormatter.toFileHeader(diff).toEditList()) {
            addedLines += edit.getEndB() - edit.getBeginB();
            removedLines += edit.getEndA() - edit.getBeginA();
        }

        int locTouched = addedLines + removedLines;
        totalTouchedLocPerFile.put(filePath, totalTouchedLocPerFile.getOrDefault(filePath, 0) + locTouched);
        totalRemovedLocPerFile.put(filePath, totalRemovedLocPerFile.getOrDefault(filePath, 0) + removedLines);
        maxRemovedLocPerFile.put(filePath, Math.max(maxRemovedLocPerFile.getOrDefault(filePath, 0), removedLines));
    }

    public static void calculateAddedLOCAndMaxPerFile(List<Release> releases, String repoPath) {
        logger.log(java.util.logging.Level.INFO, "\u001B[37mCalculating added LOC and max for release files...\u001B[0m");
        try (Repository repository = Git.open(new File(repoPath)).getRepository()) {
            try (Git git = new Git(repository)) {
                for (Release release : releases) {
                    processReleaseForLOC(repository, release);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processReleaseForLOC(Repository repository, Release release) throws IOException {
        Map<String, Integer> maxLocAddedPerFile = new HashMap<>();

        for (RevCommit commit : release.getCommits()) {
            processCommitForLOC(repository, commit, release.getFiles(), maxLocAddedPerFile);
        }

        updateJavaFilesWithMaxLOC(release, maxLocAddedPerFile);
    }

    private static void processCommitForLOC(Repository repository, RevCommit commit, List<FileJava> javaFiles,
                                      Map<String, Integer> maxLocAddedPerFile) throws IOException {
        RevCommit parent = commit.getParentCount() > 0 ? commit.getParent(0) : null;
        if (parent != null) {
            calculateAddedLOCAndMaxForCommit(repository, commit, parent, javaFiles, maxLocAddedPerFile);
        } else {
            calculateAddedLOCAndMaxForFirstCommit(repository, commit, javaFiles, maxLocAddedPerFile);
        }
    }

    private static void updateJavaFilesWithMaxLOC(Release release, Map<String, Integer> maxLocAddedPerFile) {
        for (Map.Entry<String, Integer> entry : maxLocAddedPerFile.entrySet()) {
            String fileName = entry.getKey();
            int maxLocAdded = entry.getValue();
            FileJava javaFile = release.getJavaFileByName(fileName);

            if (javaFile == null) {
                javaFile = new FileJava(fileName);
                release.addFile(javaFile);
            }
            javaFile.setMaxLocAdded(maxLocAdded);
        }
    }

    //calcolo LOC aggiunte e MAX LOX per commit
    private static void calculateAddedLOCAndMaxForCommit(Repository repository, RevCommit commit, RevCommit parent, List<FileJava> javaFiles, Map<String, Integer> maxLocAddedPerFile) throws IOException {
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);

            AbstractTreeIterator parentTreeParser = prepareTreeParser(repository, parent);
            AbstractTreeIterator commitTreeParser = prepareTreeParser(repository, commit);

            List<DiffEntry> diffs = diffFormatter.scan(parentTreeParser, commitTreeParser);


            for (DiffEntry diff : diffs) {
                String filePath = diff.getNewPath();

                if (filePath.endsWith(JAVA_FILE_EXTENSION)) {
                    int addedLines = 0;

                    for (Edit edit : diffFormatter.toFileHeader(diff).toEditList()) {
                        addedLines += edit.getEndB() - edit.getBeginB();
                    }

                    for (FileJava javaFile : javaFiles) {
                        if (javaFile.getName().equals(filePath)) {

                            javaFile.setLocAdded(javaFile.getLocAdded() + addedLines);
                            maxLocAddedPerFile.put(filePath, Math.max(maxLocAddedPerFile.getOrDefault(filePath, 0), addedLines));
                        }
                    }
                }
            }
        }
    }

    //se è il primissimo commit --> non ho parent
    private static void calculateAddedLOCAndMaxForFirstCommit(Repository repository, RevCommit commit, List<FileJava> javaFiles, Map<String, Integer> maxLocAddedPerFile) throws IOException {
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            logger.log(java.util.logging.Level.INFO, "\u001B[37mCalculating added LOC and max for first commit...\u001B[0m");
            diffFormatter.setRepository(repository);
            List<DiffEntry> diffs = diffFormatter.scan(null, commit);

            for (DiffEntry diff : diffs) {

                processDiff(repository, diff, javaFiles, maxLocAddedPerFile);


            }
        }
    }

    private static void processDiff(Repository repository, DiffEntry diff, List<FileJava> javaFiles, Map<String, Integer> maxLocAddedPerFile) throws IOException {
        String filePath = diff.getNewPath();
        if (filePath.endsWith(JAVA_FILE_EXTENSION)) {
            int addedLines = calculateAddedLines(repository, diff);
            updateFileLocAdded(javaFiles, filePath, addedLines, maxLocAddedPerFile);
        }
    }

    private static int calculateAddedLines(Repository repository, DiffEntry diff) throws IOException {
        int addedLines = 0;
        ObjectId objectId = diff.getNewId().toObjectId();
        ObjectLoader loader = repository.open(objectId);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(loader.openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    addedLines++;
                }
            }
        }
        return addedLines;
    }

    private static void updateFileLocAdded(List<FileJava> javaFiles, String filePath, int addedLines, Map<String, Integer> maxLocAddedPerFile) {
        for (FileJava javaFile : javaFiles) {
            if (javaFile.getName().equals(filePath)) {
                javaFile.setLocAdded(javaFile.getLocAdded() + addedLines);
                maxLocAddedPerFile.put(filePath, Math.max(maxLocAddedPerFile.getOrDefault(filePath, 0), addedLines));
            }
        }
    }

    public static void calculateAvgAddedLOC(List<Release> releases) {
        logger.log(java.util.logging.Level.INFO, "\u001B[37mCalculating average added LOC...\u001B[0m");
        for (Release release : releases) {
            for (FileJava javaFile : release.getFiles()) {
                int totalAddedLOC = javaFile.getLocAdded(); //LOC aggiunte totali
                int numberOfRevisions = javaFile.getNr(); //numero di revisioni

                if (numberOfRevisions > 0) {
                    double avgAddedLOC = (double) totalAddedLOC / numberOfRevisions;
                    javaFile.setAvgLocAdded(avgAddedLOC);
                } else {
                    javaFile.setAvgLocAdded(0); //se non ci sono revisioni
                }
            }
        }
    }


    public static void calculateNumberOfAuthorsPerFile(List<Release> releases, String repoPath) {
        logger.log(java.util.logging.Level.INFO, "\u001B[37mCalculating number of authors per file...\u001B[0m");
        try (Repository repository = Git.open(new File(repoPath)).getRepository()) {
                for (Release release : releases) {
                    //la mappa traccia gli autori del file java (per evitare duplicati)
                    Map<String, Set<String>> fileAuthors = new HashMap<>();
                    processCommitAuthors(repository, release, fileAuthors);
                    updateReleaseFiles(release, fileAuthors);

                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processCommitAuthors(Repository repository, Release release, Map<String, Set<String>> fileAuthors) {
        for (RevCommit commit : release.getCommits()) {
            String author = commit.getAuthorIdent().getName();
            List<DiffEntry> diffs = getDiffEntriesAuthors(repository, commit);
            for (DiffEntry diff : diffs) {
                String filePath = diff.getNewPath();
                if (filePath.endsWith(JAVA_FILE_EXTENSION)) {
                    fileAuthors.computeIfAbsent(filePath, k -> new HashSet<>()).add(author);
                }
            }
        }
    }

    // Ottieni la lista di DiffEntry per un commit, considerando i genitori o il commit stesso
    private static List<DiffEntry> getDiffEntriesAuthors(Repository repository, RevCommit commit) {
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(repository);
            if (commit.getParentCount() > 0) {
                return diffFormatter.scan(commit.getParent(0), commit);
            } else {
                return diffFormatter.scan(null, commit);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList(); // Ritorna una lista vuota in caso di errore
        }
    }

    // Aggiorna i file della release con il numero di autori
    private static void updateReleaseFiles(Release release, Map<String, Set<String>> fileAuthors) {
        for (Map.Entry<String, Set<String>> entry : fileAuthors.entrySet()) {
            String fileName = entry.getKey();
            int numberOfAuthors = entry.getValue().size();
            FileJava javaFile = release.getJavaFileByName(fileName);
            if (javaFile == null) {
                javaFile = new FileJava(fileName);
                release.addFile(javaFile);
            }
            javaFile.setNauth(numberOfAuthors);
        }
    }






}