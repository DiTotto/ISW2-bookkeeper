package models;

public class FileJava {

    private String name;
    private int loc;
    private int locTouched;
    private int nr;
    private int locAdded;
    private double avgLocAdded;
    private int nauth;
    private int maxLocAdded;

    private int totalLocRemoved;

    private int maxLocRemoved;

    private int avgLocTouched;

    private boolean isBuggy;

    public FileJava(String name) {
        this.name = name;
        this.loc = 0;
        this.locTouched = 0;
        this.nr = 0;
        this.locAdded = 0;
        this.avgLocAdded = 0;
        this.nauth = 0;
        this.maxLocAdded = 0;
        this.totalLocRemoved = 0;
        this.maxLocRemoved = 0;
        this.avgLocTouched = 0;
        this.isBuggy = false;


    }

    /* -------GETTER---------- */
    public String getName() {
        return name;
    }

    public int getLoc() {
        return loc;
    }

    public int getLocTouched() {
        return locTouched;
    }

    public int getNr() {
        return nr;
    }

    public int getLocAdded() {
        return locAdded;
    }

    public double getAvgLocAdded() {
        return avgLocAdded;
    }

    public int getNauth() {
        return nauth;
    }

    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    public int getTotalLocRemoved() {
        return totalLocRemoved;
    }

    public int getMaxLocRemoved() {
        return maxLocRemoved;
    }

    public int getAvgLocTouched() {
        return avgLocTouched;
    }

    public boolean isBuggy() {
        return isBuggy;
    }


    /* ----------------- */

    /* -------SETTER---------- */

    public void setName(String name) {
        this.name = name;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public void setLocTouched(int locTouched) {
        this.locTouched = locTouched;
    }

    public void setNr(int nr) {
        this.nr = nr;
    }

    public void setLocAdded(int locAdded) {
        this.locAdded = locAdded;
    }

    public void setAvgLocAdded(double avgLocAdded) {
        this.avgLocAdded = avgLocAdded;
    }

    public void setNauth(int nauth) {
        this.nauth = nauth;
    }

    public void setMaxLocAdded(int maxLocAdded) {
        this.maxLocAdded = maxLocAdded;
    }

    public void setTotalLocRemoved(int totalLocRemoved) {
        this.totalLocRemoved = totalLocRemoved;
    }

    public void setMaxLocRemoved(int maxLocRemoved) {
        this.maxLocRemoved = maxLocRemoved;
    }

    public void setAvgLocTouched(int avgLocTouched) {
        this.avgLocTouched = avgLocTouched;
    }

    public void setBuggy(boolean buggy) {
        isBuggy = buggy;
    }



    /* ----------------- */

    @Override
    public String toString() {
        return "File{" +
                "name='" + name + '\'' +
                ", loc='" + loc + '\'' +
                '}';
    }
}