package com.seenu.bankingsystem.util;

public class TransactionClassifier {

    public static String classify(String description, String transactionType) {
        if (description == null) {
            description = "";
        }
        
        String desc = description.toLowerCase().trim();

        // 1. First check if it matches an INCOME category (usually salary, bonus, dividend, etc. or TRANSFER_CREDIT/DEPOSIT)
        if (desc.contains("salary") || desc.contains("bonus") || desc.contains("dividend") || desc.contains("refund") || desc.contains("cashback")) {
            return "INCOME";
        }

        // 2. Map other keywords to specific spending categories
        if (desc.contains("swiggy") || desc.contains("zomato") || desc.contains("starbucks") || 
            desc.contains("food") || desc.contains("restaurant") || desc.contains("mcdonald") || 
            desc.contains("cafe") || desc.contains("dining") || desc.contains("pizza") || desc.contains("burger")) {
            return "FOOD_DINING";
        }

        if (desc.contains("netflix") || desc.contains("prime") || desc.contains("spotify") || 
            desc.contains("ticket") || desc.contains("movie") || desc.contains("show") || 
            desc.contains("game") || desc.contains("gaming") || desc.contains("hotstar") || desc.contains("cinema")) {
            return "ENTERTAINMENT";
        }

        if (desc.contains("amazon") || desc.contains("flipkart") || desc.contains("myntra") || 
            desc.contains("shopping") || desc.contains("supermarket") || desc.contains("grocery") || 
            desc.contains("groceries") || desc.contains("store") || desc.contains("mall") || 
            desc.contains("clothing") || desc.contains("apparel")) {
            return "SHOPPING";
        }

        if (desc.contains("uber") || desc.contains("ola") || desc.contains("petrol") || 
            desc.contains("fuel") || desc.contains("flight") || desc.contains("train") || 
            desc.contains("metro") || desc.contains("travel") || desc.contains("cab") || 
            desc.contains("taxi") || desc.contains("bus")) {
            return "TRAVEL_TRANSPORT";
        }

        if (desc.contains("electricity") || desc.contains("rent") || desc.contains("gas") || 
            desc.contains("water") || desc.contains("bill") || desc.contains("recharge") || 
            desc.contains("broadband") || desc.contains("mobile bill") || desc.contains("wifi") || 
            desc.contains("telephone") || desc.contains("maintenance")) {
            return "UTILITIES_BILLS";
        }

        // 3. Fallbacks based on transaction type
        if ("DEPOSIT".equalsIgnoreCase(transactionType) || "TRANSFER_CREDIT".equalsIgnoreCase(transactionType)) {
            return "INCOME";
        }

        return "OTHERS";
    }
}
