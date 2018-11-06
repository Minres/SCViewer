package com.minres.scviewer.database;
public enum BitValue {
    ZERO,
    ONE,
    X,
    Z;

    private static final BitValue[] ORDINAL_TABLE = BitValue.values();

    public static BitValue fromChar(char c) {
        switch (c) {
            case '0':
                return ZERO;
            case '1':
                return ONE;
            case 'x':
            case 'X':
                return X;
            case 'z':
            case 'Z':
                return Z;
            default:
                throw new NumberFormatException("unknown digit " + c);
        }
    }

    public char toChar() {
        switch (this) {
            case ZERO:
                return '0';
            case ONE:
                return '1';
            case X:
                return 'x';
            case Z:
                return 'z';
        }

        return ' '; // Unreachable?
    }

    public static BitValue fromInt(int i) {
        if (i == 0) {
            return ZERO;
        } else {
            return ONE;
        }
    }

    public int toInt() {
        return (this == ONE) ? 1 : 0;
    }

    public static BitValue fromOrdinal(int ord) {
        return ORDINAL_TABLE[ord];
    }

    public int compare(BitValue other) {
        if (this == ONE && other == ZERO) {
            return 1;
        } else if (this == ZERO && other == ONE) {
            return -1;
        } else {
            // Either these are equal, or there is an X and Z, which match everything.
            return 0;
        }
    }
}