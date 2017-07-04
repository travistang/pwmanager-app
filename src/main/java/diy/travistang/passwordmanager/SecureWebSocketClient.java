package diy.travistang.passwordmanager;

import android.util.Log;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;

import org.apache.http.message.BasicNameValuePair;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by travistang on 4/7/2017.
 */

public class SecureWebSocketClient extends WebSocketClient {

    private TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }
    };
    public SecureWebSocketClient(URI uri) {
        super(uri);
        try
        {
            setupSSL();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void setupSSL() throws Exception
    {
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null,this.trustAllCerts,new SecureRandom());
        this.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sc));
    }
    @Override
    public void onOpen(ServerHandshake handshakedata)
    {

    }

    @Override
    public void onMessage(String message)
    {

    }

    @Override
    public void onError(Exception e)
    {
        e.printStackTrace();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d("socket close","socket closed");
    }

}
