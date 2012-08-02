package ru.nekit.filelist.adapter;

import java.text.SimpleDateFormat;
import java.util.Date;

import ru.nekit.filelist.MainActivity.ItemVO;
import ru.nekit.filelist.R;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FileListAdapter extends ArrayAdapter<ItemVO> {

	Activity context;
	ItemVO[] dataSource;

	static class ViewHolder {
		public TextView title;
		public TextView description;
	}

	public FileListAdapter(Activity context, ItemVO[] objects) {
		super(context, R.layout.list_item, objects);
		this.dataSource = objects;
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater =  context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.list_item, null, true);
			holder = new ViewHolder();
			holder.title = (TextView) rowView.findViewWithTag("title");
			holder.description = (TextView) rowView.findViewWithTag("description");
			rowView.setTag(holder);
		} else {
			holder = (ViewHolder) rowView.getTag();
		}
		ItemVO item = dataSource[position];
		String name = item.file.getName();
		if( (item.flag & ItemVO.NOT_SHOW_EXTENSION) != 0 )
		{
			name = name.replaceFirst("[.][^.]+$", "");
		}
		holder.title.setText(name);
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm");
		Date date = new Date(dataSource[position].file.lastModified());
		holder.description.setText(sdf.format(date));
		return rowView;
	}
}