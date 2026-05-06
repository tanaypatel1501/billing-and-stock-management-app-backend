package com.gst.billingandstockmanagement.utils;

import org.springframework.stereotype.Component;

@Component
public class NumberToWordsConverter {

    private static final String[] ones = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"
    };

    private static final String[] teens = {
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] tens = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public String convert(double value) {
        long number = Math.round(value * 100);
        if (number == 0) return "Zero";

        long integerPart = number / 100;
        int decimalPart = (int) (number % 100);

        String result = "";

        int crore = (int) (integerPart / 10000000);
        integerPart %= 10000000;

        int lakh = (int) (integerPart / 100000);
        integerPart %= 100000;

        int thousand = (int) (integerPart / 1000);
        integerPart %= 1000;

        int hundreds = (int) integerPart;

        if (crore > 0) result += convertHundreds(crore) + " Crore ";
        if (lakh > 0) result += convertHundreds(lakh) + " Lakh ";
        if (thousand > 0) result += convertHundreds(thousand) + " Thousand ";
        if (hundreds > 0) result += convertHundreds(hundreds);

        String finalString = result.trim() + " Rupees";

        if (decimalPart > 0) {
            finalString += " and " + convertHundreds(decimalPart) + " Paise";
        }

        return finalString + " Only";
    }

    private String convertHundreds(int n) {
        String res = "";
        if (n >= 100) {
            res += ones[n / 100] + " Hundred ";
            n %= 100;
        }
        if (n >= 10 && n <= 19) {
            res += teens[n - 10];
        } else {
            if (n >= 20) {
                res += tens[n / 10] + " ";
                n %= 10;
            }
            if (n > 0) {
                res += ones[n];
            }
        }
        return res.trim();
    }
}