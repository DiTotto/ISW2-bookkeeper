package models;

public class AcumeModel {
    private int id;
    private double size;
    private double probability;
    private String value;

    public AcumeModel(int id, double size, double probablity, String value) {
        this.id = id;
        this.size = size;
        this.probability = probablity;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public double getSize() {
        return size;
    }

    public double getProbability() {
        return probability;
    }

    public String getValue() {
        return value;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public void setProbability(double probablity) {
        this.probability = probablity;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
