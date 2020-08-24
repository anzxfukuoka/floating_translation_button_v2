package jp.anzx.stsclient;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class TextRec {

    private final static String TAG = "TextRec";

    private static String DATA_PATH;
    private static String DATA_DIR;

    private static String TESS_DIR = "/tessdata/";
    private static String TESS_EXT = ".traineddata";

    private static String baseDownloadURL = "https://github.com/tesseract-ocr/tessdata/blob/master/%s.traineddata?raw=true";

    private Context context;

    public TextRec(Context context){
        this.context = context;

        DATA_DIR = Environment.DIRECTORY_DOWNLOADS;
        DATA_PATH = Environment.getExternalStoragePublicDirectory(DATA_DIR).getPath();

        //Log.i(TAG, DATA_PATH);

        //создаем папку /tessdata
        File data_dir = new File(DATA_PATH + TESS_DIR);
        if(!data_dir.exists()){
            data_dir.mkdir();
        }
    }

    public String extractText(Bitmap bitmap, String lang)
    {

        File langFile = new File(DATA_PATH + TESS_DIR +  lang + TESS_EXT);
        Log.i(TAG, langFile.exists() + "");

        TessBaseAPI tessBaseApi = new TessBaseAPI();
        tessBaseApi.init(DATA_PATH, lang);
        tessBaseApi.setImage(bitmap);
        String extractedText = tessBaseApi.getUTF8Text();
        Log.i(TAG, extractedText);
        tessBaseApi.end();
        return extractedText;


    }

    public void downloadTessData(String lang){

        File langFile = new File(DATA_PATH + TESS_DIR +  lang + TESS_EXT);

        if(langFile.exists())
            return;

        String downloadURL = String.format(baseDownloadURL, lang);
        Log.i(TAG, downloadURL);

        DownloadManager downloadmanager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(downloadURL);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle("Data for " + lang);
        request.setDescription("Downloading...");//request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(DATA_DIR,TESS_DIR + lang + TESS_EXT);
        downloadmanager.enqueue(request);
    }

    public ArrayList<String> getDownloadedList(){

        ArrayList<String> list = new ArrayList<String>();

        File folder = new File(DATA_PATH + TESS_DIR);
        //File file = new File(path, "db.csv");

        //Log.i(TAG,folder.exists() + " " + folder.getPath());

        File[] listOfFiles = folder.listFiles();

        if(listOfFiles == null){
            Log.e(TAG, "listOfFiles == null");
            return null;
        }

        for (File file : listOfFiles) {
            if (file.isFile()) {
                Log.i(TAG, file.getName().split("[.]", 2)[0]);
                list.add(file.getName().split("[.]", 2)[0]);
            }
        }

        return list;

    }

    private void extractTessDataFiles(Context context){
        try{

            DATA_PATH = context.getFilesDir().getPath();
            Log.i(TAG, DATA_PATH);

            //создаем папку
            File data_dir = new File(DATA_PATH + "/tessdata");
            if(!data_dir.exists()){
                data_dir.mkdir();
            }

            //перемещаем файлы из assets в телефон
            String[] files = context.getAssets().list("tessdata");

            for(String filename : files){
                //считываем из assets
                byte[] buffer = null;
                InputStream is;

                is = context.getAssets().open("tessdata/" + filename);
                int size = is.available();
                buffer = new byte[size];
                is.read(buffer);
                is.close();

                //впихиваем в телефон
                FileOutputStream fos = new FileOutputStream(new File(DATA_PATH + "/tessdata", filename));
                fos.write(buffer);
                fos.close();

                Log.e(TAG, new File(DATA_PATH, filename).getPath());
            }

        }catch (IOException e){
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

    }
}
