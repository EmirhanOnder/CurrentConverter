package com.example.denem1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.homework3.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerFromCurrency, spinnerToCurrency;
    private HashMap<String, Double> currencyRates = new HashMap<>();
    List<String> currencyList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinnerFromCurrency = findViewById(R.id.spinnerFromCurrency);
        spinnerToCurrency = findViewById(R.id.spinnerToCurrency);
        Button convertButton = findViewById(R.id.buttonConvert);
        EditText editTextAmount = findViewById(R.id.editTextAmount);
        EditText editTextResult = findViewById(R.id.editTextResult);

        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fromCurrency = spinnerFromCurrency.getSelectedItem().toString();
                String toCurrency = spinnerToCurrency.getSelectedItem().toString();
                double amount = Double.parseDouble(editTextAmount.getText().toString());

                double result = convertCurrency(fromCurrency, toCurrency, amount);
                editTextResult.setText(String.format(Locale.getDefault(), "%.2f", result));
            }
        });

        new HTTPAsyncTask().execute("https://api.currencyfreaks.com/v2.0/rates/latest?apikey=f7ad20222da740e3be14550752bfce6e&format=xml", "https://api.currencyfreaks.com/v2.0/supported-currencies");


    }

    private class HTTPAsyncTask extends AsyncTask<String,Void,String[]> {

        @Override
        protected String[] doInBackground(String... urls) {
            String[] results = new String[2];
            try {
                results[0] = HttpGet(urls[0]);
                results[1] = HttpGet(urls[1]);
            } catch (IOException e) {
                e.printStackTrace();
                results[0] = "Unable to retrieve web page1. URL may be invalid.";
                results[1] = "Unable to retrieve web page2. URL may be invalid.";
            }
            return results;
        }

        protected void onPostExecute(String[] results) {
            if (results[0] != null && !results[0].startsWith("Unable")) {
                parseXMLAndStoreRates(results[0]);
                currencyList = XMLParser(results[0]);
                Collections.sort(currencyList);
                //updateCurrencyList(currencyList);
            }
            if (results[1] != null && !results[1].startsWith("Unable")) {
                HashMap<String, String> iconUrls = JSONParser(results[1]);
                CurrencyAdapter adapter = new CurrencyAdapter(MainActivity.this, currencyList, iconUrls);
                spinnerFromCurrency.setAdapter(adapter);
                spinnerToCurrency.setAdapter(adapter);
            }
        }

        private String HttpGet(String myUrl) throws IOException {
            InputStream inputStream = null;
            String result = "";
            URL url = new URL(myUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            inputStream = conn.getInputStream();
            if (inputStream != null) {
                result = convertInputStreamToString(inputStream);
            } else {
                result = "Did not work!";
            }
            return result;
        }

        private String convertInputStreamToString(InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            String result = "";
            while ((line = bufferedReader.readLine()) != null) {
                result += line;
            }
            inputStream.close();
            return result;
        }
    }

    private HashMap<String, String> JSONParser(String json) {
        HashMap<String, String> iconUrlMap = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject currenciesMap = jsonObject.getJSONObject("supportedCurrenciesMap");

            Iterator<String> keys = currenciesMap.keys();
            while(keys.hasNext()) {
                String key = keys.next(); // Para birimi kodu, örneğin "AGLD"
                JSONObject currencyJson = currenciesMap.getJSONObject(key);
                String iconUrl = currencyJson.optString("icon", null);

                if (iconUrl != null && !iconUrl.isEmpty()) {
                    Log.d("chekpoint-if","asdfadsf");
                    iconUrlMap.put(key, iconUrl);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return iconUrlMap;
    }

    private List<String> XMLParser(String xml) {
        List<String> currencyList = new ArrayList<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if (tagName.equals("rates")) {
                        while (!(eventType == XmlPullParser.END_TAG && parser.getName().equals("rates"))) {
                            if (eventType == XmlPullParser.START_TAG) {
                                if(!parser.getName().equals("rates"))
                                    currencyList.add(parser.getName());
                            }
                            eventType = parser.next();
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currencyList;
    }

    private void parseXMLAndStoreRates(String xml) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            int eventType = parser.getEventType();
            String currentTag = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        currentTag = parser.getName();
                        break;
                    case XmlPullParser.TEXT:
                        if (currentTag.equals("base")) {
                            currencyRates.put("USD", 1.0);
                        } else if (!currentTag.equals("rates")) {
                            try {
                                double rate = Double.parseDouble(parser.getText());
                                currencyRates.put(currentTag, rate);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double convertCurrency(String fromCurrency, String toCurrency, double amount) {
        if (currencyRates.containsKey(fromCurrency) && currencyRates.containsKey(toCurrency)) {
            double rateFrom = currencyRates.get(fromCurrency);
            double rateTo = currencyRates.get(toCurrency);
            return (amount / rateFrom) * rateTo; // Convert from the base currency to the target currency
        } else {
            return 0; // Handle the case where currency is not found
        }
    }
    /*private void updateCurrencyList(List<String> currencyList) {
        Collections.sort(currencyList);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencyList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFromCurrency.setAdapter(adapter);
        spinnerToCurrency.setAdapter(adapter);
    }*/

    public class CurrencyAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final List<String> currencyCodes;
        private final HashMap<String, String> iconUrls;

        public CurrencyAdapter(Context context, List<String> currencyCodes, HashMap<String, String> iconUrls) {
            super(context, R.layout.currency_spinner, currencyCodes);
            this.context = context;
            this.currencyCodes = currencyCodes;
            this.iconUrls = iconUrls;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return createItemView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return createItemView(position, convertView, parent);
        }

        private View createItemView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.currency_spinner, parent, false);
            }

            ImageView imageView = convertView.findViewById(R.id.imageViewCurrencyIcon);
            TextView textView = convertView.findViewById(R.id.textViewCurrencyCode);

            String currencyCode = currencyCodes.get(position);
            String iconUrl = iconUrls.get(currencyCode);

            textView.setText(currencyCode);
            if (iconUrl != null && !iconUrl.isEmpty()) {
                new DownloadImageTask(imageView).execute(iconUrl);
            }

            return convertView;
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Log.d("checkpoint bitmap doınbackground","sdfadsf");
            Bitmap bitmap = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            Log.d("checkpoint bitmap post","sdfadsf");
            imageView.setImageBitmap(result);
        }
    }

}