package diy.travistang.passwordmanager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.google.zxing.common.StringUtils;
import com.google.zxing.qrcode.encoder.QRCode;

/**
 * Created by travistang on 20/6/2017.
 */

public class QRScannerActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener {
    private QRCodeReaderView qrCodeReaderView;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanner_view);
        qrCodeReaderView = (QRCodeReaderView) findViewById(R.id.qrdecoderview);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
        {
            requestCameraPermission();
        }
        qrCodeReaderView.setOnQRCodeReadListener(this);

        qrCodeReaderView.setQRDecodingEnabled(true);
        qrCodeReaderView.setBackCamera();

    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        Log.d("QR Scanner",text);
        // validate QR codes
        // a valid QR code should contain a string of 256 alphanumeric characters

        boolean isValidToken = false;
        isValidToken &= (text.length() == 256);
        for(char c : text.toCharArray())
        {
            isValidToken &= Character.isLetterOrDigit(c);
        }

        if(isValidToken) {
            Intent intent = new Intent();
            intent.putExtra("QR", text);
            setResult(RESULT_OK, intent);
            finish();
        }else
        {
            // error message to the user: the qr found is not valid
            Toast.makeText(this.getApplicationContext(),"Invalid QR Code",Toast.LENGTH_SHORT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        qrCodeReaderView.startCamera();
        Log.d("QR","Showing toast");
        Toast.makeText(this.getBaseContext(),"Point the camera to the QR code shown on your password manager app to authorize this device. This page will go away upon a valid QR code is found or the back button is pressed.",Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onPause() {
        super.onPause();
        qrCodeReaderView.stopCamera();
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Snackbar.make(qrCodeReaderView, "Camera access is required to display the camera preview.",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override public void onClick(View view) {
                    ActivityCompat.requestPermissions(QRScannerActivity.this, new String[] {
                            Manifest.permission.CAMERA
                    }, 0);
                }
            }).show();
        } else {
            Snackbar.make(qrCodeReaderView, "Permission is not available. Requesting camera permission.",
                    Snackbar.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.CAMERA
            }, 0);
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                     @NonNull int[] grantResults) {
        if (requestCode != 0) {
            return;
        }

        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(qrCodeReaderView, "Camera permission was granted.", Snackbar.LENGTH_SHORT).show();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
