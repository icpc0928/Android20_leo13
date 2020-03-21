package tw.org.iii.leo.leo13;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class MainActivity extends AppCompatActivity {
    private ConnectivityManager cmgr;
    private MyReceiver myReceiver;
    private TextView mesg;
    private ImageView img;
    private boolean isAllowSDCard;
    private File downloadDir;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    123);


        }else{
            isAllowSDCard = true;
            init();
        }
    }

    private void init(){

        if(isAllowSDCard){
            downloadDir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Alart");
        progressDialog.setMessage("Downloading...");


        mesg = findViewById(R.id.mesg);
        img = findViewById(R.id.img);

        cmgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        //目前只有一個Act所以寫在onCreate 如果有兩個畫面就要寫在onStart onPause 自己判斷
        myReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION); //Action
//        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);// 如果有多個就用這招 一直添加下去讓它過濾
        filter.addAction("leo");
        registerReceiver(myReceiver,filter);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isAllowSDCard = true;
        }else{
            isAllowSDCard = false;
        }
        init();
    }

    @Override
    public void finish() {
        unregisterReceiver(myReceiver);
        super.finish();
    }

    private boolean isConnectNetwork(){
        NetworkInfo networkInfo = cmgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();

    }

    private boolean isWifiConnected(){
        NetworkInfo networkInfo = cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo.isConnected();
    }


    public void test1(View view) {
        Log.v("leo","isNetwork = " + isConnectNetwork());
    }
    public void test2(View view) {
        Log.v("leo","isWifi = " + isWifiConnected());
    }

    public void test3(View view) {
        new Thread(){
            @Override
            public void run() {
                try {
                    URL url = new URL("https://bradchao.com/wp");  //http => usesCleartextTraffic="true"
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.connect();

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(conn.getInputStream()));
                    String line; StringBuffer sb = new StringBuffer();
                    while((line = reader.readLine()) != null){
                        sb.append(line + "\n");

                    }
                    reader.close();
                    //把廣播發出去
                    Intent intent = new Intent("leo");
                    intent.putExtra("data",sb.toString());
                    sendBroadcast(intent);  //Context => Activity , Service , Application 都是context 都可以發廣播


                } catch (Exception e) {
                    Log.v("leo",e.toString());
                }

            }
        }.start();

    }
    public void test4(View view){
        new Thread(){
            @Override
            public void run() {
                fetchImage();
            }
        }.start();
    }

    public void test5(View view) {
    if(!isAllowSDCard)return;

        progressDialog.show();

        new Thread(){
            @Override
            public void run() {
                fetchPDF();
            }
        }.start();
    }
    private void fetchPDF(){
        try{
            URL url = new URL("https://pdfmyurl.com/?url=https://www.gamer.com.tw");
            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            conn.connect();

            File downloadFile = new File(downloadDir,"gamer.pdf");
            FileOutputStream fout = new FileOutputStream(downloadFile);

            byte[] buf = new byte[1024*4096];
            BufferedInputStream bin = new BufferedInputStream(conn.getInputStream());

            int len = -1;
            while( ( len = bin.read(buf)) != -1 ){
                fout.write(buf,0,len);
            }

            bin.close();
            fout.flush();
            fout.close();

            uiHandler.sendEmptyMessage(2);

            Log.v("leo","save OK");


        }catch(Exception e ){
            Log.v("leo",e.toString());
        }finally {
            uiHandler.sendEmptyMessage(1);
        }
    }



    private Bitmap bmp;
    private void fetchImage(){
        try{
            URL url = new URL("https://s1.yimg.com/uu/api/res/1.2/Celm7WD.nwzHJqxb71O.lA--~B/Zmk9dWxjcm9wO2N3PTEyMTU7ZHg9MDtjaD02ODM7ZHk9MDt3PTM5MjtoPTMwODtjcj0xO2FwcGlkPXl0YWNoeW9u/https://media.zenfs.com/ko/udn.com/37abcc294d0f64591abeceb4a23e4801");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.connect();

            //1. 我會取得conn.getInputStream()
            //2. 我要顯示到ImageView
            bmp = BitmapFactory.decodeStream(conn.getInputStream());
            uiHandler.sendEmptyMessage(0);


        }
        catch (Exception e){

        }
    }

    private UIHandler uiHandler = new UIHandler();



    private class UIHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {

            super.handleMessage(msg);
            if (msg.what == 0) img.setImageBitmap(bmp);
            if (msg.what==1)progressDialog.dismiss();
            if (msg.what == 2 ) showPDF();
        }
    }


    private void showPDF(){
        File file = new File(downloadDir,"gamer.pdf");
        Uri pdfuri= FileProvider.getUriForFile(this,getPackageName()+".provider",file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfuri,"application/pdf");
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(intent);
    }



    private class MyReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
           // Log.v("leo","onReceive");
            if(intent.getAction().equals("leo")){
                String data = intent.getStringExtra("data");
                mesg.setText(data);

            }else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                test1(null);
            }




        }
    }

}


