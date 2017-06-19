package diy.travistang.passwordmanager;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import android.view.View;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.LayoutInflater;

/**
 * Created by travistang on 13/6/2017.
 */

public class PasswordArrayAdapter extends ArrayAdapter<Password> {

    public PasswordArrayAdapter(Context context, int textViewResourceId, ArrayList<Password> items) {
        super(context, textViewResourceId, items);
    }
    public View getView(int position, View convertView, ViewGroup parent) {

        // if the view has not been created before

        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.password_list_row, parent, false);

        }

        TextView nameView = (TextView)convertView.findViewById(R.id.passwordName);
        TextView pwLengthView = (TextView)convertView.findViewById(R.id.passwordLengthDesc);
        TextView pwConstraintDescView = (TextView)convertView.findViewById(R.id.passwordConstriantDesc);

        Password item = getItem(position);

        if (item != null) {
            nameView.setText(item.name);
            pwLengthView.setText(item.dateCreated);
            pwConstraintDescView.setText(item.description);
        }

        return convertView;
    }
}
