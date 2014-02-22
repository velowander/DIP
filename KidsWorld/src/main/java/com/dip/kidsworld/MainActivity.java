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
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class MainActivity extends Activity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static NdefHelper ndefHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        */
        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "onCreate");

        setContentView(R.layout.activity_main);

        // initialize NFC
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        PendingIntent nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        ndefHelper = new NdefHelper(this, nfcAdapter, nfcPendingIntent);


        // Register Android Beam callback
        //nfcAdapter.setNdefPushMessageCallback(this, this);
        // Register callback to listen for message-sent success
        //nfcAdapter.setOnNdefPushCompleteCallback(this, this);

        /*
        if(getIntent().hasExtra(NfcAdapter.EXTRA_TAG)) {
            TextView textView = (TextView) findViewById(R.id.textView0);
            textView.setText("Hello NFC tag from home screen!");
            //vibrate(); // signal detected tag :-)
        };
        */
    }

    @Override
    protected void onResume() {
        Log.d(LOG_TAG, "onResume");

        super.onResume();

        ndefHelper.enableForegroundMode();
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "onPause");

        super.onPause();

        ndefHelper.disableForegroundMode();
    }

    @Override
    public void onNewIntent(final Intent intent) {
        Log.d(LOG_TAG, "onNewIntent");
        setIntent(intent);

        // *** Code below no longer needed, using Mifare UL format instead of NDEF ***
        //String domain = MainActivity.class.getCanonicalName(); //usually your app's package name
        //NdefRecord ndefAnswerToLife = NdefRecord.createExternal(domain, "answer_to_life", "42".getBytes());
        //NdefRecord ndefKingofBritain = NdefRecord.createExternal(domain, "king_of_britain", "Arthur".getBytes());
        //NdefRecord ndefConsultant = NdefRecord.createExternal(domain, "technical_consultant", "Jose".getBytes());
        //NdefRecord[] ndefRecords = {ndefAnswerToLife,ndefKingofBritain,ndefConsultant};

        //ndefHelper.simpleWrite(intent, new NdefRecord[]{ndefKingofBritain});
        /*
        MifareUltralightTester tester = new MifareUltralightTester();
        tester.writeTag(intent);
        String tagText = tester.readTag(intent);
        if (tagText != null) {
            Toast.makeText(this, "WOOHOO! tagText: " + tagText, Toast.LENGTH_LONG).show();
        }
        */
        ToggleButton button_mode = (ToggleButton) findViewById(R.id.button_mode);
        if (button_mode.isChecked()) {
            final int DEBIT_AMOUNT = 500;
            Log.d(LOG_TAG, "Debit Tag mode");
            int balance = KidsWorldNtag203.readTagBalance(intent);
            boolean writeOk = KidsWorldNtag203.writeTag(intent, new GateEvent(balance -= DEBIT_AMOUNT, new Date().getTime()));
            if (writeOk) {
                UpdateUI(intent);
                Toast.makeText(this, "Write complete", Toast.LENGTH_LONG).show();
            }
        } else {
            UpdateUI(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //if (id == R.id.action_settings) {
        //    return true;
        //}
        return super.onOptionsItemSelected(item);
    }

    public void UpdateUI(final Intent intent) {
        //intent - pass from onNewIntent, presence of NFC tag
        Log.d(LOG_TAG, "Update UI from read");
        GateEvent gateEvent = KidsWorldNtag203.readTag(intent);
        try {
            TextView tvwId = (TextView) findViewById(R.id.gate_id);
            tvwId.setText(String.valueOf(gateEvent.id));
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUI Exception" ,e);
        }
        try {
            TextView tvwName = (TextView) findViewById(R.id.gate_name);
            tvwName.setText(gateEvent.name);
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUI Exception", e);
        }
        try {
            TextView tvwBalance = (TextView) findViewById(R.id.gate_balance);
            tvwBalance.setText(String.valueOf(gateEvent.balance / 100.));
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUI Exception", e);
        }
        try {
            //TODO Dates displayed for last purchase transaction make no sense - issue in splitting double into 2x 4 byte pages
            TextView tvwDebit = (TextView) findViewById(R.id.gate_last_purchase);
            Date date = new Utils.Int64Date(gateEvent.lastPurchaseMs);
            tvwDebit.setText(date.toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUI Exception", e);
        }
        try {
            TextView tvwCheckIn = (TextView) findViewById(R.id.gate_check_in);
            Log.d(LOG_TAG, "MainActivity.UpdateUI(); checkInMs: " + gateEvent.checkInMs);
            Date date = new Utils.Int64Date(gateEvent.checkInMs);
            tvwCheckIn.setText(date.toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUI Exception", e);
        }
        try {
            TextView tvwCheckOut = (TextView) findViewById(R.id.gate_check_out);
            Log.d(LOG_TAG, "MainActivity.UpdateUI(); checkOutMs: " + gateEvent.checkOutMs);
            Date date = new Utils.Int64Date(gateEvent.checkInMs);
            tvwCheckOut.setText(date.toString());
            Toast.makeText(this, "Read complete", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUI Exception", e);
        }
    }

    public static class NdefHelper {
        protected NfcAdapter nfcAdapter;
        protected PendingIntent nfcPendingIntent;
        protected Activity parentActivity;

        public NdefHelper(Activity parentActivity, NfcAdapter nfcAdapter, PendingIntent nfcPendingIntent) {
            //parentActivity reference needed for NFC Foreground dispatch
            this.nfcAdapter = nfcAdapter;
            this.nfcPendingIntent = nfcPendingIntent;
            this.parentActivity = parentActivity;
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
                    Log.e(LOG_TAG, "IO or Format Exception connecting or getting NDEF message",e);
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

        /**
         * Converts the byte array to HEX string.
         *
         * @param buffer the buffer.
         * @return the HEX string.
         */
        public String toHexString(byte[] buffer) {
            StringBuilder sb = new StringBuilder();
            for (byte b : buffer)
                sb.append(String.format("%02x ", b & 0xff));
            return sb.toString().toUpperCase();
        }


    }

    public static class KidsWorldNtag203 {
        /* Helper class for reading and writing Kids World data to/from Mifare Ultralight card
        * The code is specific to the Mifare Ultralight which has 4 byte pages, reading from
        * the card returns 4 pages at a time. To store bytes compatible with Visual C++, it
        * reverses the byte order of both reads and writes. */
        private static final String LOG_TAG = KidsWorldNtag203.class.getSimpleName();

        //Constants for reading and writing to Mifare NTAG203
        private static final ByteOrder KW_ENDIANNESS = ByteOrder.LITTLE_ENDIAN;
        private static final short CARD_POSITION_ID = 4;
        private static final short CARD_POSITION_NAME = 6; // Uses all 4 pages for 16 byte name
        private static final short CARD_POSITION_BALANCE = 12;
        private static final short CARD_POSITION_CHECKIN = 14;// Uses all 4 pages for 2 dates (stored as doubles)
        private static final short CARD_POSITION_DEBIT_TIME = 19;

        public static int readTagBalance(final Intent intent) {
            /*Reads only the balance from the card, doesn't get all the fields */
            Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareUltralight mifare = MifareUltralight.get(myTag);

            int balance = -1;
            try {
                mifare.connect();
                balance = ByteBuffer.wrap(mifare.readPages(CARD_POSITION_BALANCE)).order(KW_ENDIANNESS).getInt();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    mifare.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "readTagBalance couldn't close mifare!!");
                }
            }
            Log.d(LOG_TAG, "balance: " + balance);

            return balance;
        }

        public static GateEvent readTag(final Intent intent) {
            Log.d(LOG_TAG, "Native Byte Ordering is Little Endian? " + String.valueOf(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN));
            Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareUltralight mifare = MifareUltralight.get(myTag);
            try {
                mifare.connect();
                int id = ByteBuffer.wrap(mifare.readPages(CARD_POSITION_ID)).order(KW_ENDIANNESS).getInt();
                //int id = Integer.reverseBytes(ByteBuffer.wrap(mifare.readPages(CARD_POSITION_ID)).getInt());
                Log.d(LOG_TAG, "id: " + id);
                String name = new String(mifare.readPages(CARD_POSITION_NAME)); //pages 6-9
                Log.d(LOG_TAG, "name: " + name);

                int balance = ByteBuffer.wrap(mifare.readPages(CARD_POSITION_BALANCE)).order(KW_ENDIANNESS).getInt();
                //int balance = Integer.reverseBytes(ByteBuffer.wrap(mifare.readPages(CARD_POSITION_BALANCE)).getInt());
                Log.d(LOG_TAG, "balance: " + balance);

                ByteBuffer dateBuffer = ByteBuffer.wrap(mifare.readPages(CARD_POSITION_CHECKIN)).order(KW_ENDIANNESS);
                long checkInMs = dateBuffer.getLong();
                Log.d(LOG_TAG, "checkInMs (long): " + checkInMs);

                long checkOutMs = dateBuffer.getLong();
                Log.d(LOG_TAG, "checkOutMs (long): " + checkOutMs);

                long debitTimeMs = ByteBuffer.wrap(mifare.readPages(19)).order(KW_ENDIANNESS).getLong();
                Log.d(LOG_TAG, "debitTime (long): " + debitTimeMs);

                GateEvent gateEvent = new GateEvent(balance, debitTimeMs);
                gateEvent.id = id;
                gateEvent.name = name;
                gateEvent.checkInMs = checkInMs;
                gateEvent.checkOutMs = checkOutMs;

                return gateEvent;

            } catch (IOException e) {
                Log.e(LOG_TAG, "IOException while writing MifareUltralight message...", e);
            } catch (NullPointerException e) {
                Log.e(LOG_TAG, "NullPointerException reading Mifare - not ultralight??", e);
            } finally {
                if (mifare != null) {
                    try {
                        mifare.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error closing tag...", e);
                    }
                }
            }
            return null;
        }

        public static boolean writeDouble(final Intent intent, MifareUltralight mifare, double doubleToWrite,
                                          int startPosition) {
            /* Helper method for the NXP NTAG203 / Ultralight that write a double (also useful for Dates which
            * can be saved as doubles) to the NFC card. Recall doubles are 8 bytes and pages are 4 bytes so
            * a double must split into two consecutive pages.
            * mifare must already be connected, and this method does not close it!
            * return value: boolean, successful or not */
//          TODO split byte array when reassembled isn't the same as original (??)
            if (mifare.isConnected()) {
                //byte[] eightBytes = ByteBuffer.allocate(8).order(KW_ENDIANNESS).putDouble(doubleToWrite).array();
                //Log.d(LOG_TAG, "writeDouble; length of eightBytes: " + String.valueOf(eightBytes.length));
                ByteBuffer bufferEight = ByteBuffer.allocate(8).order(KW_ENDIANNESS).putDouble(doubleToWrite);
                byte[] bytesFirstPage = new byte[4];
                byte[] bytesSecondPage = new byte[4];
                bufferEight.rewind();
                bufferEight.get(bytesFirstPage).get(bytesSecondPage);
                //byte[] bytesFirstPage = Arrays.copyOfRange(eightBytes, 0, 4);
                Log.d(LOG_TAG, "writeDouble; length of bytesFirstPage: " + String.valueOf(bytesFirstPage.length));
                //byte[] bytesSecondPage = Arrays.copyOfRange(eightBytes, 4, 8);
                Log.d(LOG_TAG, "writeDouble; length of bytesSecondPage: " + String.valueOf(bytesSecondPage.length));
                byte[] testBytes = ByteBuffer.allocate(8).order(KW_ENDIANNESS).put(bytesFirstPage).put(bytesSecondPage).array();
                Log.d(LOG_TAG, "writeDouble; arrays match?: " + String.valueOf(testBytes.equals(bufferEight.array())));
                try {
                    mifare.writePage(startPosition, bytesFirstPage);
                    mifare.writePage(startPosition + 1, bytesSecondPage);
                    return true;
                } catch (IOException e) {
                    Log.e(LOG_TAG, "writeDouble unable to write one or both pages", e);
                }
                return false;
            } else return false;
        }

        public static boolean writeTag(final Intent intent, final GateEvent gateEvent) {
            /* intent - pass onNewIntent from scanning NFC tag, saves gateEvent fields to card
            * times are stored as longs using Int64 conversion for .NET compatibility (Int64Date class)
            * Note: many fields are commented out as only the Windows .NET version modifies them */
            Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareUltralight ultralight = MifareUltralight.get(myTag);

            //if (gateEvent.name.length() > 16) { throw new IllegalArgumentException("Max name length: 16"); }
            //pad name to fixed length of 16 bytes, makes it easier to encode
            //String namePadded = padRight(gateEvent.name, ' ' , 16);
            //Log.d(LOG_TAG, "namePadded length: " + namePadded.length());
            //Log.d(LOG_TAG, "namePadded:<string>" + namePadded + "</string>" );
            //byte[] name06 = namePadded.substring( 0, 4).getBytes(Charset.forName(myCharset));
            //byte[] name07 = namePadded.substring( 4, 8).getBytes(Charset.forName(myCharset));
            //byte[] name08 = namePadded.substring( 8,12).getBytes(Charset.forName(myCharset));
            //byte[] name09 = namePadded.substring(12,16).getBytes(Charset.forName(myCharset));
            try {
                ultralight.connect();
                //ultralight.writePage(4, ByteBuffer.allocate(4).order(KW_ENDIANNESS).putInt(gateEvent.id).array());
                //ultralight.writePage( 6, name06);
                //ultralight.writePage( 7, name07);
                //ultralight.writePage( 8, name08);
                //ultralight.writePage( 9, name09);
                ultralight.writePage(CARD_POSITION_BALANCE, ByteBuffer.allocate(4).order(KW_ENDIANNESS).putInt(gateEvent.balance).array());
                Log.d(LOG_TAG, "Saving balance of " + gateEvent.balance);
                Date date = new Utils.Int64Date();
                Log.d(LOG_TAG, "writeTag: date is: " + date.toString());
                if (writeDouble(intent, ultralight, date.getTime(), CARD_POSITION_DEBIT_TIME)) {
                    Log.d(LOG_TAG, "Saved Debit Time (long): " + String.valueOf(date.getTime()));
                    Log.d(LOG_TAG, "Saved Debit Time: " + date.toString());
                }

                return true;
            } catch (IOException e) {
                Log.e(LOG_TAG, "IOException while writing MifareUltralight...", e);
            } finally {
                try {
                    ultralight.close();
                    Log.i(LOG_TAG, "Closed MifareUltralight write - success!!");
                } catch (IOException e) {
                    Log.e(LOG_TAG, "IOException while closing MifareUltralight...", e);
                }
            }
            return false;
        }
    }


    public static class GateEvent {
        /* This class is currently only a passive container for passing information about a GateEvent,
        * any data validation lies outside the class due to time pressure. In future, this class may
        * encapsulate its data validation. */
        public GateEvent(int balance, long lastPurchaseMs) {
            this.balance = balance;
            this.lastPurchaseMs = lastPurchaseMs;
        }

        int id;
        String name;
        int balance;
        long lastPurchaseMs;
        long checkInMs;
        long checkOutMs;
    }
}


