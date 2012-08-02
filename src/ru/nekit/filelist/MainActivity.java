package ru.nekit.filelist;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import ru.nekit.filelist.adapter.FileListAdapter;
import ru.nekit.filelist.adapter.SectionListAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

public class MainActivity extends SherlockActivity implements OnItemClickListener, OnItemLongClickListener{

	static final int SORT_BY_NAME = 1;
	static final int SORT_BY_DATE = 2;
	static final int SORT_BY_SIZE = 3;
	static final int SORT_BY_EXTENSION = 4;
	static final int INTERNAL_SORT_BY_EXTENSION_AND_NAME = 5;
	static final String[] EXTENSION_RULE = new String[]{"png", "txt", "pdf", "jpeg", "jpg", "html", "htm", "doc", "gif"};
	static final String MAIN_PATH = "ru.nekit.FileList";

	private static File[] fileList;
	private int currentSortType;
	private File mainFolder;

	private ListView fileListView;
	private ArrayList<ItemVO> dataSource;
	private Map<String, ArrayList<ItemVO>> sectionDataSource;
	private ListAdapter listAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Sherlock___Theme_DarkActionBar);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		fileListView = (ListView)findViewById(R.id.fileListView);

		Boolean canRead 	= false;
		String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
		mainFolder 			= new File(extStorageDirectory, MAIN_PATH);
		dataSource 			= new ArrayList<MainActivity.ItemVO>();
		sectionDataSource 	= new TreeMap<String, ArrayList<ItemVO>>();
		if( isSDCardReady() )
		{
			if( !mainFolder.exists() )
			{
				File parent = new File(extStorageDirectory);
				if( parent != null && parent.canWrite() )
				{
					mainFolder.mkdir();
				}
				else
				{
				}
			}else
			{
				canRead = true;
			}
		}else if( isSDCardShared() )
		{
			if( mainFolder.exists() )
			{
				canRead = true;
			}
		}else
		{
		}
		if( canRead )
		{
			readDirectory(SORT_BY_EXTENSION);
		}
		fileListView.setOnItemClickListener(this);
		registerForContextMenu(fileListView);
		fileListView.setOnItemLongClickListener(this);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		SubMenu subMenu = menu.addSubMenu(0, -1, 0, null);
		subMenu.setIcon(R.drawable.sort_list);
		MenuItem head = subMenu.add(0, 0, 0, "Сортировать по...");
		head.setEnabled(false);
		subMenu.add(1, SORT_BY_NAME, 0, "Имени");
		subMenu.add(1, SORT_BY_DATE, 0, "Дате создания");
		subMenu.add(1, SORT_BY_EXTENSION, 0, "Типу");
		subMenu.setGroupCheckable(1, true, true);
		MenuItem subMenuItem = subMenu.getItem();
		subMenuItem.setTitle("Сортировать");
		MenuItem checkedItem =subMenu.findItem(currentSortType);
		checkedItem.setChecked(true);
		subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int type = item.getItemId();
		if ( type == -1) 
		{
			return false;
		}
		switch( type )
		{

		case SORT_BY_NAME:
		case SORT_BY_DATE:
		case SORT_BY_EXTENSION:
			item.setChecked(true);
			if( fileList == null || fileList.length == 0 )
			{
				readDirectory(type);
			}
			executeSort(type);
			break;
		default:
			break;
		}

		return true;
	}

	private Intent createIntent(ItemVO item)
	{
		Intent intent = new Intent(this, ViewActivity.class);
		intent.putExtra(ItemVO.name, item);
		return intent;
	}

	private void readDirectory(int sortType)
	{
		fileList = mainFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				String extension = MimeTypeMap.getFileExtensionFromUrl(file.getPath());
				return file.isFile() && !file.isHidden() && Arrays.asList(EXTENSION_RULE).indexOf(extension.toLowerCase()) != -1;
			}
		});
		if( fileList != null )
		{
			executeSort(sortType);
		}
	}

	private void executeSort(int sortType)
	{
		if( currentSortType != sortType )
		{
			currentSortType = sortType;
			SortFileListTask task = new SortFileListTask();
			task.execute(sortType);
			try {
				listAdapter = task.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			fileListView.setAdapter(listAdapter);
		}
	}

	class SortFileListTask extends AsyncTask<Integer, Void, ListAdapter> {

		@Override
		protected ListAdapter doInBackground(Integer... params) {
			int type = params[0];
			return sortFileListBy(type);
		}

		private ListAdapter sortFileListBy(int type)
		{
			ListAdapter adapter = null;
			dataSource.clear();
			sectionDataSource.clear();
			if( type != SORT_BY_EXTENSION )
			{
				sortBy(fileList, type);
				for( File file : fileList )
				{
					dataSource.add(new ItemVO(file));
				}
				adapter = new FileListAdapter(MainActivity.this, dataSource.toArray(new ItemVO[dataSource.size()]));
			}
			else
			{
				sortBy(fileList, INTERNAL_SORT_BY_EXTENSION_AND_NAME);
				adapter = new SectionListAdapter(MainActivity.this);
				SectionListAdapter sectionAdapter = (SectionListAdapter)adapter;
				Set<String> extensionSet = new HashSet<String>();
				for( File file : fileList )
				{
					ItemVO item = new ItemVO(file, ItemVO.NOT_SHOW_EXTENSION);
					dataSource.add(item);
					String currentExtension = MimeTypeMap.getFileExtensionFromUrl(file.getPath());
					currentExtension = currentExtension.toLowerCase();
					ArrayList<ItemVO> list = sectionDataSource.get(currentExtension);
					if( list == null )
					{
						sectionDataSource.put(currentExtension, list = new ArrayList<ItemVO>());
						extensionSet.add(currentExtension);
					}
					list.add(item);
				}
				Set<Entry<String, ArrayList<ItemVO>>> iterator = sectionDataSource.entrySet();
				for( Entry<String, ArrayList<ItemVO>> entity :  iterator )
				{
					ArrayList<ItemVO> entityValue = entity.getValue();
					sectionAdapter.addSection(entity.getKey(), new FileListAdapter(MainActivity.this, entityValue.toArray(new ItemVO[entityValue.size()])));
				}
			}
			return adapter;
		}

		private void sortBy( File[] list, int type )
		{
			Comparator<? super File> comparator;
			switch( type )
			{
			case SORT_BY_NAME:

				comparator = new Comparator<File>(){
					public int compare(File file1, File file2) {
						return String.valueOf(file1.getName()).toLowerCase().compareTo(file2.getName().toLowerCase());
					};
				};

				break;

			case SORT_BY_DATE:

				comparator =  new Comparator<File>(){
					public int compare(File file1, File file2) {
						return new Date(file1.lastModified()).compareTo(new Date(file2.lastModified()));
					};
				};

				break;

			case SORT_BY_SIZE:

				comparator =  new Comparator<File>(){
					public int compare(File file1, File file2) {
						long fl1 = file1.length();
						long fl2 = file2.length();
						if( fl1 > fl2 )
						{
							return 1;
						}
						if( fl1 < fl2 )
						{
							return -1;
						}
						return 0;
					};
				};

				break;

			case SORT_BY_EXTENSION:

				comparator =  new Comparator<File>(){
					public int compare(File file1, File file2) {

						String extension1 = MimeTypeMap.getFileExtensionFromUrl(file1.getPath());
						String extension2 = MimeTypeMap.getFileExtensionFromUrl(file2.getPath());

						if( extension1 == null )
						{
							extension1 = "";
						}
						if( extension2 == null )
						{
							extension2 = "";
						}
						return extension1.compareTo(extension2);
					};
				};

				break;

			case INTERNAL_SORT_BY_EXTENSION_AND_NAME:

				comparator =  new Comparator<File>(){
					public int compare(File file1, File file2)
					{
						String extension1 = MimeTypeMap.getFileExtensionFromUrl(file1.getPath());
						String extension2 = MimeTypeMap.getFileExtensionFromUrl(file2.getPath());
						if( extension1 == null )
						{
							extension1 = "";
						}
						if( extension2 == null )
						{
							extension2 = "";
						}
						int result = extension1.compareTo(extension2);
						if( result == 0 )
						{
							result = String.valueOf(file1.getName()).compareTo(file2.getName());
						}
						return result;
					};
				};

				break;

			default:
				throw new IllegalArgumentException();
			}
			Arrays.sort(list, comparator);
		}

	}

	private boolean isSDCardReady() 
	{
		return (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED));
	}

	private boolean isSDCardShared() 
	{
		return (Environment.getExternalStorageState().equals(Environment.MEDIA_SHARED));
	}

	static public class ItemVO implements Parcelable
	{

		public static int NOT_SHOW_EXTENSION = 1;

		public File file;
		public int flag = 0;

		static public String name = "ItemVO";

		public ItemVO(File file)
		{
			this.file = file;
		}

		public ItemVO(File file, int flag)
		{
			this.file = file;
			this.flag = flag;
		}

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel parcel, int flags) {
			parcel.writeValue(file);
			parcel.writeInt(flag);
		}

		public static final Parcelable.Creator<ItemVO> CREATOR = new Parcelable.Creator<ItemVO>() {
			public ItemVO createFromParcel(Parcel in) {
				return new ItemVO(in);
			}

			public ItemVO[] newArray(int size) {
				return new ItemVO[size];
			}
		};

		private ItemVO(Parcel parcel) {
			file = (File) parcel.readValue(File.class.getClassLoader());
			flag =  parcel.readInt();
		}

	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int index, long i) 
	{
		ItemVO item = (ItemVO)adapter.getItemAtPosition(index);
		startActivity(createIntent(item));
	}

	@Override
	public boolean onItemLongClick(final AdapterView<?> adapter, View view, final int index, long i) {
		AlertDialog dialog = (new AlertDialog.Builder(this)
		.setTitle("Выберите действие")
		.setItems(new String[]{"Открыть в..."}
		, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				ItemVO item;
				switch( id )
				{

				case 0:

					item = (ItemVO)adapter.getItemAtPosition(index);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(item.file), MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(item.file.getAbsolutePath())));
					startActivity(Intent.createChooser(intent, "Share"));

					break;


				default:
					break;
				}
				dialog.dismiss();
			}
		})
		.setCancelable(true)
		.create());
		dialog.show();
		return false;
	}
}