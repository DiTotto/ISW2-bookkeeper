package models;

import weka.classifiers.Classifier;
import weka.core.Instances;

import java.util.List;

public class FeatureSelectionConfig {
    private String nameProj;
    private int walkIteration;
    private Instances trainingData;
    private Instances testingData;
    private List<ClassifierMetrics> metricOfClassifierList;
    private Classifier[] classifiers;
    private boolean isUnderSampling;
    private boolean isOverSampling;

    public FeatureSelectionConfig(String nameProj, int walkIteration, Instances trainingData, Instances testingData, List<ClassifierMetrics> metricOfClassifierList, Classifier[] classifiers, boolean isUnderSampling, boolean isOverSampling) {
        this.nameProj = nameProj;
        this.walkIteration = walkIteration;
        this.trainingData = trainingData;
        this.testingData = testingData;
        this.metricOfClassifierList = metricOfClassifierList;
        this.classifiers = classifiers;
        this.isUnderSampling = isUnderSampling;
        this.isOverSampling = isOverSampling;
    }

    public String getNameProj() {
        return nameProj;
    }

    public void setNameProj(String nameProj) {
        this.nameProj = nameProj;
    }

    public int getWalkIteration() {
        return walkIteration;
    }

    public void setWalkIteration(int walkIteration) {
        this.walkIteration = walkIteration;
    }

    public Instances getTrainingData() {
        return trainingData;
    }

    public void setTrainingData(Instances trainingData) {
        this.trainingData = trainingData;
    }

    public Instances getTestingData() {
        return testingData;
    }

    public void setTestingData(Instances testingData) {
        this.testingData = testingData;
    }

    public List<ClassifierMetrics> getMetricOfClassifierList() {
        return metricOfClassifierList;
    }

    public void setMetricOfClassifierList(List<ClassifierMetrics> metricOfClassifierList) {
        this.metricOfClassifierList = metricOfClassifierList;
    }

    public Classifier[] getClassifiers() {
        return classifiers;
    }

    public void setClassifiers(Classifier[] classifiers) {
        this.classifiers = classifiers;
    }

    public boolean isUnderSampling() {
        return isUnderSampling;
    }

    public void setUnderSampling(boolean underSampling) {
        isUnderSampling = underSampling;
    }

    public boolean isOverSampling() {
        return isOverSampling;
    }

    public void setOverSampling(boolean overSampling) {
        isOverSampling = overSampling;
    }
}
