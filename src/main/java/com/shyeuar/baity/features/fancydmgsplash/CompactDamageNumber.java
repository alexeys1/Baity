package com.shyeuar.baity.features.fancydmgsplash;

public class CompactDamageNumber {
    private static final int DEFAULT_MAX_PRECISION = 4;
    
    private static final int[] DIGIT_GUESSES = new int[]{
        0,  1,  1,  1,  1,  2,  2,  2,
        3,  3,  3,  4,  4,  4,  4,  5,
        5,  5,  6,  6,  6,  7,  7,  7,
        7,  8,  8,  8,  9,  9,  9,  10,
        10, 10, 10, 11, 11, 11, 12, 12,
        12, 13, 13, 13, 13, 14, 14, 14,
        15, 15, 15, 16, 16, 16, 16, 17,
        17, 17, 18, 18, 18, 19, 19, 19,
    };

    private static final long[] POWERS_OF_TEN = new long[]{
        1L,                   10L,                100L,
        1000L,                10000L,             100000L,
        1000000L,             10000000L,          100000000L,
        1000000000L,          10000000000L,       100000000000L,
        1000000000000L,       10000000000000L,    100000000000000L,
        1000000000000000L,    10000000000000000L, 100000000000000000L,
        1000000000000000000L,
    };

    private CompactDamageNumber() {
    }

    public static String formatDamage(double damage, int maxPrecision) {
        long damageLong = (long) damage;
        return formatDamageNumber(damageLong, maxPrecision);
    }

    public static String formatDamage(double damage) {
        return formatDamage(damage, DEFAULT_MAX_PRECISION);
    }

    private static String formatDamageNumber(final long damage, final int maxPrecision) {
        long targetDamage = damage;
        int targetPrecision = maxPrecision;
        
        int usedPrecision = getBaseTenDigits(targetDamage);
        if (usedPrecision > targetPrecision) {
            double powerToRoundTo = POWERS_OF_TEN[usedPrecision - maxPrecision];
            targetDamage = (long) (Math.round((double) targetDamage / powerToRoundTo) * powerToRoundTo);
        } else if (targetPrecision > usedPrecision) {
            targetPrecision = usedPrecision;
        }

        if (targetDamage < 1_000L) return String.valueOf(targetDamage);
        if (targetDamage < 1_000_000L) return formatNumberToPrecision(targetDamage / 1_000.0, targetPrecision) + "k";
        if (targetDamage < 1_000_000_000L) return formatNumberToPrecision(targetDamage / 1_000_000.0, targetPrecision) + "M";
        if (targetDamage < 1_000_000_000_000L) return formatNumberToPrecision(targetDamage / 1_000_000_000.0, targetPrecision) + "B";
        if (targetDamage < 1_000_000_000_000_000L) return formatNumberToPrecision(targetDamage / 1_000_000_000_000.0, targetPrecision) + "T";
        return formatNumberToPrecision(targetDamage / 1_000_000_000_000_000.0, targetPrecision) + "Q";
    }

    private static String formatNumberToPrecision(double number, int precision) {
        int usedPrecision = getBaseTenDigits((int) number);
        int remainingPrecision = precision - usedPrecision;
        if (remainingPrecision <= 0) {
            long powerToRoundTo = POWERS_OF_TEN[usedPrecision - precision];
            return String.valueOf((Math.round(number / powerToRoundTo) * powerToRoundTo));
        }
        return ("%." + remainingPrecision + "f").formatted(number);
    }

    private static int getBaseTwoDigits(long x) {
        return 64 - Long.numberOfLeadingZeros(x);
    }

    private static int getBaseTenDigits(long x) {
        int guess = DIGIT_GUESSES[getBaseTwoDigits(x)];
        return guess + ((x >= POWERS_OF_TEN[guess]) ? 1 : 0);
    }
}
