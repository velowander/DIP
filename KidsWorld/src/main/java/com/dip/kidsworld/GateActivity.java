package com.dip.kidsworld;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.ToggleButton;


public class GateActivity extends Activity implements LoaderManager.LoaderCallbacks {

    private static final String LOG_TAG = GateActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gate);
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
        ToggleButton button_mode = (ToggleButton) findViewById(R.id.button_mode_gate);
        if (button_mode.isChecked()) {
            return new SimpleDebitActivity.KidsCard.CardLoader(this, getIntent(), new Utils.Int64Date());
        } else return new SimpleDebitActivity.KidsCard.CardLoader(this, getIntent());
    }

    @Override
    public void onLoadFinished(Loader loader, Object o) {
        SimpleDebitActivity.KidsCard kidsCard = (SimpleDebitActivity.KidsCard) o;
        updateUi(kidsCard);
    }

    @Override
    public void onLoaderReset(android.content.Loader loader) {
        //Not used
    }

    public void updateUi(final SimpleDebitActivity.KidsCard kidsCard) {
        try {
            TextView tvwCheckIn = (TextView) findViewById(R.id.check_in);
            tvwCheckIn.setText(kidsCard.getCheckInDate().toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUi Exception", e);
        }
        try {
            TextView tvwCheckOut = (TextView) findViewById(R.id.check_out);
            tvwCheckOut.setText(kidsCard.getCheckOutDate().toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUi Exception", e);
        }
    }

}
