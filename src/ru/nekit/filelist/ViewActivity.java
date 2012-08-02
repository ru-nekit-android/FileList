package ru.nekit.filelist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import ru.nekit.filelist.MainActivity.ItemVO;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;

public class ViewActivity extends SherlockActivity {

	static final int TEXT_READER = 1;
	static final int WEBVIEW_READER = 2;
	static final String PDF = "pdf"; 

	WebView webView;
	ItemVO item;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Sherlock___Theme_DarkActionBar);
		//Used for theme switching in samples
		//requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//Load partially transparent black background
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		webView= (WebView)findViewById(R.id.webView);
		Intent intent = getIntent();
		item = intent.getParcelableExtra(ItemVO.name);
		webView.setWebChromeClient(new mWebViewClient());
		WebSettings ws = webView.getSettings();
		ws.setBuiltInZoomControls(true);
		ws.setAllowFileAccess(true);
		updateView();
	}

	/*@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_view);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


	}*/

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		finish();
		return true;
	};

	private void updateView()
	{
		displayContentByType(WEBVIEW_READER);
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onRestart()
	{
		super.onRestart();
	}

	private void displayContentByType(int type)
	{
		switch(type)
		{
		case TEXT_READER:

			String summary = "<!Doctype html><html><head><meta charset=utf-8></head><body>" + readTextFile(item.file) + "</body></html>";
			webView.loadData(summary, "text/html", "utf-8");
			break;

		case WEBVIEW_READER:

			String extension = MimeTypeMap.getFileExtensionFromUrl(item.file.getPath());

			Uri uri = Uri.fromFile(item.file);
			String path = uri.toString();
			String mimeType =  MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			if( PDF.equals(extension) )
			{
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(uri, mimeType);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				Boolean canResolveIntent = getPackageManager().resolveActivity(intent, 0) != null;
				if( canResolveIntent ) 
				{
					startActivity(Intent.createChooser(intent, "Открыть " + PDF));
					finish();
				} 
				else{
					Toast.makeText(this, "No Application Available to View PDF", Toast.LENGTH_LONG).show();
				}
			}else
			{
				webView.loadUrl(path);
			}

			break;	

		default:
			break;

		}
	}

	private String readTextFile(File file) {

		StringBuffer contents = new StringBuffer();
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(file));
			String text = null;
			while ((text = reader.readLine()) != null) {
				contents.append(text)
				.append(System.getProperty(
						"line.separator"));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return contents.toString();
	}

	static final private class mWebViewClient extends WebChromeClient
	{

	}

}