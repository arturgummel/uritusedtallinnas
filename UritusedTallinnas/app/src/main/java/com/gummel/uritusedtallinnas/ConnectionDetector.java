package com.gummel.uritusedtallinnas;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

//check Internet Connection
public class ConnectionDetector {
    private Context context;

    public ConnectionDetector(Context _context){
        this.context = _context;
    }
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}