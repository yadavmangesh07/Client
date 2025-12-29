package com.billingapp.util;

public class NumberToWords {

    private static final String[] units = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] tens = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String convert(double n) {
        long num = Math.round(n);
        if (num == 0) {
            return "Zero Only";
        }
        return convertToIndianCurrency(num) + " Only";
    }

    private static String convertToIndianCurrency(long n) {
        if (n < 0) {
            return "Minus " + convertToIndianCurrency(-n);
        }
        
        if (n < 20) {
            return units[(int) n];
        }
        
        if (n < 100) {
            return tens[(int) n / 10] + ((n % 10 != 0) ? " " + units[(int) n % 10] : "");
        }
        
        if (n < 1000) {
            return units[(int) n / 100] + " Hundred" + ((n % 100 != 0) ? " " + convertToIndianCurrency(n % 100) : "");
        }
        
        if (n < 100000) { // Limit for Thousands (1 Lakh)
            return convertToIndianCurrency(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " + convertToIndianCurrency(n % 1000) : "");
        }
        
        if (n < 10000000) { // Limit for Lakhs (1 Crore)
            return convertToIndianCurrency(n / 100000) + " Lakh" + ((n % 100000 != 0) ? " " + convertToIndianCurrency(n % 100000) : "");
        }
        
        // Crores and above
        return convertToIndianCurrency(n / 10000000) + " Crore" + ((n % 10000000 != 0) ? " " + convertToIndianCurrency(n % 10000000) : "");
    }
}