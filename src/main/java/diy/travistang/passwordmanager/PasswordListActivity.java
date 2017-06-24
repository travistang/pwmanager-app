package diy.travistang.passwordmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.net.Uri;

import com.gordonwong.materialsheetfab.MaterialSheetFab;
import com.gordonwong.materialsheetfab.MaterialSheetFabEventListener;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.callback.HttpConnectCallback;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
public class PasswordListActivity extends AppCompatActivity {
    private WebSocketClient socket;
    private ArrayList<Password> passwordList = new ArrayList<>();
    private ArrayList<Password> shownPasswordList = new ArrayList<>(); // this one is for holding the filter result

    private PasswordArrayAdapter passwordListArrayAdapter;
    private String filterString = "";

    private Timer timeoutTimer;
    private static int CAMERA_REQUEST_CODE = 1;
    private boolean isFABOpen = false;
    private static final String AUTH_TOKEN_FILENAME = "auth_token";
    private static final String HOST_URL_FILENAME = "host_url";

    private void setServerBaseURL(String url)
    {
        try {
            FileOutputStream file = openFileOutput(HOST_URL_FILENAME, MODE_PRIVATE);
            file.write(url.getBytes());
            file.close();
        }catch(FileNotFoundException e)
        {
            // does that matter?
            e.printStackTrace();
        }catch(IOException e)
        {
            Toast.makeText(getBaseContext(),"Unable to save token to device",Toast.LENGTH_SHORT).show();
        }
    }

    public String getServerBaseURL()
    {
        try {
            FileInputStream file = openFileInput(HOST_URL_FILENAME);
            DataInputStream in = new DataInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String token = br.readLine();
            file.close();
            return token;
        }catch(FileNotFoundException e)
        {
            return null;
        }catch(IOException e)
        {
            // the file is empty
            return null;
        }
    }

    private void setAuthenticationToken(String token)
    {
        try {
            FileOutputStream file = openFileOutput(AUTH_TOKEN_FILENAME, MODE_PRIVATE);
            file.write(token.getBytes());
            file.close();
        }catch(FileNotFoundException e)
        {
            // does that matter?
            e.printStackTrace();
        }catch(IOException e)
        {
            Toast.makeText(getBaseContext(),"Unable to save token to device",Toast.LENGTH_SHORT).show();
        }
    }

