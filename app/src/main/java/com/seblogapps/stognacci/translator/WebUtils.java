package com.seblogapps.stognacci.translator;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Created by stognacci on 10/05/2016.
 */
public class WebUtils {

    protected static boolean hasInternetConnection(Context context) {
        ConnectivityManager connectivityManager =
                ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));

        return connectivityManager != null
                && connectivityManager.getActiveNetworkInfo() != null
                && connectivityManager.getActiveNetworkInfo().isConnected();
    }
}
