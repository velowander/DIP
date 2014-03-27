package com.dip.kidsworld;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Loader;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class GateActivity extends Activity implements LoaderManager.LoaderCallbacks {

    private static final String LOG_TAG = GateActivity.class.getSimpleName();
    private static Utils.NdefHelper ndefHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gate);

        // initialize NFC
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        PendingIntent nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        ndefHelper = new Utils.NdefHelper(this, nfcAdapter, nfcPendingIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ndefHelper.enableForegroundMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ndefHelper.disableForegroundMode();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return item.getItemId() == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onNewIntent(final Intent intent) {

        Log.d(LOG_TAG, "onNewIntent");
        setIntent(intent);

        if (!NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) return;
        getLoaderManager().restartLoader(0, null, this).forceLoad();
    }

    @Override
    public Loader onCreateLoader(int i, Bundle bundle) {
        Log.d(LOG_TAG, "in onCreateLoader()");
        /* Read or write (depending on button_mode) from the NFC card */
        return new SimpleDebitActivity.PrepaidCard.CardLoader(this, getIntent(), new Utils.Int64Date());
    }

    @Override
    public void onLoadFinished(Loader loader, Object o) {
        SimpleDebitActivity.PrepaidCard prepaidCard = (SimpleDebitActivity.PrepaidCard) o;
        updateUi(prepaidCard);
    }

    @Override
    public void onLoaderReset(android.content.Loader loader) {
        //Not used
    }

    public void updateUi(final SimpleDebitActivity.PrepaidCard prepaidCard) {
        try {
            TextView tvwCheckIn = (TextView) findViewById(R.id.check_in);
            tvwCheckIn.setText(prepaidCard.getCheckInDate().toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUi Exception", e);
        }
        try {
            TextView tvwCheckOut = (TextView) findViewById(R.id.check_out);
            tvwCheckOut.setText(prepaidCard.getCheckOutDate().toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUi Exception", e);
        }
    }

}
