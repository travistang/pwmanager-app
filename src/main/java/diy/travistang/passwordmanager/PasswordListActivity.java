package diy.travistang.passwordmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
public class PasswordListActivity extends AppCompatActivity {
    private WebSocketClient socket;
    private ArrayList<Password> passwordList = new ArrayList<>();
    private PasswordArrayAdapter passwordListArrayAdapter;

    public ArrayList<Password> getPasswordByName(String pwName)
    {
        // TODO: how about removing a pw?
        ArrayList<Password> res = (ArrayList<Password>)passwordList.stream().filter(pw -> pw.name.equals(pwName)).collect(Collectors.toList());
        return res;
    }

    // TODO: return the "real" token from QR code
    public String getAuthenticateToken()
    {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        configureFloatingButton();
        configurePasswordListView();
        configureSwipeView();
        // connect the socket
        configureSocket();
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
                passwordListArrayAdapter.notifyDataSetChanged();
            }
        });
    }
    private void configureSocket()
    {
        this.socket  = new WebSocketClient(URI.create("ws://10.0.2.2:8888"))
        {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d("socket open","socket opened");
                passwordList.clear();
                socket.send("pw_list");
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
                            socket.close();
                            // inform the view that the refreshing has completed

                            cancelRefresh();
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

            }
        };
        socket.connect();
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
                configureSocket();
            }
        });
    }

    private void configureFloatingButton()
    {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

    }
    private void configurePasswordListView()
    {
        // configure the password list view
        ListView passwordListView = (ListView)findViewById(R.id.passwordListView);
        passwordListArrayAdapter = new PasswordArrayAdapter(this, R.layout.password_list_row,passwordList);
        //TODO: authenticate this device
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
