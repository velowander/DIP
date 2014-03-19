package com.dip.kidsworld;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.io.IOException;


public class WriteTest extends Activity {

    private static final String LOG_TAG = WriteTest.class.getSimpleName();
    private static NfcActivity.NdefHelper ndefHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_test);

        // initialize NFC
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        PendingIntent nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        ndefHelper = new NfcActivity.NdefHelper(this, nfcAdapter, nfcPendingIntent);
    }

    @Override
    public void onNewIntent(final Intent intent) {
        Log.d(LOG_TAG, "onNewIntent");
        setIntent(intent);

        ToggleButton button_mode = (ToggleButton) findViewById(R.id.button_mode_2);
        Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        MifareUltralight mifare = MifareUltralight.get(myTag);
        EditText editPosition = (EditText) findViewById(R.id.position_number);
        int positionNumber = Integer.parseInt(editPosition.getText().toString());
        try {
            mifare.connect();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to connect to mifare");
            return;
        }
        if (button_mode.isChecked()) {
            Log.d(LOG_TAG, "Write Tag mode");
            EditText editLong = (EditText) findViewById(R.id.long_to_write);
            long longToWrite = Long.parseLong(editLong.getText().toString());
            NfcActivity.KidsWorldNtag203.writeLong(mifare, longToWrite, positionNumber);

        } else {
            Log.d(LOG_TAG, "Read tag mode");
        }
        try {
            mifare.close();
        } catch (IOException e) {
            Log.d(LOG_TAG, "Unable to close mifare connection");
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.write_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
