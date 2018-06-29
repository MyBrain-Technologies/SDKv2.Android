package utils;


import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import java.util.ArrayList;


/**
 * @author Sophie ZECRI on 13/06/2018
 */
@Keep
public class MatrixUtils {

    /**
     * Inverts the columns and lines of a matrix of Float
     *
     */

    public static ArrayList<ArrayList<Float>> invertFloatMatrix(@NonNull ArrayList<ArrayList<Float>> source){

        if (source.size() == 0 )
            return null;

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

    /**
     * Inverts the columns and lines of a matrix of Integer
     */
    @NonNull
    public static ArrayList<ArrayList<Integer>> invertIntegerMatrix(ArrayList<ArrayList<Integer>> source){

        int nbLinesDestination = source.get(0).size();
        int nbColumnsDestination = source.size();

        ArrayList<ArrayList<Integer>> destination = new ArrayList<>();
        for(int nbLine = 0 ; nbLine < nbLinesDestination; nbLine++){ //init the matrix
            destination.add(new ArrayList<Integer>());
        }

        for (int nbLine = 0 ; nbLine < nbLinesDestination; nbLine++){ //fill the lines of the destination matrix
            for (int nbColumn = 0 ; nbColumn < nbColumnsDestination; nbColumn++){ //fill the columns of the destination matrix
                destination.get(nbLine).add(source.get(nbColumn).get(nbLine)); //add the column elements for each line
            }
        }
        return destination;
    }
    /**
     * Inverts the columns and lines of a matrix of String
     */
    @NonNull
    public static ArrayList<ArrayList<String>> invertStringMatrix(ArrayList<ArrayList<String>> source){

        int nbLinesDestination = source.get(0).size();
        int nbColumnsDestination = source.size();

        ArrayList<ArrayList<String>> destination = new ArrayList<>();
        for(int nbLine = 0 ; nbLine < nbLinesDestination; nbLine++){ //init the matrix
            destination.add(new ArrayList<String>());
        }

        for (int nbLine = 0 ; nbLine < nbLinesDestination; nbLine++){ //fill the lines of the destination matrix
            for (int nbColumn = 0 ; nbColumn < nbColumnsDestination; nbColumn++){ //fill the columns of the destination matrix
                destination.get(nbLine).add(source.get(nbColumn).get(nbLine)); //add the column elements for each line
            }
        }
        return destination;
    }
}
