package fr.insa;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVReader {

    public static ArrayList<ArrayList<Double>> getData(String csvFile) {
        String line = "";
        String cvsSplitBy = ",";
        ArrayList<ArrayList<Double>> mat = new ArrayList<ArrayList<Double>>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

            while ((line = br.readLine()) != null) {

                String[] ligne = line.split(cvsSplitBy);
                ArrayList<Double> parsed = new ArrayList<Double>(ligne.length);
                for (int i = 0; i<ligne.length; i++)
                    parsed.add(Double.valueOf(ligne[i]));
                mat.add(parsed);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return mat;
    }

}