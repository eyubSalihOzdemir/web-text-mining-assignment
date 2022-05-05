package com.textmining.main;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Helper {
    // tells if string is numeric value or not
    public static boolean isNumeric(String strNum) {
        final Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        if (strNum == null) {
            return false;
        }
        return pattern.matcher(strNum).matches();
    }

    // trims the string and makes it lower case
    public static String trimAndLowerCase(String text) {
        text = text.replace("\r\n", " ")
                .replace("\n", " ")
                .trim()
                .toLowerCase();
        return text;
    }

    // converts List<Double> to List<String>
    public static List<String> convertToStringList(List<Double> doubleList) {
        List<String> stringList = new ArrayList<>();
        for (Double d : doubleList) {
            stringList.add(d.toString());
        }
        return stringList;
    }

    // increases the given index of the List<Integer> by 1
    public static void increaseListIndexByOne(int index, List<Integer> list) {
        list.set(index, list.get(index) + 1);
    }

    // calculates the average values of double values in a list
    public static double getAverageofDoubleList(List<Double> doubleList) {
        double sum = 0;
        for (var value : doubleList) {
            sum += value;
        }

        return sum / (doubleList.size() * 1.0);
    }

    // fill the category nested list with zeros (to be used as accuracy matrix)
    public static void fillAccuracyParameters(List<List<Integer>> categories) {
        for (int i = 0; i < 4; i++) {
            List<Integer> accuracyParameters = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                accuracyParameters.add(0);
            }
            categories.add(accuracyParameters);
        }
    }
}
