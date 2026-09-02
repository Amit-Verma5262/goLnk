package com.projects.golnk.util;

import java.math.BigInteger;
import java.security.SecureRandom;

public final class Base62 {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final int RADIX = ALPHABET.length;
    private static final SecureRandom RNG = new SecureRandom();

    private Base62() {
    }

    public static String randomCode(int length) {
        int bits = (int) Math.ceil(length * 6.0);
        byte[] buf = new byte[(bits + 7) / 8];
        RNG.nextBytes(buf);
        return encode(new BigInteger(1, buf)).substring(0, length);
    }

    private static String encode(BigInteger number) {
        if (number.signum() == 0) return "0";
        StringBuilder sb = new StringBuilder();
        BigInteger radix = BigInteger.valueOf(RADIX);
        while (number.signum() > 0) {
            BigInteger[] divRem = number.divideAndRemainder(radix);
            sb.append(ALPHABET[divRem[1].intValue()]);
            number = divRem[0];
        }
        return sb.reverse().toString();
    }
}
