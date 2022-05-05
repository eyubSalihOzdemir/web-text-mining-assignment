package com.textmining.main;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

public class Main {

    private static final String TFIDF_CSV_FILE_PATH = "C:\\Users\\Public\\Documents\\tfidf.csv";
    private static final String ACCURACY_CSV_PATH = "C:\\Users\\Public\\Documents\\accuracy.csv";

    public static void main(String[] args) {

        // initialized list to keep track of word counts of each document
        List<HashMap<String, Integer>> documents = new ArrayList<>();
        List<HashMap<String, Integer>> documentsTest = new ArrayList<>();

        // initialized list to keep track of total word count
        HashMap<String, Integer> allWordCounts = new HashMap<>();

        // initialized lists to keep track of word counts of each individual category
        HashMap<String, Integer> economyWordCounts = new HashMap<>();
        HashMap<String, Integer> sportsWordCounts = new HashMap<>();
        HashMap<String, Integer> magazineWordCounts = new HashMap<>();
        HashMap<String, Integer> healthWordCounts = new HashMap<>();

        try {
            // read stop words into memory
            List<String> stopWords = IO.readStopWords();

            // raad the train documents, tokenize the words and fill the necessary lists for calculations
            IO.readAndTokenizeArticles(stopWords, "train",
                    documents,
                    allWordCounts,
                    economyWordCounts,
                    sportsWordCounts,
                    magazineWordCounts,
                    healthWordCounts);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // delete the words that have occurrences less than 6 and more than 2k
        featureReduction(6, 2000, allWordCounts);

        // for each word, count how many documents it occurs in
        HashMap<String, Integer> allWordsDocumentCounts = new HashMap<>();
        for (var token : allWordCounts.keySet()) {
            for (var doc : documents) {
                if (doc.containsKey(token)) {
                    IO.putOrIncreaseByOne(allWordsDocumentCounts, token);
                }
            }
        }

        // read the test documents
        // there are some redundant operations for reading the test documents here,
        // but instead of making radical changes in the code, I decided to leave it this way.
        try {
            List<String> stopWords = IO.readStopWords();
            IO.readAndTokenizeArticles(stopWords, "test",
                    documentsTest,
                    allWordCounts,      // redundant
                    economyWordCounts,  // redundant
                    sportsWordCounts,   // redundant
                    magazineWordCounts, // redundant
                    healthWordCounts);  // redundant

        } catch (IOException e) {
            e.printStackTrace();
        }

        // create the csv file to write tf-idf values
        File csvFile = new File(TFIDF_CSV_FILE_PATH);
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "ISO-8859-9"));

            // calculate and write tf-idf values for each token and document
            writeTFIDF(bufferedWriter, "", allWordCounts, documents, allWordsDocumentCounts);
            writeTFIDF(bufferedWriter,"Test", allWordCounts, documentsTest, allWordsDocumentCounts);

            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // this part is to generally calculate and write accuracy parameters
        int documentCounter = 0;

        List<List<Integer>> categories = new ArrayList<>();
        // "categories" list is for accuracy matrix:
        // [[TP(economy), FP(economy), FN(economy)],
        // [TP(magazine), FP(magazine), FN(magazine)],
        // [TP(health), FP(health), FN(health)],
        // [TP(sports), FP(sports), FN(sports)]]

        // [[0,0,0], [0,0,0], [0,0,0], [0,0,0]]
        Helper.fillAccuracyParameters(categories);

        // for each test document
        for (var doc : documentsTest) {

            // get the probability of document being in a certain category
            BigDecimal economyProb = getProbability(doc, economyWordCounts, allWordCounts);
            BigDecimal healthProb = getProbability(doc, healthWordCounts, allWordCounts);
            BigDecimal magazineProb = getProbability(doc, magazineWordCounts, allWordCounts);
            BigDecimal sportsProb = getProbability(doc, sportsWordCounts, allWordCounts);

            // get the biggest probability of the 4 categories
            BigDecimal returned = biggestOfBD(economyProb, healthProb, magazineProb, sportsProb);

            // decide which category it predicted as
            if(returned.compareTo(economyProb) == 0) {
                //System.out.print("economy ");

                // increase the TP, FP, FN values accordingly
                incrementAccuracyParameters(0, documentCounter, categories.get(0), categories);
            } else if (returned.compareTo(magazineProb) == 0) {
                //System.out.print("magazine ");
                incrementAccuracyParameters(1, documentCounter, categories.get(1), categories);
            } else if (returned.compareTo(healthProb) == 0) {
                //System.out.print("health ");
                incrementAccuracyParameters(2, documentCounter, categories.get(2), categories);
            } else {
                //System.out.print("sports ");
                incrementAccuracyParameters(3, documentCounter, categories.get(3), categories);
            }

            // document is used to determine which category the read document really belongs to
            // since they are sorted, we know that first 80 document is belongs to economy category,
            // documents 81 to 160 belongs to magazine category and so on...
            documentCounter++;
        }

