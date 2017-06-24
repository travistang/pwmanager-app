package diy.travistang.passwordmanager;

import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by travistang on 24/6/2017.
 */

public class SocketAction {
    public static void getPasswordList(WebSocketClient socket,PasswordListActivity activity)
    {
        activity.clearPasswordList();
        try {
            JSONObject pw_list = new JSONObject();
            pw_list.put("token", activity.getAuthenticationToken());
            pw_list.put("action","pw_list");
            socket.send(pw_list.toString());
        }catch(JSONException e)
        {
            Toast.makeText(activity.getBaseContext(),"Problem occured when communicating with server",Toast.LENGTH_SHORT);
        }
    }
}
