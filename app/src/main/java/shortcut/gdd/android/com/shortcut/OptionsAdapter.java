package shortcut.gdd.android.com.shortcut;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Samuel PC on 12/04/2015.
 */
public class OptionsAdapter extends ArrayAdapter<Options> {

    private static class ViewHolder {
        TextView title;
        CheckBox defaultValue;
        ImageButton tag;
    }

    public OptionsAdapter(Context context, ArrayList<Options> options) {
        super(context, 0, options);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Options option = getItem(position);

        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item_shortcut, parent, false);
            viewHolder.title = (TextView) convertView.findViewById(R.id.list_item_text);
            viewHolder.defaultValue = (CheckBox) convertView.findViewById(R.id.list_item_checkbox);
            viewHolder.tag = (ImageButton) convertView.findViewById(R.id.list_item_imagebutton);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data into the template view using the data object
        viewHolder.title.setText(option.title);
        viewHolder.defaultValue.setChecked(option.defaultValue);
        viewHolder.tag.setTag(option.tag);
        // Return the completed view to render on screen
        return convertView;
    }
}