        // write the csv file for accuracy parameters
        File accuracyParametersFile = new File(ACCURACY_CSV_PATH);
        writeAccuracyParameters(accuracyParametersFile, categories);
    }

    private static void writeAccuracyParameters(File accuracyParametersFile, List<List<Integer>> categories) {
        // initializations to make the writing easier
        HashMap<Integer, String> names = new HashMap<>();
        names.put(0, "economy");
        names.put(1, "magazine");
        names.put(2, "health");
        names.put(3, "sports");
        List<Double> precisionList = new ArrayList<>();
        List<Double> recallList = new ArrayList<>();
        List<Double> fScoreList = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            List<Integer> category = categories.get(i);
            double tp = category.get(0); // [(TP), FP,  FN ]
            double fp = category.get(1); // [ TP, (FP), FN ]
            double fn = category.get(2); // [ TP,  FP, (FN)]

            double precision = tp / (tp + fp);
            double recall = tp / (tp + fn);
            double fScore = (2*precision*recall) / (precision + recall);
            precisionList.add(precision);
            recallList.add(recall);
            fScoreList.add(fScore);
        }

        // add average values to the end of the lists
        precisionList.add(Helper.getAverageofDoubleList(precisionList));
        recallList.add(Helper.getAverageofDoubleList(recallList));
        fScoreList.add(Helper.getAverageofDoubleList(fScoreList));

        // write them to the file
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(accuracyParametersFile), "ISO-8859-9"));

            // first, convert List<Dobule> to List<String>
            List<String> precisionString = Helper.convertToStringList(precisionList);
            List<String> recallString = Helper.convertToStringList(recallList);
            List<String> fScoreString = Helper.convertToStringList(fScoreList);

            // add headers
            List<String> header = new ArrayList<>(List.of("", "Economy", "Magazine", "Health", "Sports", "Average"));
            precisionString.add(0, "Precision");
            recallString.add(0, "Recall");
            fScoreString.add(0, "F-Score");

            // and than write them
            bufferedWriter.append(String.join(",", header));
            bufferedWriter.append("\n");
            bufferedWriter.append(String.join(",", precisionString));
            bufferedWriter.append("\n");
            bufferedWriter.append(String.join(",", recallString));
            bufferedWriter.append("\n");
            bufferedWriter.append(String.join(",", fScoreString));

            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void incrementAccuracyParameters(int prediction, int documentCount, List<Integer> categoryAccuracyParameters, List<List<Integer>> categories) {
        // as I've explained before, since we have 80 test documents from each category and they are sorted
        // we can keep track of the real category of the document by dividing document count by 80
        // if it's 0, category is economy
        // if it's 1, category is magazine and so on...

        // since I know these numbers, I make my prediction according to this.
        // if my prediction is category of "economy", I label it as "0",
        // if my prediction is category of "sports", I label it as "3",
        // so that I can easily compare my prediction and it's real value

        if(prediction == documentCount/80) {
            // if my prediction and document's category is the same: it's TP
            // increase the value at the index of 0
            Helper.increaseListIndexByOne(0, categoryAccuracyParameters);
        } else {
            // if my prediction is the given category, but document does not belong to this category: it's FP
            // increase the value at the index of 1
            Helper.increaseListIndexByOne(1, categoryAccuracyParameters);

            // and if neither my prediction nor the real category of the document is true, it's FN ( for the category at the document/80'th index)
            Helper.increaseListIndexByOne(2, categories.get(documentCount/80));
        }

        // I hope this structure is not too messed up and you can understand it.
        // I believe it has a decent logic behind it and it works without a problem.
    }

    // loop through the values of hashmap, and make the necessary removings for given min and max values
    private static void featureReduction(int minOccurrence, int maxOccurrence, HashMap<String, Integer> allWordCounts) {
        List<String> toBeRemoved = new ArrayList<>();
        for (var key : allWordCounts.keySet()) {
            int occurrence = allWordCounts.get(key);
            if(occurrence <= minOccurrence || occurrence >= maxOccurrence) {
                toBeRemoved.add(key);
            }
        }
        for (var entry : toBeRemoved) {
            allWordCounts.remove(entry);
        }
    }

    // write the tf-idf values as csv to the file
    private static void writeTFIDF (BufferedWriter writer, String columnString, HashMap<String, Integer> allWordCounts, List<HashMap<String, Integer>> documents, HashMap<String, Integer> allWordsDocumentCounts) {
        try {
            List<String> tokens = new ArrayList<>(allWordCounts.keySet());
            tokens.add(0, " "); // add 1 column at the beginning to fix the alignment in xls

            int documentCountMod = 80; // because we have 80 test document of each category

            // if it's writing tf-idf values of "train" data set
            if(columnString.equals("")) {
                // write words/tokens as headers
                writer.append(String.join(",", tokens));
                writer.append("\n");

                // set the document count mod to 150
                // because we have 150 train document of each category
                documentCountMod = 150;
            }

            int documentCounter = 0;
            // write the tfidf values
            for (var docHash : documents) {
                documentCounter++;

                // calculate the tf-idf value of the token for the current document
                List<Double> doubleValuesTFIDF = calculateTFIDF(allWordCounts, docHash, documents.size(), allWordsDocumentCounts);

                // convert the double list to string list
                List<String> strings = Helper.convertToStringList(doubleValuesTFIDF);

                // put the document name to the beginning of the line
                strings.add(0, columnString + documentCounter + ".txt");

                // put the name of the category at the end of the line
                addCategoryNameToList(strings, documentCounter, documentCountMod);

                // write the string list of tf-idf values to file
                writer.append(String.join(",", strings));
                writer.append("\n");
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    // add the category according the document count (explained before)
    private static void addCategoryNameToList(List<String> list, int counter, int mod) {
        switch (counter / mod) {
            case 0:
                list.add(list.size(), "economy");
                break;
            case 1:
                list.add(list.size(), "magazine");
                break;
            case 2:
                list.add(list.size(), "health");
                break;
            case 3:
            case 4:
                list.add(list.size(), "sports");
                break;
            default:
                list.add(list.size(), "error");
        }
    }

    // calculate the tf-idf value of the each word for the given document
    private static List<Double> calculateTFIDF (HashMap<String, Integer> allWordCounts, HashMap<String, Integer> docHash, double documentsSize, HashMap<String, Integer> allWordsDocumentCounts) {
        double tf, freq, maxOthers;
        double idf, docN, appearedDocs = 0;

        List<Double> doubleValuesTFIDF = new ArrayList<>();
        for (var token : allWordCounts.keySet()) {
            // if document has token: calculate tf-idf, write "0" otherwise
            if (docHash.containsKey(token)) {
                freq = docHash.get(token);
                maxOthers=(Collections.max(docHash.values()));
                tf = freq / maxOthers;

                docN = documentsSize;
                appearedDocs = allWordsDocumentCounts.get(token);
                idf = Math.log10(docN / appearedDocs);

                // add it to the list, will be converted to string later on
                doubleValuesTFIDF.add(tf*idf);
            } else {
                doubleValuesTFIDF.add(0.0);
            }
        }

        return doubleValuesTFIDF;
    }

    // compare the given 4 BigDecimals and return the biggest one
    private static BigDecimal biggestOfBD(BigDecimal economy, BigDecimal health, BigDecimal magazine, BigDecimal sports) {
        BigDecimal maxFirst = economy.max(health);
        BigDecimal maxSecond = magazine.max(sports);

        return maxFirst.max(maxSecond);
    }

    // calculate the final probability of word (Multinomial Naive Bayes)
    private static BigDecimal getProbability(HashMap<String, Integer> doc, HashMap<String, Integer> categoryWordCounts, HashMap<String, Integer> allWordCounts) {
        // since we have 150 of each 4 category
        double categoryProbability = 0.25;

        BigDecimal multipliedProbsOfWords = new BigDecimal(categoryProbability).setScale(5, RoundingMode.HALF_UP);
        for (var token : doc.keySet()) {
            // (1)* number of occurrences of token in all documents from the category + 1
            // divided by
            // (2)* all the words in every document from a category + total number of unique words
            double numberOfOccurrences = 0;

            // if the word occurs in the category
            if(categoryWordCounts.get(token) != null) {
                // add it's value
                numberOfOccurrences += categoryWordCounts.get(token);
            }
            numberOfOccurrences += 1; // (1)


            double allWordsFromCategory = IO.valuesSum(categoryWordCounts);
            allWordsFromCategory += allWordCounts.size(); // (2)

            MathContext mc = new MathContext(5);
            BigDecimal bd = new BigDecimal(numberOfOccurrences/allWordsFromCategory);

            // multiply the probability of each word and category
            multipliedProbsOfWords = multipliedProbsOfWords.multiply(bd, mc);
        }

        return multipliedProbsOfWords;
    }

}
