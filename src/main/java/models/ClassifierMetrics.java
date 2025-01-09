package models;

public class ClassifierMetrics {

    private String nameProject;
    private String classifier;
    //index dell'iterazione della walk forward
    private int iteration;
    private boolean featureSelection;
    private boolean sampling;
    private boolean costSensitive;
    private double precision;
    private double recall;
    private double auc;
    private double kappa;
    private double tp;
    private double fp;
    private double npofb;
    private double tn;
    private double fn;
    private String samplingType;

    private double percentOfTheTraining;

    public ClassifierMetrics(String nameProject, int iteration, String classifier, boolean featureSelection, boolean sampling, boolean costSensitive) {
        this.nameProject = nameProject;
        this.iteration = iteration;
        this.classifier = classifier;
        this.featureSelection = featureSelection;
        this.sampling = sampling;
        this.costSensitive = costSensitive;
        this.precision = 0;
        this.recall = 0;
        this.auc = 0;
        this.kappa = 0;
        this.tp = 0;
        this.fp = 0;
        this.npofb = 0;
        this.tn = 0;
        this.fn = 0;
        this.percentOfTheTraining = 0;
        this.samplingType = "NO_SAMPLING";

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

    public boolean isFeatureSelection() {
        return featureSelection;
    }

    public boolean isSampling() {
        return sampling;
    }

    public boolean isCostSensitive() {
        return costSensitive;
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
    public double getNpofb() { return npofb; }

    public double getTn() {
        return tn;
    }

    public double getFn() {
        return fn;
    }


    public double getPercentageOfTraining() {
        return percentOfTheTraining;
    }

    public String getSamplingType() {
        return samplingType;
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

    public void setFeatureSelection(boolean featureSelection) {
        this.featureSelection = featureSelection;
    }

    public void setSampling(boolean sampling) {
        this.sampling = sampling;
    }

    public void setCostSensitive(boolean costSensitive) {
        this.costSensitive = costSensitive;
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

    public void setNpofb(double npofb) { this.npofb = npofb; }

    public void setTn(double tn) {
        this.tn = tn;
    }

    public void setFn(double fn) {
        this.fn = fn;
    }

    public void setSamplingType(String samplingType) {
        this.samplingType = samplingType;
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
                ", feature_selection=" + featureSelection +
                ", sampling=" + sampling +
                ", cost_sensitive=" + costSensitive +
                ", precision=" + precision +
                ", recall=" + recall +
                ", auc=" + auc +
                ", kappa=" + kappa +
                ", tp=" + tp +
                ", fp=" + fp +
                ", npofb=" + npofb +
                ", tn=" + tn +
                ", fn=" + fn +
                ", percentOfTheTraining=" + percentOfTheTraining +
                '}';
    }


}