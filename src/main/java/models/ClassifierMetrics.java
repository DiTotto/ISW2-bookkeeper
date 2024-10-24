package models;

public class ClassifierMetrics {

    private String nameProject;
    private String classifier;
    //index dell'iterazione della walk forward
    private int iteration;
    private boolean feature_selection;
    private boolean sampling;
    private boolean cost_sensitive;
    private double precision;
    private double recall;
    private double auc;
    private double kappa;
    private double tp;
    private double fp;
    private double tn;
    private double fn;

    private double percentOfTheTraining;

    public ClassifierMetrics(String nameProject, int iteration, String classifier, boolean feature_selection, boolean sampling, boolean cost_sensitive) {
        this.nameProject = nameProject;
        this.iteration = iteration;
        this.classifier = classifier;
        this.feature_selection = feature_selection;
        this.sampling = sampling;
        this.cost_sensitive = cost_sensitive;
        this.precision = 0;
        this.recall = 0;
        this.auc = 0;
        this.kappa = 0;
        this.tp = 0;
        this.fp = 0;
        this.tn = 0;
        this.fn = 0;
        this.percentOfTheTraining = 0;

    }

    /* -- GETTER -- */

    public String getNameProject() {
        return nameProject;
    }

    public String getClassifier() {
        return classifier;
    }

    public int getIteration() {
        return iteration;
    }

    public boolean isFeature_selection() {
        return feature_selection;
    }

    public boolean isSampling() {
        return sampling;
    }

    public boolean isCost_sensitive() {
        return cost_sensitive;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getAuc() {
        return auc;
    }

    public double getKappa() {
        return kappa;
    }

    public double getTp() {
        return tp;
    }

    public double getFp() {
        return fp;
    }

    public double getTn() {
        return tn;
    }

    public double getFn() {
        return fn;
    }

    public double getPercentOfTheTraining() {
        return percentOfTheTraining;
    }

    /* -- SETTER -- */

    public void setNameProject(String nameProject) {
        this.nameProject = nameProject;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public void setFeature_selection(boolean feature_selection) {
        this.feature_selection = feature_selection;
    }

    public void setSampling(boolean sampling) {
        this.sampling = sampling;
    }

    public void setCost_sensitive(boolean cost_sensitive) {
        this.cost_sensitive = cost_sensitive;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public void setAuc(double auc) {
        this.auc = auc;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }

    public void setTp(double tp) {
        this.tp = tp;
    }

    public void setFp(double fp) {
        this.fp = fp;
    }

    public void setTn(double tn) {
        this.tn = tn;
    }

    public void setFn(double fn) {
        this.fn = fn;
    }

    public void setPercentOfTheTraining(double percentOfTheTraining) {
        this.percentOfTheTraining = percentOfTheTraining;
    }

    @Override
    public String toString() {
        return "MetricOfClassifier{" +
                "nameProject='" + nameProject + '\'' +
                ", classifier='" + classifier + '\'' +
                ", iteration=" + iteration +
                ", feature_selection=" + feature_selection +
                ", sampling=" + sampling +
                ", cost_sensitive=" + cost_sensitive +
                ", precision=" + precision +
                ", recall=" + recall +
                ", auc=" + auc +
                ", kappa=" + kappa +
                ", tp=" + tp +
                ", fp=" + fp +
                ", tn=" + tn +
                ", fn=" + fn +
                ", percentOfTheTraining=" + percentOfTheTraining +
                '}';
    }
}