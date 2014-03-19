package com.dip.kidsworld;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class Utils {

    private static final String LOG = Utils.class.getSimpleName();

    @Deprecated
    @SuppressWarnings("unused")
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

    public static class Int64Date extends Date {
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

    public static class NdefHelper {
        protected NfcAdapter nfcAdapter;
        protected PendingIntent nfcPendingIntent;
        protected Activity parentActivity;
        private String LOG_TAG = NdefHelper.class.getSimpleName();

        public NdefHelper(Activity parentActivity, NfcAdapter nfcAdapter, PendingIntent nfcPendingIntent) {
            //parentActivity reference needed for NFC Foreground dispatch
            this.nfcAdapter = nfcAdapter;
            this.nfcPendingIntent = nfcPendingIntent;
            this.parentActivity = parentActivity;
        }

        /**
         * Converts the byte array to HEX string.
         *
         * @param buffer the buffer.
         * @return the HEX string.
         */
        public static String toHexString(byte[] buffer) {
            StringBuilder sb = new StringBuilder();
            for (byte b : buffer)
                sb.append(String.format("%02x ", b & 0xff));
            return sb.toString().toUpperCase();
        }

        public void disableForegroundMode() {
            Log.d(LOG_TAG, "disableForegroundMode");
            nfcAdapter.disableForegroundDispatch(parentActivity);
        }

        public void enableForegroundMode() {
            Log.d(LOG_TAG, "enableForegroundMode");

            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED); // filter for all

            IntentFilter[] writeTagFilters = new IntentFilter[]{tagDetected};
            nfcAdapter.enableForegroundDispatch(parentActivity, nfcPendingIntent, writeTagFilters, null);
        }

        @SuppressWarnings("unused")
        private String simpleRead(final Intent intent) {
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
                Log.d(LOG_TAG, "ACTION_TAG_DISCOVERED");
                Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if (intent.hasExtra(NfcAdapter.EXTRA_ID)) {
                    byte[] byteArrayExtra = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                    String sTagId = "Tag id is " + toHexString(byteArrayExtra);
                    Log.d(LOG_TAG, sTagId);
                }
                Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                Log.i(LOG_TAG, "rawMsgs is null: " + (rawMsgs == null));
                NdefMessage message0 = (NdefMessage) rawMsgs[0];
                NdefRecord record0 = message0.getRecords()[0];
                return new String(record0.getPayload());
            }
            return null;
        }

        @SuppressWarnings("unused")
        protected void printTagId(final Intent intent) {
            if (intent.hasExtra(NfcAdapter.EXTRA_ID)) {
                byte[] byteArrayExtra = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                Log.d(LOG_TAG, "Tag id is " + toHexString(byteArrayExtra));
                Toast.makeText(parentActivity, toHexString(byteArrayExtra), Toast.LENGTH_SHORT).show();
            }
        }

        private String simpleRead2(final Intent intent) {
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
                Log.d(LOG_TAG, "ACTION_TAG_DISCOVERED");
                Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Ndef ndef = Ndef.get(myTag);
                NdefMessage ndefMessage;
                NdefRecord record0 = null;
                try {
                    ndef.connect();
                    ndefMessage = ndef.getNdefMessage();
                    record0 = ndefMessage.getRecords()[0];
                    ndef.close();
                } catch (IOException | FormatException e) {
                    Log.e(LOG_TAG, "IO or Format Exception connecting or getting NDEF message", e);
                }
                if (record0 != null) {
                    return new String(record0.getPayload());
                }
            }
            return null;
        }

        private void simpleWrite(final Intent intent, NdefRecord[] ndefRecords) {
            //Did you remember to setIntent() to the new intent before calling?
            NdefMessage ndefMessage = new NdefMessage(ndefRecords);
            if (ndefRecords == null) {
                Log.w(LOG_TAG, "ndefRecords is null");
            }
            Ndef ndef;

            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
                Log.d(LOG_TAG, "A tag was scanned!");
                Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                //write(message, intent);
                Toast.makeText(parentActivity, "LOG_TAG: " + myTag.toString(), Toast.LENGTH_SHORT).show();
                ndef = Ndef.get(myTag);

                try {
                    if (!ndef.isConnected()) {
                        ndef.connect();
                    }
                    ndef.writeNdefMessage(ndefMessage);
                    ndef.close();
                    Log.i(LOG_TAG, "Write complete!");
                } catch (IOException e) {
                    Log.e(LOG_TAG, "writeNDefMessage IOException", e);
                } catch (FormatException e) {
                    Log.e(LOG_TAG, "writeNDefMessage FormatException", e);
                } catch (NullPointerException e) {
                    Log.e(LOG_TAG, "writeNDefMessage NullPointerException", e);
                }
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

    public static class Ntag203 {
        /* reading and writing methods specific to the Ntag203 card but not specific to KidsWorld*/
        public static final ByteOrder ENDIANNESS = ByteOrder.LITTLE_ENDIAN;

        public static long readLong(final MifareUltralight mifare, final int startPosition) throws IOException {
        /* Helper method for the NXP NTAG203 / Ultralight that reads a long (also useful for Dates which
        * can be saved as longs) from the NFC card.
        * If mifare is already connected, uses the existing connection and does not close it afterwards,
        * otherwise connects and closes the connection. */
            boolean closeMifareOnExit = false;
            long result;
            try {
                if (!mifare.isConnected()) {
                    mifare.connect();
                    closeMifareOnExit = true;
                }
                result = ByteBuffer.wrap(mifare.readPages(startPosition)).order(ENDIANNESS).getLong();
            } finally {
                if (closeMifareOnExit && mifare.isConnected()) mifare.close();
            }
            return result;
        }

        public static void writeLong(final MifareUltralight mifare, final long longToWrite,
                                     final int startPosition) throws IOException {
        /* Helper method for the NXP NTAG203 / Ultralight that writes a long (also useful for Dates which
        * can be saved as longs) to the NFC card. Recall doubles are 8 bytes and pages are 4 bytes so
        * a double must split into two consecutive pages.
        * If mifare is already connected, uses the existing connection and does not close it afterwards,
        * otherwise connects and closes the connection. */
            boolean closeMifareOnExit = false;
            try {
                if (!mifare.isConnected()) {
                    mifare.connect();
                    closeMifareOnExit = true;
                }
                ByteBuffer bufferEight = ByteBuffer.allocate(8).order(ENDIANNESS).putLong(longToWrite);
                byte[] bytesFirstPage = new byte[4];
                byte[] bytesSecondPage = new byte[4];
                bufferEight.rewind();
                bufferEight.get(bytesFirstPage).get(bytesSecondPage);
                mifare.writePage(startPosition, bytesFirstPage);
                mifare.writePage(startPosition + 1, bytesSecondPage);
            } finally {
                if (closeMifareOnExit && mifare.isConnected()) mifare.close();
            }
        }
    }
}