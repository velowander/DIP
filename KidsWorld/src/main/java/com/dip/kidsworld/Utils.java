package com.dip.kidsworld;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Date;

public class Utils {

    private static final String LOG = Utils.class.getSimpleName();

    @Deprecated
    public static class ByteUtil {

        /* Some helper utilities for working with bytes and byte arrays. Also has a few string
        * utilities (which I used for converting Strings to byte[]) */

        public static String repeat(String str, int times) {
            StringBuilder ret = new StringBuilder();
            for (int i = 0; i < times; i++) ret.append(str);
            return ret.toString();
        }

        public static byte[] toBytes(final int i) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            byteBuffer.putInt(i);
            return byteBuffer.array();
        }

        public static byte[] toBytes(final long l) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.putLong(l);
            return byteBuffer.array();
        }

        public static int toInt(final byte[] bytes) {
            if (bytes.length != 4) throw new IllegalArgumentException("byte[] must have length 4");
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            return byteBuffer.getInt();
        }
    }

    public class Int64Date extends Date {
        /* Specialized class to deal with .NET Int64 Datetime
        WARNING: Constructor (long) and getTime() now return Int64 ticks (100ns since 01/01/0001) rather than ms since Unix Epoch
        http://msdn.microsoft.com/en-us/library/z2xf7zzk.aspx */
        public static final long DIFF_TICK_TO_EPOCH = 621357696000000L;
        public static final byte TICKS_PER_MS = 10;

        public Int64Date() {
            super();
        }

        public Int64Date(long ticks) {
            super((ticks - DIFF_TICK_TO_EPOCH) / TICKS_PER_MS);
        }

        @Override
        public long getTime() {
            return super.getTime() * TICKS_PER_MS + DIFF_TICK_TO_EPOCH;
        }
    }


    public static class StringUtil {
        public static String padRight(final String initialString, final char filler, final int desiredLength) {
            /* Pad initialString with filler char to the right up to desiredLength and return
            Does not trim initialString if it is too long already */
            if (initialString.length() >= desiredLength) return initialString;
            int numberFiller = desiredLength - initialString.length();
            StringBuilder stringBuilder = new StringBuilder(desiredLength);
            stringBuilder.append(initialString);
            Log.d(LOG, "stringBuilder length: " + stringBuilder.length());
            for (int i = 0; i < numberFiller; i++) {
                stringBuilder.append(filler);
            }
            Log.d(LOG, "stringBuilder length after append: " + stringBuilder.length());
            return stringBuilder.toString();
        }

        public static String padRight(final String initialString, final int desiredLength) {
            //Assumes padding is spaces
            return padRight(initialString, ' ', desiredLength);
        }
    }
}