package acumeController;

import models.AcumeModel;
import weka.classifiers.AbstractClassifier;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;

public class AcumeController {

    private AcumeController() {
        throw new IllegalStateException("AcumeController class");
    }

    public static double retrieveNpofb(Instances data, AbstractClassifier classifier) {
        //Npofb stands for Number of Positive Out of False Positives

        double npofb = 0.0;
        List<AcumeModel> AcumeModelList = new ArrayList<>();
        return 0.0;
    }
}
