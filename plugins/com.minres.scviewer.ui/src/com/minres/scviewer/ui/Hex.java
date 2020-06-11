package com.minres.scviewer.ui;
public final class Hex {
    public static byte[] decode(final String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("A hex string must contain an even number of characters: " + hex);
        }

        byte[] out = new byte[hex.length() / 2];

        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("A hex string can only contain the characters 0-9, A-F, a-f: " + hex);
            }

            out[i / 2] = (byte) (high * 16 + low);
        }

        return out;
    }

    private static final char[] UPPER_HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', };

    public static String encode(final byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder(bytes.length * 2);
        for (byte cur : bytes) {
            stringBuilder.append(UPPER_HEX_DIGITS[(cur >> 4) & 0xF]);
            stringBuilder.append(UPPER_HEX_DIGITS[(cur & 0xF)]);
        }
        return stringBuilder.toString();
    }

    private Hex() {
    }
}