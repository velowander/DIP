package com.dip.kidsworld;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;


public class WriteTestActivity extends Activity {

    private static final String LOG_TAG = WriteTestActivity.class.getSimpleName();
    private static Utils.NdefHelper ndefHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_test);

        // initialize NFC
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        PendingIntent nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        ndefHelper = new Utils.NdefHelper(this, nfcAdapter, nfcPendingIntent);
    }

    @Override
    public void onNewIntent(final Intent intent) {
        Log.d(LOG_TAG, "onNewIntent");
        setIntent(intent);

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) nfcOperation(intent);
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

    public void nfcOperation(Intent intent) {
        /* Read or write the long value from/to the NFC card, typically fired from onNewIntent() so
        * an NFC card is present and being scanned. Handles updating the UI directly as needed */
        ToggleButton button_mode = (ToggleButton) findViewById(R.id.button_mode_2);
        Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        MifareUltralight mifare = MifareUltralight.get(myTag);
        EditText editOffset = (EditText) findViewById(R.id.et_position_offset);
        EditText editLong = (EditText) findViewById(R.id.et_long_testing);
        long longTesting;
        int offset;
        try {
            longTesting = Long.parseLong(editLong.getText().toString());
            offset = Integer.parseInt(editOffset.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "The testing value and offset must be numbers", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            mifare.connect();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to connect to mifare");
            return;
        }
        if (button_mode.isChecked()) {
            Log.d(LOG_TAG, "Write Tag mode");
            try {
                Utils.Ntag203.writeLong(mifare, longTesting, offset);
                String text = "Write Successful";
                Log.d(LOG_TAG, text);
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to write to NFC card");
            }
        } else {
            Log.d(LOG_TAG, "Read tag mode");
            long readLong = 0;
            try {
                readLong = Utils.Ntag203.readLong(mifare, offset);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to read long value from NFC card");
            }
            if (longTesting == readLong) {
                String text = "Number verified!";
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                Log.d(LOG_TAG, text);
            } else {
                Log.d(LOG_TAG, "Number read is: " + readLong);
                Toast.makeText(this, "Number does not match. Number read is: " + readLong, Toast.LENGTH_LONG).show();
            }
        }
        try {
            if (mifare.isConnected()) mifare.close();
        } catch (IOException e) {
            Log.d(LOG_TAG, "Unable to close mifare connection");
        }
    }
}
