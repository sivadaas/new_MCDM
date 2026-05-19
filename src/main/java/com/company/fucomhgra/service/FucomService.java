package com.company.fucomhgra.service;
//Brain of our project

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class FucomService {

    private static final int DECIMAL_PLACES= 2;

    private double round(double value){
        return BigDecimal.valueOf(value).setScale(DECIMAL_PLACES,RoundingMode.HALF_UP).doubleValue();
    }

    public Map<String,Double> computeWeights(
            List<String> priorityOrder,
            Map<String,Double> comparativeRatios
            //arguments for this method
    ){
        int n=priorityOrder.size();
        double[] unnormalized =new double[n];
        //starting from base
        unnormalized[n -1]=1.0;

        for(int i=n-2;i>=0;i--){
            String currentCriterion =priorityOrder.get(i);
            String nextCriterion    =priorityOrder.get(i+1);
            String key              =currentCriterion+"/"+nextCriterion;

            if(!comparativeRatios.containsKey(key)){
                throw new IllegalArgumentException(
                        "Missing comparative ratio for:" +key+
                                ".Expected format:'BOD/COD'"
                );
            }

            double phi=comparativeRatios.get(key);
            unnormalized[i]=round(unnormalized[i+1]*phi);
        }

        double sum=0.0;
        for(double w:unnormalized){
            sum+=w;
        }
        sum=round(sum);

        Map<String,Double> weights=new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            weights.put(priorityOrder.get(i), unnormalized[i] / sum);
        }
        return weights;
    }

    //rounds any number
}
