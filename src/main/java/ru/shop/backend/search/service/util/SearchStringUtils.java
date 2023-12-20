package ru.shop.backend.search.service.util;

import java.util.regex.Pattern;

import static ru.shop.backend.search.service.SearchConstants.enLayout;
import static ru.shop.backend.search.service.SearchConstants.ruLayout;

public class SearchStringUtils {

    private static final Pattern pattern = Pattern.compile("\\d+");

    public static boolean isNumeric(String strNum) {
        return strNum != null
                && pattern.matcher(strNum).matches();
    }

    public static String convert(String message) {
        boolean isCyrillic = message.matches(".*\\p{InCyrillic}.*");

        StringBuilder builder = new StringBuilder();

        char[] sourceArray = isCyrillic ? ruLayout : enLayout;
        char[] targetArray = isCyrillic ? enLayout : ruLayout;

        for (int i = 0; i < message.length(); i++) {
            for (int j = 0; j < sourceArray.length; j++) {
                if (message.charAt(i) == sourceArray[j]) {
                    builder.append(targetArray[j]);
                }
            }
        }

        return builder.toString();
    }

}
