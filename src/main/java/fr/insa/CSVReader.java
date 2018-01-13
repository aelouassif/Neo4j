package fr.insa;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVReader {
    private String csvFile;
    private int y;
    private int x;

    public CSVReader(String csvFile,int x,int y) {
        this.csvFile = csvFile;
        this.x = x;
        this.y = y;
    }

    public Double[][] getData() {
        String line = "";
        String cvsSplitBy = ",";
        Double[][] mat = new Double[this.y][this.x];

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            int i=0;
            while ((line = br.readLine()) != null) {
                String[] ligne = line.split(cvsSplitBy);
                for (int j = 0; j<ligne.length; j++)
                    mat[i][j] = Double.valueOf(ligne[j]);
                i++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return mat;
    }
//    public static void main(final String[] args) {
//    	CSVReader reader = new CSVReader("data/probability-5.csv",1800,900);
//    	Double[][] mat = reader.getData();
//    	for(int i=0;i<900 ;i++) {
//    		for (int j = 0; j < 1800 ; j++) {
//    			System.out.print(mat[i][j]+" ");
//			}
//    		System.out.println();
//    	}
//    }
}
