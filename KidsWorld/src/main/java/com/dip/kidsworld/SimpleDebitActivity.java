package com.dip.kidsworld;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class SimpleDebitActivity extends Activity implements LoaderManager.LoaderCallbacks {

    private static final String LOG_TAG = SimpleDebitActivity.class.getSimpleName();
    private static Utils.NdefHelper ndefHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_debit);

        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "onCreate");

        setContentView(R.layout.activity_simple_debit);

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
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.simple_debit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.menu_gate:
                startActivity(new Intent(this, GateActivity.class));
                break;
            case R.id.menu_write_test:
                startActivity(new Intent(this, WriteTestActivity.class));
                break;
            case R.id.menu_list_host:
                startActivity(new Intent(this, ListHostActivity.class));
                break;
            default:
                //Do Nothing
        }
        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        /* Process the product information from the QR code scanner */
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            try {
                JSONObject productDef = new JSONObject(scanResult.getContents());
                String sName = (String) productDef.get("name");
                String sPrice = (String) productDef.get("price");
                ((TextView) findViewById(R.id.et_product_name)).setText(sName);
                ((TextView) findViewById(R.id.et_product_price)).setText(sPrice);

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Unable to read JSON from QR Code");
            }
        }
    }

    @Override
    public void onNewIntent(final Intent intent) {

        Log.d(LOG_TAG, "onNewIntent");
        setIntent(intent);

        if (!NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) return;
        getLoaderManager().restartLoader(0, null, this).forceLoad();
    }

    public void onClick(View vw) {
        /* Google's zxing wrapper for open source app Barcode Scanner available on f-droid and Google Play */
        IntentIntegrator intentIntegrator = new IntentIntegrator(this); // where this is activity ALL_CODE_TYPES
        intentIntegrator.initiateScan(IntentIntegrator.QR_CODE_TYPES); // or ALL_CODE_TYPES
    }

    public void UpdateUi(final PrepaidCard prepaidCard) {
        //intent - pass from onNewIntent, presence of NFC tag
        Log.d(LOG_TAG, "Update UI from read");
        try {
            TextView tvwClientId = (TextView) findViewById(R.id.client_id);
            tvwClientId.setText(String.valueOf(prepaidCard.getClientId()));
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUi Exception", e);
        }
        try {
            TextView tvwName = (TextView) findViewById(R.id.client_name);
            tvwName.setText(prepaidCard.name);
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUi Exception", e);
        }
        try {
            TextView tvwBalance = (TextView) findViewById(R.id.balance);
            tvwBalance.setText(String.valueOf(prepaidCard.getBalance() / 100.));
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUi Exception", e);
        }
        try {
            TextView tvwDebit = (TextView) findViewById(R.id.last_purchase);
            tvwDebit.setText(prepaidCard.getLastPurchaseDate().toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "UpdateUi Exception", e);
        }
    }

    @Override
    public Loader onCreateLoader(int i, Bundle bundle) {
        Log.d(LOG_TAG, "in onCreateLoader()");
        /* Read or write (depending on button_mode) from the NFC card */
        ToggleButton button_mode = (ToggleButton) findViewById(R.id.button_mode);
        if (button_mode.isChecked()) {
            String sName = ((EditText) findViewById(R.id.et_product_name)).getText().toString();
            String sPrice = ((EditText) findViewById(R.id.et_product_price)).getText().toString();
            int price;
            try {
                price = 100 * Integer.parseInt(sPrice); // convert to pennies
            } catch (NumberFormatException e) {
                Log.i(LOG_TAG, "No valid product - read card only");
                return new PrepaidCard.CardLoader(this, getIntent());
            }
            return new PrepaidCard.CardLoader(this, getIntent(), new Product(sName, price));
        } else return new PrepaidCard.CardLoader(this, getIntent());
    }

    @Override
    public void onLoadFinished(android.content.Loader loader, Object o) {
        PrepaidCard prepaidCard = (PrepaidCard) o;
        UpdateUi(prepaidCard);
    }

    @Override
    public void onLoaderReset(android.content.Loader loader) {
        //Not used
    }

    public static class PrepaidCard {
        /* This represents a single contact with a PrepaidCard NFC card, it should remain in scope only
        * during the activity's onNewIntent() as reader scans the card. Repeated scans will create new
        * PrepaidCard instances.
        * Included CardLoader class (extends AsyncTaskLoader) reads and writes to NFC cards on a worker thread
        * Don't write() to the card outside onNewIntent(), as you could overwrite data on a different
        * card accidentally (or the original card may no longer be present)
        * * times are stored as Utils.Int64Date (longs) for .NET compatibility */

        private static final String LOG_TAG = PrepaidCard.class.getSimpleName();

        //Constants for reading and writing to Mifare NTAG203
        private static final short OFFSET_ID = 4;
        private static final short OFFSET_NAME = 6; // Uses all 4 pages for 16 byte name
        private static final short OFFSET_BALANCE = 12;
        private static final short OFFSET_CHECKIN = 14;// Uses all 4 pages for 2 dates (stored as doubles)
        private static final short OFFSET_DEBIT_TIME = 19;

        private MifareUltralight mifare;

        private int clientId;
        private String name;
        private int balance;
        private Utils.Int64Date lastPurchaseDate;
        private Utils.Int64Date checkInDate;
        private Utils.Int64Date checkOutDate;

        public PrepaidCard(Intent intent) {
            /* intent - pass onNewIntent from scanning NFC tag, saves kidsCard fields to card */
            Log.d(LOG_TAG, "Native Byte Ordering is Little Endian? " + String.valueOf(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN));
            Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            this.mifare = MifareUltralight.get(myTag);
            try {
                mifare.connect();
                int id = ByteBuffer.wrap(mifare.readPages(OFFSET_ID)).order(Utils.Ntag203.ENDIANNESS).getInt();
                Log.d(LOG_TAG, "clientId: " + id);
                this.clientId = id;

                String name = new String(mifare.readPages(OFFSET_NAME)); //pages 6-9
                Log.d(LOG_TAG, "name: " + name);
                this.name = name;

                int balance = ByteBuffer.wrap(mifare.readPages(OFFSET_BALANCE)).order(Utils.Ntag203.ENDIANNESS).getInt();
                Log.d(LOG_TAG, "balance: " + balance);
                this.balance = balance;

                long debitTicks = ByteBuffer.wrap(mifare.readPages(OFFSET_DEBIT_TIME)).order(Utils.Ntag203.ENDIANNESS).getLong();
                Log.d(LOG_TAG, "debitTicks (long): " + debitTicks);
                this.lastPurchaseDate = new Utils.Int64Date(debitTicks);

                //Read check in and check out together since they are adjacent
                ByteBuffer dateBuffer = ByteBuffer.wrap(mifare.readPages(OFFSET_CHECKIN)).order(Utils.Ntag203.ENDIANNESS);
                long checkInTicks = dateBuffer.getLong();
                Log.d(LOG_TAG, "checkInTicks (long): " + checkInTicks);
                this.checkInDate = new Utils.Int64Date(checkInTicks);
                long checkOutTicks = dateBuffer.getLong();
                Log.d(LOG_TAG, "checkOutTicks (long): " + checkOutTicks);
                this.checkOutDate = new Utils.Int64Date(checkOutTicks);
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
        }

        public boolean buy(Product product) throws IOException {
            int price = product.getPrice();
            if (balance >= price) {
                balance -= price;
                lastPurchaseDate = new Utils.Int64Date();
                try {
                    this.mifare.connect();
                    this.mifare.writePage(OFFSET_BALANCE, ByteBuffer.allocate(4).order(Utils.Ntag203.ENDIANNESS).putInt(this.balance).array());
                    Log.d(LOG_TAG, "Saving balance of " + getBalance());
                    Utils.Int64Date date = new Utils.Int64Date();
                    Utils.Ntag203.writeLong(this.mifare, date.getTicks(), OFFSET_DEBIT_TIME);
                    Log.d(LOG_TAG, "Saved Debit Ticks (long): " + String.valueOf(date.getTime()));
                    Log.d(LOG_TAG, "Saved Debit Time: " + date.toString());
                    return true;
                } finally {
                    if (this.mifare.isConnected()) this.mifare.close();
                }
            } else return false;
        }

        public void checkIn() throws IOException {
            try {
                this.mifare.connect();
                Utils.Ntag203.writeLong(this.mifare, new Utils.Int64Date().getTicks(), OFFSET_CHECKIN);
            } finally {
                if (this.mifare.isConnected()) this.mifare.close();
            }
        }

        @SuppressWarnings("unused")
        public void checkOut() throws IOException {
            try {
                this.mifare.connect();
                Utils.Ntag203.writeLong(this.mifare, new Utils.Int64Date().getTicks(), OFFSET_CHECKIN + 2);
            } finally {
                if (this.mifare.isConnected()) this.mifare.close();
            }

        }

        public int getClientId() {
            return this.clientId;
        }

        public int getBalance() {
            return balance;
        }

        public Utils.Int64Date getCheckInDate() {
            /* Returns the check in time in Intel64 ticks */
            return checkInDate;
        }

        public Utils.Int64Date getCheckOutDate() {
            /* Returns the check out time in Intel64 ticks */
            return checkOutDate;
        }

        public Utils.Int64Date getLastPurchaseDate() {
            /* Returns the last purchase time in Intel64 ticks */
            return lastPurchaseDate;
        }

        public static class CardLoader extends AsyncTaskLoader<PrepaidCard> {

            /* Use this helper class to read and write to the NFC tag on a worker thread */

            public final String LOG_TAG = CardLoader.class.getSimpleName();
            private final byte LOADER_MODE_READ = 0;
            private final byte LOADER_MODE_DEBIT = 1;
            private final byte LOADER_MODE_CHECK_IN = 2;
            private byte loaderMode;
            private Intent intent;
            private Product product;

            public CardLoader(Context context, Intent intent) {
                /* Constructor to read card only
                * intent: NFC tag intent from Activity.onNewIntent() */
                super(context);
                loaderMode = LOADER_MODE_READ;
                this.intent = intent;
            }

            public CardLoader(Context context, Intent intent, Product product) {
                /* Constructor for card debit (purchase). In balance is sufficient, will debit card funds
                * to purchase product */
                super(context);
                loaderMode = LOADER_MODE_DEBIT;
                this.intent = intent;
                this.product = product;
            }

            @SuppressWarnings("unused")
            public CardLoader(Context context, Intent intent, Utils.Int64Date date) {
                super(context);
                loaderMode = LOADER_MODE_CHECK_IN;
                this.intent = intent;
                //This class doesn't use the Utils.Int64Date; it triggers loading this constructor
            }

            @Override
            public PrepaidCard loadInBackground() {
                /* instantiates PrepaidCard (reads NFC) and optionally debits supplied PrepaidCard */
                Log.d(LOG_TAG, "loadInBackground()");
                PrepaidCard prepaidCard = new PrepaidCard(intent);
                switch (loaderMode) {
                    case LOADER_MODE_READ:
                        break; //constructor reads data from NFC card, just return the populated object
                    case LOADER_MODE_DEBIT:
                        Log.d(LOG_TAG, "buy() price: " + String.valueOf(product.price));
                        try {
                            prepaidCard.buy(product);
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "buy(): NFC IOException");
                        }
                        break;
                    case LOADER_MODE_CHECK_IN:
                        Log.d(LOG_TAG, "begin check in");
                        try {
                            prepaidCard.checkIn();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "checkIn(): NFC IOException");
                        }
                        break;
                    default:
                        // same as LOADER_MODE_READ
                }
                return prepaidCard;
            }
        }
    }

    public static class Product {
        private String name;
        private int price;

        public Product(String name, int price) {
            this.name = name;
            this.price = price;
        }

        public String getName() {
            return this.name;
        }

        public int getPrice() {
            return this.price;
        }
    }

    public static class HistoryFragment extends ListFragment {

        CardTransactionHistoryAdapter adapter;

        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            adapter = new CardTransactionHistoryAdapter(getActivity());
            setListAdapter(adapter);
        }

        public void add(Product product) {
            adapter.add(product);
        }

        public static class CardTransactionHistoryAdapter extends BaseAdapter {
            private static LayoutInflater inflater;
            private List<Product> list = new ArrayList<>();

            public CardTransactionHistoryAdapter(Context context) {
                inflater = LayoutInflater.from(context);
            }

            public void add(Product product) {
                list.add(product);
                notifyDataSetChanged();
            }

            public int getCount() {
                return list.size();
            }

            public Object getItem(int i) {
                return list.get(i);
            }

            public long getItemId(int i) {
                return i;
            }

            public View getView(int i, View view, ViewGroup viewGroup) {
                if (view == null) {
                    Product product = list.get(i);
                    view = inflater.inflate(R.layout.card_transaction_view, null);
                    ((TextView) view.findViewById(R.id.tvw_product_name)).setText(product.getName());
                    ((TextView) view.findViewById(R.id.tvw_product_price)).setText(String.valueOf(product.getPrice()));
                }
                return view;
            }
        }
    }
}