package com.shyeuar.baity.utils;

import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RomanNumeralUtils {
    private static final Pattern NUMERAL_FINDING_PATTERN = Pattern.compile(
            " (?=[MDCLXVI])(?<roman>M*(?:C[MD]|D?C{0,3})(?:X[CL]|L?X{0,3})(?:I[XV]|V?I{0,3}))(?<after>(?: ✖|.)?)"
    );
    private static final Pattern NUMERAL_VALIDATION_PATTERN = Pattern.compile(
            "^(?=[MDCLXVI])M*(C[MD]|D?C{0,3})(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})$"
    );
    private static final Pattern WORD_PART_PATTERN = Pattern.compile("^[\\w-']");
    private static final TreeMap<Integer, String> INT_ROMAN_MAP = new TreeMap<>(Map.ofEntries(
            Map.entry(1000, "M"),
            Map.entry(900, "CM"),
            Map.entry(500, "D"),
            Map.entry(400, "CD"),
            Map.entry(100, "C"),
            Map.entry(90, "XC"),
            Map.entry(50, "L"),
            Map.entry(40, "XL"),
            Map.entry(10, "X"),
            Map.entry(9, "IX"),
            Map.entry(5, "V"),
            Map.entry(4, "IV"),
            Map.entry(1, "I")
    ));

    private RomanNumeralUtils() {
    }

    public static Component replaceNumeralsWithIntegers(Component inputComponent) {
        String inputString = inputComponent.getString();
        Matcher matcher = NUMERAL_FINDING_PATTERN.matcher(inputString);
        Component result = inputComponent;
        boolean modified = false;

        while (matcher.find()) {
            String roman = matcher.group("roman");
            String after = matcher.group("after");

            if (WORD_PART_PATTERN.matcher(after).matches() || roman.isEmpty()) {
                continue;
            }

            if (!isNumeralValid(roman)) {
                continue;
            }

            int parsedInteger = parseNumeral(roman);

            if (parsedInteger != 1 || after.equals("§") || after.isEmpty() || after.equals(" ✖")) {
                result = ComponentTextUtils.replaceComponent(result, " " + roman, " " + parsedInteger);
                modified = true;
            }
        }

        return modified ? result : inputComponent;
    }

    public static boolean isNumeralValid(String romanNumeral) {
        return romanNumeral != null && NUMERAL_VALIDATION_PATTERN.matcher(romanNumeral).matches();
    }

    public static int parseNumeral(String numeral) {
        if (numeral == null || numeral.isEmpty()) {
            return 0;
        }
        int total = 0;
        int prev = 0;
        for (int i = numeral.length() - 1; i >= 0; i--) {
            int val = romanValue(numeral.charAt(i));
            if (val < prev) {
                total -= val;
            } else {
                total += val;
            }
            prev = val;
        }
        return total;
    }

    public static String integerToRoman(int number) {
        StringBuilder result = new StringBuilder();
        while (number > 0) {
            var entry = INT_ROMAN_MAP.floorEntry(number);
            result.append(entry.getValue());
            number -= entry.getKey();
        }
        return result.toString();
    }

    private static int romanValue(char c) {
        return switch (c) {
            case 'I' -> 1;
            case 'V' -> 5;
            case 'X' -> 10;
            case 'L' -> 50;
            case 'C' -> 100;
            case 'D' -> 500;
            case 'M' -> 1000;
            default -> 0;
        };
    }
}
