package com.dip.kidsworld;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class ListHostActivity extends Activity {

    SimpleDebitActivity.HistoryFragment historyFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_host);
        historyFragment = (SimpleDebitActivity.HistoryFragment) getFragmentManager().findFragmentById(R.id.history_fragment);
    }

    public void onClick(View vw) {
        EditText editProduct = (EditText) findViewById(R.id.et_product);
        EditText editPrice = (EditText) findViewById(R.id.et_price);
        String name = editProduct.getText().toString();
        int price;
        try {
            price = Integer.parseInt(editPrice.getText().toString());
        } catch (NumberFormatException e) {
            price = 0;
        }
        historyFragment.add(new SimpleDebitActivity.Product(name, price));
    }
}
