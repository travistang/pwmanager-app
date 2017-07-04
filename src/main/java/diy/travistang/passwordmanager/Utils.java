package diy.travistang.passwordmanager;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.cert.Certificate;

/**
 * Created by travistang on 4/7/2017.
 */

public class Utils {

    public static void makeNoticeOnUiThread(Activity activity,String msg, int dur)
    {
        if (dur != Toast.LENGTH_LONG && dur != Toast.LENGTH_SHORT)
        {
            throw new IllegalArgumentException("duration must be either Toast.LENGTH_LONG or Toast.LENGTH_SHORT");
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity.getBaseContext(),msg,dur).show();
            }
        });
    }

    public static void setupSSL(WebSocketClient ws) throws Exception
    {

        SSLContext sc = SSLContext.getInstance("TLS");
        // trust all certificate: please ensure that it is run on VPN...
        sc.init(new KeyManager[0],new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }},new SecureRandom());
        ws.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sc));

    }

    public static boolean downloadFileFromInternet(Activity context,String targetUrl,String localFilename)
    {
        try {
            File caFile = new File(localFilename);
            URL url = new URL(targetUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            FileOutputStream fileoutput = context.openFileOutput(localFilename, Context.MODE_PRIVATE);
            InputStream input = connection.getInputStream();
            byte[] buffer = new byte[1024];
            int bufferLength = 0;
            while ((bufferLength = input.read(buffer)) > 0) {
                fileoutput.write(buffer, 0, bufferLength);
            }
            fileoutput.close();
            return true;
        }catch(Exception e)
        {
            return false;
        }
    }

    public static boolean hasFile(Activity context,String name)
    {
        return context.getFileStreamPath(name).exists();
    }
    @Nullable
    public static SSLContext getSSLSocketFactory(Activity activity,String certFile, String certName)
    {
        // from https://developer.android.com/training/articles/security-ssl.html#UnknownCa
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = activity.openFileInput(certFile);
            Certificate ca;
            ca = cf.generateCertificate(caInput);
            caInput.close();

            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry(certName, ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null,tmf.getTrustManagers(),null);
            return context;
        }catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
