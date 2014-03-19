package com.dip.kidsworld;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class SimpleDebitActivity extends Activity {

    private static final String LOG_TAG = SimpleDebitActivity.class.getSimpleName();
    private static Utils.NdefHelper ndefHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "onCreate");

        setContentView(R.layout.activity_nfc);

        // initialize NFC
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        PendingIntent nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        ndefHelper = new Utils.NdefHelper(this, nfcAdapter, nfcPendingIntent);
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

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()))
            nfcOperation(intent); //not an NFC tag, nothing to do!
    }

    protected void nfcOperation(Intent intent) {
        /* Read or write (depending on button_mode) from the NFC card
        * For this sample application, deducts the constant DEBIT_AMOUNT from */
        ToggleButton button_mode = (ToggleButton) findViewById(R.id.button_mode);
        if (button_mode.isChecked()) {
            //Debit mode
            final int DEBIT_AMOUNT = 500;
            Log.d(LOG_TAG, "Debit Tag mode");
            int balance = KidsWorldNtag203.readTagBalance(intent);
            boolean writeOk = false;
            if (balance >= DEBIT_AMOUNT) {
                writeOk = KidsWorldNtag203.writeTag(intent, new GateEvent(balance -= DEBIT_AMOUNT, new Utils.Int64Date().getTime()));
                if (writeOk) {
                    UpdateUI(intent);
                    Toast.makeText(this, "Debit complete", Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(this, "Unable to process transaction", Toast.LENGTH_LONG).show();
            }
            //Card doesn't have enough money!!
            else Toast.makeText(this, "Insufficient balance!", Toast.LENGTH_LONG).show();
        } else {
            //Read mode
            UpdateUI(intent);
        }
    }

    public void UpdateUI(final Intent intent) {
        //intent - pass from onNewIntent, presence of NFC tag
        Log.d(LOG_TAG, "Update UI from read");
        GateEvent gateEvent = KidsWorldNtag203.readTag(intent);
        try {
            TextView tvwId = (TextView) findViewById(R.id.gate_id);
            tvwId.setText(String.valueOf(gateEvent.id));
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUI Exception", e);
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
            TextView tvwDebit = (TextView) findViewById(R.id.gate_last_purchase);
            Date date = new Utils.Int64Date(gateEvent.lastPurchaseMs);
            tvwDebit.setText(date.toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUI Exception", e);
        }
        try {
            TextView tvwCheckIn = (TextView) findViewById(R.id.gate_check_in);
            Log.d(LOG_TAG, "SimpleDebitActivity.UpdateUI(); checkInMs: " + gateEvent.checkInMs);
            Date date = new Utils.Int64Date(gateEvent.checkInMs);
            tvwCheckIn.setText(date.toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUI Exception", e);
        }
        try {
            TextView tvwCheckOut = (TextView) findViewById(R.id.gate_check_out);
            Log.d(LOG_TAG, "SimpleDebitActivity.UpdateUI(); checkOutMs: " + gateEvent.checkOutMs);
            Date date = new Utils.Int64Date(gateEvent.checkInMs);
            tvwCheckOut.setText(date.toString());
            Toast.makeText(this, "Read complete", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUI Exception", e);
        }
    }

    public static class KidsWorldNtag203 {
        /* Helper class for reading and writing Kids World data to/from Mifare Ultralight card
        * The code is specific to the Mifare Ultralight which has 4 byte pages, reading from
        * the card returns 4 pages at a time. To store bytes compatible with Visual C++, it
        * reverses the byte order of both reads and writes. */
        private static final String LOG_TAG = KidsWorldNtag203.class.getSimpleName();

        //Constants for reading and writing to Mifare NTAG203
        private static final short OFFSET_ID = 4;
        private static final short OFFSET_NAME = 6; // Uses all 4 pages for 16 byte name
        private static final short OFFSET_BALANCE = 12;
        private static final short OFFSET_CHECKIN = 14;// Uses all 4 pages for 2 dates (stored as doubles)
        private static final short OFFSET_DEBIT_TIME = 19;

        public static int readTagBalance(final Intent intent) {
            /*Reads only the balance from the card, doesn't get all the fields */
            Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareUltralight mifare = MifareUltralight.get(myTag);

            int balance = -1;
            try {
                mifare.connect();
                balance = ByteBuffer.wrap(mifare.readPages(OFFSET_BALANCE)).order(Utils.Ntag203.ENDIANNESS).getInt();
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
                int id = ByteBuffer.wrap(mifare.readPages(OFFSET_ID)).order(Utils.Ntag203.ENDIANNESS).getInt();
                Log.d(LOG_TAG, "id: " + id);
                String name = new String(mifare.readPages(OFFSET_NAME)); //pages 6-9
                Log.d(LOG_TAG, "name: " + name);

                int balance = ByteBuffer.wrap(mifare.readPages(OFFSET_BALANCE)).order(Utils.Ntag203.ENDIANNESS).getInt();
                Log.d(LOG_TAG, "balance: " + balance);

                ByteBuffer dateBuffer = ByteBuffer.wrap(mifare.readPages(OFFSET_CHECKIN)).order(Utils.Ntag203.ENDIANNESS);
                long checkInMs = dateBuffer.getLong();
                Log.d(LOG_TAG, "checkInMs (long): " + checkInMs);

                long checkOutMs = dateBuffer.getLong();
                Log.d(LOG_TAG, "checkOutMs (long): " + checkOutMs);

                long debitTimeMs = ByteBuffer.wrap(mifare.readPages(OFFSET_DEBIT_TIME)).order(Utils.Ntag203.ENDIANNESS).getLong();
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
                ultralight.writePage(OFFSET_BALANCE, ByteBuffer.allocate(4).order(Utils.Ntag203.ENDIANNESS).putInt(gateEvent.balance).array());
                Log.d(LOG_TAG, "Saving balance of " + gateEvent.balance);
                Date date = new Utils.Int64Date();
                Log.d(LOG_TAG, "writeTag: date is: " + date.toString());
                Utils.Ntag203.writeLong(ultralight, date.getTime(), OFFSET_DEBIT_TIME);
                Log.d(LOG_TAG, "Saved Debit Time (long): " + String.valueOf(date.getTime()));
                Log.d(LOG_TAG, "Saved Debit Time: " + date.toString());
                return true;
            } catch (IOException e) {
                Log.e(LOG_TAG, "IOException while writing MifareUltralight...", e);
            } finally {
                try {
                    ultralight.close();
                    Log.i(LOG_TAG, "Closed Mifare Ultralight write - success!!");
                } catch (IOException e) {
                    Log.e(LOG_TAG, "IOException while closing MifareUltralight...", e);
                }
            }
            return false;
        }
    }

    public static class GateEvent {
        int id;
        String name;
        int balance;
        long lastPurchaseMs;
        long checkInMs;
        long checkOutMs;

        /* This class is currently only a passive container for passing information about a GateEvent,
        * any data validation lies outside the class due to time pressure. In future, this class may
        * encapsulate its data validation. */
        public GateEvent(int balance, long lastPurchaseMs) {
            this.balance = balance;
            this.lastPurchaseMs = lastPurchaseMs;
        }
    }
}