package com.textmining.main;

import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class IO {

    private static final String STOP_WORD_PATH = "C:\\Users\\Public\\Documents\\stop-words-turkish.txt";
    private static final String PATH_TO_TEXT_MINING_FILE = "C:\\Users\\Public\\Documents\\text-mining";

    // reads the stop word data set to memory
    public static List<String> readStopWords() throws IOException {
        List<String> stopWords = new ArrayList<>();
        File myFile = new File(STOP_WORD_PATH);

        String st;
        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(myFile), StandardCharsets.UTF_8));
        while ((st = br.readLine()) != null) {
            stopWords.add(st);
        }

        return stopWords;
    }

    // this is an important function.
    // you can decide which document type to read, train or test
    // reads all the documents line by line and converts them to tokens
    public static void readAndTokenizeArticles(List<String> stopWords, String trainOrTest,
                                               List<HashMap<String, Integer>> documents,
                                               HashMap<String, Integer> allWordCounts,
                                               HashMap<String, Integer> economyWordCounts,
                                               HashMap<String, Integer> sportsWordCounts,
                                               HashMap<String, Integer> magazineWordCounts,
                                               HashMap<String, Integer> healthWordCounts) throws IOException {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

        // paths for each category folder
        List<String> categoryPaths = Arrays.asList(
                String.format(PATH_TO_TEXT_MINING_FILE + "\\%s\\ekonomi", trainOrTest),
                String.format(PATH_TO_TEXT_MINING_FILE + "\\%s\\magazin", trainOrTest),
                String.format(PATH_TO_TEXT_MINING_FILE + "\\%s\\saglik", trainOrTest),
                String.format(PATH_TO_TEXT_MINING_FILE + "\\%s\\spor", trainOrTest));

        int i = -1;
        // for each category folder
        for (var path : categoryPaths) {
            File folder = new File(path);
            // get the list of files in the folder
            File[] listOfFiles = folder.listFiles();

            String st;
            // foreach file in the folders
            for (File file : listOfFiles != null ? listOfFiles : new File[0]) {
                if (file.isFile()) {
                    i++;

                    // template hashmap for document tokens
                    HashMap<String, Integer> documentHashTemplate = new HashMap<>();

                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO-8859-9"));

                    // read next line until end of file
                    while ((st = br.readLine()) != null) {

                        st = Helper.trimAndLowerCase(st);

                        // split the line not just by comma but by given regex
                        List<String> tokens = new ArrayList<>(Arrays.asList(st.split("[ \r\n\t!\"+$%()/:?'.,-]+")));
                        // remove empty entries
                        tokens.removeAll(Arrays.asList("", null));

                        // for each word/token in the list
                        for (var token : tokens) {
                            // check if it's numeric or not (i decided that i won't use numeric values as tokens)
                            if(!Helper.isNumeric(token)) {

                                // get zemberek analysis for token
                                WordAnalysis result = morphology.analyze(token);

                                // proceed only if it's a valid word
                                if(result.analysisCount() != 0) {

                                    // get the first stem of the word
                                    String stem = result.getAnalysisResults().get(0).getStem();

                                    // proceed if stemmed word is not a stop word
                                    if(!stopWords.contains(stem)) {
                                        putOrIncreaseByOne(documentHashTemplate, stem);

                                        // edit these lists if it's a train document:
                                        // allWordCounts, economyWordCounts, magazineWordCounts, healthWordCounts, sportsWordCounts
                                        if(trainOrTest.equals("train")) {
                                            putOrIncreaseByOne(allWordCounts, stem);

                                            if(i <= 149) {               // economy
                                                putOrIncreaseByOne(economyWordCounts, stem);
                                            } else if (i <= 299) {       // magazine
                                                putOrIncreaseByOne(magazineWordCounts, stem);
                                            } else if (i <= 449) {       // health
                                                putOrIncreaseByOne(healthWordCounts, stem);
                                            } else {                     // sports
                                                putOrIncreaseByOne(sportsWordCounts, stem);
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                    // add the template to "documents" list
                    documents.add(documentHashTemplate);
                }
            }
        }

    }

    // add the value to the hashmap, if it already exists: increase by 1
    public static void putOrIncreaseByOne(HashMap<String, Integer> hashMap, String text) {
        if(hashMap.containsKey(text)) {
            hashMap.put(text, hashMap.get(text) + 1);
        } else {
            hashMap.put(text, 1);
        }
    }

    // get the sum of all the values of a hashmap
    public static int valuesSum(HashMap<String, Integer> hashMap) {
        int sum = 0;
        for (var datum : hashMap.values()) {
            sum += datum;
        }

        return sum;
    }

}
