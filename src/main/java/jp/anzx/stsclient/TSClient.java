package jp.anzx.stsclient;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class TSClient {

    private final static String TAG = "TSClient";
    private final static String KEY = "qwerty1337";

    interface TSClientListener{
        void OnTranslated(String text);
    }

    private TSClientListener tsClientListener;


    private String langFrom = "ru";
    private String langTo = "en";

    private Handler handler;

    //set

    public void setLangFrom(String langFrom) {
        this.langFrom = langFrom;
        Log.i(TAG, "langFrom = " + langFrom);
    }

    public void setLangTo(String langTo) {
        this.langTo = langTo;
        Log.i(TAG, "langTo = " + langTo);
    }

    public void setTsClientListener(TSClientListener tsClientListener) {
        this.tsClientListener = tsClientListener;
    }

    //get

    public String getLangFrom() {
        return langFrom;
    }

    public String getLangTo() {
        return langTo;
    }

    public TSClient(){
        handler = new Handler() {
            public void handleMessage(Message msg) {

                Bundle bundle = msg.getData();
                String translatedText = bundle.getString(KEY);

                tsClientListener.OnTranslated(translatedText);
            }
        };
    }

    public void TS(final String text){

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "re:");

                HashMap<String, String> data = new HashMap<String, String>();

                data.put("lang_to", langTo);
                data.put("lang_from", langFrom);
                data.put("text", text);

                String re = performGetCall("https://sts-web-app.herokuapp.com/ts", data);

                Log.i(TAG, re);

                if(tsClientListener != null){

                    Message msg = handler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString(KEY, re);
                    msg.setData(bundle);
                    handler.sendMessage(msg);

                }

            }
        });

        t.start();
    }

    public String performGetCall(String requestURL, HashMap<String, String> getDataParams){

        URL url;
        String response = "";

        try {
            url = new URL(requestURL + getDataString(getDataParams));

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response += line;
                }
            }
            else {

                response = "err: " + responseCode;

            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return response;
    }

    private String getDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for(Map.Entry<String, String> entry : params.entrySet()){

            if (first){
                first = false;
                result.append("?");
            }
            else{
                result.append("&");
            }

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

}