    public ArrayList<Password> getShownPasswordList()
    {
        return shownPasswordList;
    }
    public String getAuthenticationToken()
    {
        try {
            FileInputStream file = openFileInput(AUTH_TOKEN_FILENAME);
            DataInputStream in = new DataInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String token = br.readLine();
            file.close();
            return token;
        }catch(FileNotFoundException e)
        {
            return null;
        }catch(IOException e)
        {
            // the file is empty
            return null;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        configureFloatingButton();
        configurePasswordListView();

        configureSocket();

    }
    @Override
    public void setContentView(int layout)
    {
        super.setContentView(layout);
        configureSwipeView();
    }

    private void cancelRefresh()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                SwipeRefreshLayout layout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
                layout.setRefreshing(false);
            }
        });
    }
    public void clearPasswordList()
    {
        passwordList.clear();
    }

    // everything related to websocket.
    // if either token or host url is not obtained this method will do nothing.
    private void configureSocket()
    {
        // synthesise socket
        String token = getAuthenticationToken();
        String hostUrl = getServerBaseURL();
        if(token == null || hostUrl == null) {
            Toast.makeText(getBaseContext(), "Cannot connect to host. Either the host or the authentication token is not defined. Please enter the host URL and scan the token provided",Toast.LENGTH_LONG).show();
            return;
        }

        if(timeoutTimer != null) {
            timeoutTimer.cancel();
            timeoutTimer = null;
        }
        timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(),"Unable to connect to server. Please try again later",Toast.LENGTH_SHORT).show();
                        cancelRefresh();
                    }
                });
                socket.close();
            }
        },6000);
        this.socket  = new WebSocketClient(URI.create(String.format("ws://%s/pwmanager/socket/%s", hostUrl,token)))
        {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d("socket open","socket opened");
                SocketAction.getPasswordList(socket,PasswordListActivity.this);
                timeoutTimer.cancel();
                timeoutTimer = null;
            }

            @Override
            public void onMessage(String message) {
                Log.d("socket message", message);
                try
                {
                    JSONObject msg = new JSONObject(message);
                    if (msg.has("action"))
                    {
                        String action = msg.getString("action");
                        if(action.equals("done"))
                        {
//                            socket.close();
                            // inform the view that the refreshing has completed
                            if (passwordList.isEmpty())
                                Snackbar.make(findViewById(R.id.swipe_container),"You have no password",Snackbar.LENGTH_INDEFINITE).show();
                            cancelRefresh();
                        }else if (action.equals("deleted"))
                        {
                            // TODO: handle deleting cases
                            String pwName = msg.getString("name");
                            for(Password pw : passwordList)
                            {
                                if(pw.name.equals(pwName))
                                {
                                    passwordList.remove(pw);
                                }
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Snackbar.make(findViewById(R.id.swipe_container),String.format("Password %s is deleted",pwName),Snackbar.LENGTH_SHORT);
                                    passwordListArrayAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }else
                    {
                        // new password
                        String password = msg.getString("password");
                        String desc = msg.getString("description");
                        String name = msg.getString("name");
                        String date = msg.getString("date_created");

                        Password pw = new Password();
                        pw.password = password;
                        pw.description = desc;
                        pw.name = name;
                        pw.dateCreated = date;

                        passwordList.add(pw);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                applyPasswordFilter();
                            }
                        });
                    }
                }catch(Exception e)
                {
                    e.printStackTrace();
                }

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d("socket close","socket closed");
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };
        socket.connect();
    }

    private void applyPasswordFilter()
    {
        shownPasswordList.clear();
        shownPasswordList.addAll(passwordList);
        if(filterString.length() > 0)
        {
            passwordListArrayAdapter.applyFilter(filterString);
        }
        else
        {
            passwordListArrayAdapter.notifyDataSetChanged();
        }
    }

    private void configureSwipeView()
    {
        SwipeRefreshLayout layout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);

        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // TODO: really?
                if (socket.getConnection().isConnecting())
                {
                    socket.close();
                }
                // TODO: handle socket connection prerequisite
                configureSocket();
            }
        });
    }

    @Override
    public void onBackPressed()
    {
        if(this.findViewById(R.id.toolbar) == null)
        {
            // inside the scanner
            setContentView(R.layout.activity_password_list);
        }else if(this.isFABOpen)
        {
            // simulate the FAB click to close the FAB menu. That is probably what the user wants
            this.findViewById(R.id.fab).performClick();
        }else {
            // default behaviour
            super.onBackPressed();
        }
    }
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data)
    {
        if(requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK)
        {
            String qr = data.getStringExtra("QR");
            Log.d("Password Activity", "got qr:" + qr);

            String postURL = String.format("http://%s/pwmanager/authorize/%s", getServerBaseURL(), qr);
            AsyncHttpClient.getDefaultInstance().executeJSONObject(new AsyncHttpGet(postURL), new AsyncHttpClient.JSONObjectCallback()
            {
                @Override
                public void onCompleted(Exception e, AsyncHttpResponse source, JSONObject result)
                {
                    try
                    {
                        if (result.getString("status").equals("ok"))
                        {
                            Log.d("QR Scanner","Authenticated");
                            PasswordListActivity.this.setAuthenticationToken(result.getString("token"));
                            Toast.makeText(PasswordListActivity.this.getBaseContext(),"Device authorized.",Toast.LENGTH_SHORT);
                            configureSocket();
                        }else
                        {
                            Toast.makeText(PasswordListActivity.this.getBaseContext(),"Unable to authenticate this device",Toast.LENGTH_SHORT).show();
                        }

                    }catch(JSONException je)
                    {
                        Toast.makeText(PasswordListActivity.this.getBaseContext(),"Error response from server",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void configureFloatingButton()
    {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        FloatingActionButton url_button = (FloatingActionButton) findViewById(R.id.url_button);
        FloatingActionButton scan_button = (FloatingActionButton) findViewById(R.id.scan_button);
        FloatingActionButton filter_button = (FloatingActionButton) findViewById(R.id.filter_button);
        ArrayList<FloatingActionButton> buttons = new ArrayList<>();
        buttons.add(url_button);
        buttons.add(scan_button);
        buttons.add(filter_button);
        filter_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = "Search for password";
                AlertDialog.Builder builder = new AlertDialog.Builder(PasswordListActivity.this);
                builder.setTitle(msg);
                final EditText input = new EditText(builder.getContext());
                builder.setView(input);
                input.setText(filterString);
                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        filterString = s.toString();
                        applyPasswordFilter();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
            }
        });
        url_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String msg = "Enter the URL or IP of the machine running your password manager web application";
                AlertDialog.Builder builder = new AlertDialog.Builder(PasswordListActivity.this);
                builder.setTitle(msg);
                final EditText input = new EditText(builder.getContext());
                builder.setView(input);
                input.setText(PasswordListActivity.this.getServerBaseURL());
                builder.setPositiveButton("Set URL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = input.getText().toString();

                        // TODO: validate url
                        PasswordListActivity.this.setServerBaseURL(url);
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });
        scan_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check that URL is provided before scanning QR code (or post request cannot be performed)
                if (getServerBaseURL() == null)
                {
                    Toast.makeText(PasswordListActivity.this.getBaseContext(),"Please provide server URL before scanning QR code",Toast.LENGTH_LONG).show();
                    return;
                }
                Intent qrTransitionIntent = new Intent(getApplicationContext(),QRScannerActivity.class);
                startActivityForResult(qrTransitionIntent,CAMERA_REQUEST_CODE);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFABOpen)
                {
                    Animation fab_close = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_close);
                    for(FloatingActionButton b : buttons)
                    {
                      b.startAnimation(fab_close);
                      b.setVisibility(View.INVISIBLE);
                      b.setClickable(false);
                    }

                    isFABOpen = false;
                    Log.d("FAB","FAB closed");
                }else
                {
                    Animation fab_open = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_open);
                    for(FloatingActionButton b : buttons)
                    {
                      b.startAnimation(fab_open);
                      b.setVisibility(View.VISIBLE);
                      b.setClickable(true);
                    }
                    isFABOpen = true;
                    Log.d("FAB","FAB opened");
                }
            }
        });

    }
    private void configurePasswordListView()
    {
        // configure the password list view
        ListView passwordListView = (ListView)findViewById(R.id.passwordListView);
        passwordListArrayAdapter = new PasswordArrayAdapter(this, R.layout.password_list_row,shownPasswordList);

        passwordListView.setAdapter(passwordListArrayAdapter);
        passwordListArrayAdapter.notifyDataSetChanged();

        passwordListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?>adapter,View v, int position,long arg3){
                Password item = (Password)adapter.getItemAtPosition(position);
                String pw = item.password;
                String name = item.name;

                // click to copy to pasteboard

                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(ClipData.newPlainText(pw,pw));

                Toast toast = Toast.makeText(getApplicationContext(),
                        String.format("Password for %s copied",name),
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_password_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
