package com.mybraintech.sdk.util;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class MatrixUtils2 {

    @NonNull
    public static ArrayList<ArrayList<Float>> invertFloatMatrix(@NonNull ArrayList<ArrayList<Float>> source){
        if (source.size() == 0 )
            return new ArrayList<>();

        int nbLinesDestination = source.get(0).size();
        int nbColumnsDestination = source.size();

        ArrayList<ArrayList<Float>> destination = new ArrayList<>();

        for (int nbLine = 0 ; nbLine < nbLinesDestination; nbLine++){ //fill the lines of the destination matrix
            destination.add(new ArrayList<Float>());
            for (int nbColumn = 0 ; nbColumn < nbColumnsDestination; nbColumn++){ //fill the columns of the destination matrix
                destination.get(nbLine).add(source.get(nbColumn).get(nbLine)); //add the column elements for each line
            }
        }
        return destination;
    }

}
