package diy.travistang.passwordmanager;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import android.view.View;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.LayoutInflater;

/**
 * Created by travistang on 13/6/2017.
 */

public class PasswordArrayAdapter extends ArrayAdapter<Password> implements Filterable{

    public PasswordArrayAdapter(Context context, int textViewResourceId, ArrayList<Password> items) {
        super(context, textViewResourceId, items);
    }

    public void applyFilter(String text)
    {
        this.getFilter().filter(text);
    }
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                ArrayList<Password> filteredArray = new ArrayList<>();
                constraint = constraint.toString().toLowerCase();
                for(int i = 0; i < getCount(); i++)
                {
                    Password pw = getItem(i);
                    if(pw.name.toLowerCase().startsWith(constraint.toString()))
                        filteredArray.add(pw);
                }
                results.count = filteredArray.size();
                results.values = filteredArray;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                ArrayList<Password> list = ((PasswordListActivity)getContext()).getShownPasswordList();
                list.clear();
                list.addAll((ArrayList<Password>)results.values);
                notifyDataSetChanged();
            }
        };
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
