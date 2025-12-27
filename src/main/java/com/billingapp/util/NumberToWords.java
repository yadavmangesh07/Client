package com.billingapp.util;

//import java.text.DecimalFormat;

public class NumberToWords {

    private static final String[] units = {
            "", " One", " Two", " Three", " Four", " Five", " Six", " Seven", " Eight", " Nine", " Ten",
            " Eleven", " Twelve", " Thirteen", " Fourteen", " Fifteen", " Sixteen", " Seventeen", " Eighteen", " Nineteen"
    };

    private static final String[] tens = {
            "", "", " Twenty", " Thirty", " Forty", " Fifty", " Sixty", " Seventy", " Eighty", " Ninety"
    };

    public static String convert(double amount) {
        if (amount == 0) return "Zero Only";
        
        long n = (long) amount;
        String words = convertToIndianCurrency(n) + " Only";
        return words;
    }

    private static String convertToIndianCurrency(long n) {
        if (n < 0) return "Minus " + convertToIndianCurrency(-n);
        if (n < 20) return units[(int) n];
        if (n < 100) return tens[(int) n / 10] + ((n % 10 != 0) ? " " : "") + units[(int) n % 10];
        if (n < 1000) return units[(int) n / 100] + " Hundred" + ((n % 100 != 0) ? " " : "") + convertToIndianCurrency(n % 100);
        if (n < 100000) return convertToIndianCurrency(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " : "") + convertToIndianCurrency(n % 1000);
        if (n < 10000000) return convertToIndianCurrency(n / 100000) + " Lakh" + ((n % 100000 != 0) ? " " : "") + convertToIndianCurrency(n % 100000);
        return convertToIndianCurrency(n / 10000000) + " Crore" + ((n % 10000000 != 0) ? " " : "") + convertToIndianCurrency(n % 10000000);
    }
}