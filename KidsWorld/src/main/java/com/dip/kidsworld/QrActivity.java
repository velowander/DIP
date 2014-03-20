package com.dip.kidsworld;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class QrActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
    }

    public void onClick(View vw) {
        IntentIntegrator intentIntegrator = new IntentIntegrator(this); // where this is activity ALL_CODE_TYPES
        intentIntegrator.initiateScan(IntentIntegrator.QR_CODE_TYPES); // or ALL_CODE_TYPES
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            TextView txtResults = (TextView) findViewById(R.id.txtResults);
            txtResults.setText(scanResult.getContents());
        }
        // else continue with any other code you need in the method
    }
}
