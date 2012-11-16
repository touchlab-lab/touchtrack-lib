package co.touchlab.ir.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import co.touchlab.ir.MemLog;
import co.touchlab.ir.process.UploadManagerService;
import co.touchlab.ir.util.InternalLog;
import co.touchlab.ir.util.PackageUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by IntelliJ IDEA.
 * User: kgalligan
 * Date: 1/22/12
 * Time: 3:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class UpdateDownloadActivity extends Activity
{
    private Handler handler;
    private AtomicLong remoteFileLength = new AtomicLong(-1l);
    private File localApkFile;
    private AtomicBoolean downloadDone = new AtomicBoolean(false);
    public static final java.lang.String BASE_URL = "http://192.168.1.3:8080";

    public static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_download);
        
        findViewById(R.id.startDownload).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                startDownload();
            }
        });

        findViewById(R.id.remindLater).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                finish();
            }
        });
        
        handler = new Handler();

        //noinspection unchecked
        new AsyncTask(){

            @Override
            protected Object doInBackground(Object... objects)
            {
                PackageInfo packageInfo = PackageUtils.loadPackageInfo(UpdateDownloadActivity.this);
                final Drawable appIcon = packageInfo.applicationInfo.loadIcon(getPackageManager());

                final CharSequence applicationLabel = getPackageManager().getApplicationLabel(packageInfo.applicationInfo);

                return new Object[]{appIcon, applicationLabel};
            }

            @Override
            protected void onPostExecute(Object o)
            {
                Object[] data = (Object[]) o;
                if(o != null)
                    ((ImageView)findViewById(R.id.appIcon)).setImageDrawable(((Drawable)data[0]));

                ((TextView)findViewById(R.id.appName)).setText((CharSequence)data[1]);
            }
        }.execute();

        //startDownload();
    }

    private void startDownload()
    {
        try
        {
            downloadApk();
        }
        catch (IOException e)
        {
            MemLog.e(getClass().getName(), e);
        }

        showProgressBar();

        Runnable rerunRunnable = new Runnable()
        {
            public void run()
            {
                updateDisplay();
                if(!downloadDone.get())
                    handler.postDelayed(this, 1000);
            }
        };

        handler.postDelayed(rerunRunnable, 1000);
    }

    private void showProgressBar()
    {
        findViewById(R.id.downloadProgress).setVisibility(View.VISIBLE);
        findViewById(R.id.buttons).setVisibility(View.GONE);
    }

    private void showButtons()
    {
        findViewById(R.id.downloadProgress).setVisibility(View.GONE);
        findViewById(R.id.buttons).setVisibility(View.VISIBLE);
    }

    private void updateDisplay()
    {
        if(localApkFile != null)
        {
            long current = localApkFile.length();
            long total = remoteFileLength.longValue();
            double prog = ((double) current) / ((double) total);
            ((ProgressBar)findViewById(R.id.downloadProgress)).setProgress((int) (prog * 100));
        }
    }

    private void downloadApk() throws IOException
    {
        String apkUrl = getIntent().getStringExtra(UploadManagerService.DOWNLOAD_LINK_KEY);
        final String installUrl = BASE_URL + "/s3d/" + apkUrl;

        new Thread(){
            @Override
            public void run()
            {
                try
                {
                    localApkFile = createDownloadFile();

                    FileOutputStream out = new FileOutputStream(localApkFile);

                    URL u = new URL(installUrl);
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();
                    c.setRequestMethod("GET");
                    c.setDoOutput(true);
                    c.connect();

                    String clString = c.getHeaderField("Content-Length");

                    try
                    {
                        remoteFileLength.set(Long.parseLong(clString));
                    }
                    catch (NumberFormatException e)
                    {
                        InternalLog.logExecption(e);
                    }

                    InputStream in = c.getInputStream();

                    IOUtils.copy(in, out);

                    in.close();
                    out.close();

                    handler.post(new Runnable()
                    {
                        public void run()
                        {
                            Uri myUrl = Uri.fromFile(localApkFile);
                            Intent update2 = new Intent(Intent.ACTION_VIEW).setDataAndType(myUrl, APK_MIME_TYPE);
                            startActivity( update2 );
                        }
                    });

                    downloadDone.set(true);
                    showButtons();
                }
                catch (IOException e)
                {
                    MemLog.e(getClass().getName(), e);
                }
            }
        }.start();
    }

    private File createDownloadFile()
    {
        File root;
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            root = Environment.getExternalStorageDirectory();
        else
            root = getFilesDir();

        return new File(root, getIntent().getStringExtra(UploadManagerService.DOWNLOAD_LINK_KEY));
    }

    public static void callMe(Context c)
    {
        c.startActivity(new Intent(c, UpdateDownloadActivity.class));
    }
}