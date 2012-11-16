package co.touchlab.ir.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.*;
import co.touchlab.ir.*;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: touchlab
 * Date: 11/9/12
 * Time: 5:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReportIssueActivity extends Activity {

    protected IssueReport issueReport = null;

    public static final boolean CANNED_SHOULD_SHOW_SCREENSHOT = true;
    public static final boolean CANNED_SHOULD_SHOW_RECORD_AUDIO = true;

    private boolean hasRecorded = false;
    private ProgressBar audioProgress;
    private Button audioControlBtn;
    private final AudioRecorder ar = new AudioRecorder();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        UserActionLog.activityCreated(this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_issue);

        if (getIntent() != null && getIntent().hasExtra(RobotDownActivity.ISSUE_REPORT))
        {
            issueReport = (IssueReport) getIntent().getSerializableExtra(RobotDownActivity.ISSUE_REPORT);
        }

        initControls();
    }

    @Override
    protected void onResume()
    {
        IssueReportHelper.logActivityResumed(this);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioControlBtn.getText().toString().equalsIgnoreCase("Stop") && ar != null){
            ar.stopRecording();
            ar.discard(this);
        }
    }

    protected void initControls()
    {
        //pull out the fields for the dialog.....
        final EditText input = (EditText) findViewById(R.id.issue_text);
        final CheckBox includeScreenshot = (CheckBox) findViewById(R.id.includeScreenshot);
        if (!CANNED_SHOULD_SHOW_SCREENSHOT)
            includeScreenshot.setVisibility(View.GONE);

        final CheckBox includeAudioclip = (CheckBox) findViewById(R.id.includeAudioclip);
        if (!CANNED_SHOULD_SHOW_RECORD_AUDIO)
            includeAudioclip.setVisibility(View.GONE);

        final LinearLayout audioPanel = (LinearLayout) findViewById(R.id.audioPanel);
        this.audioControlBtn = (Button) findViewById(R.id.audioControlBtn);
        final Button audioControlBtn = this.audioControlBtn;
        this.audioProgress = (ProgressBar) findViewById(R.id.audioProgress);
        this.audioProgress.setHorizontalScrollBarEnabled(true);
        //wire controls to the audio action button....
        audioControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (audioControlBtn.getText().toString().equalsIgnoreCase("Record") || audioControlBtn.getText().toString().equalsIgnoreCase("Re-Record")) {
                    if(hasRecorded){
                        rerecordBtnPressed(ReportIssueActivity.this);
                    }
                    else{
                        ar.startRecording(ReportIssueActivity.this);
                        restarttimebar();
                        audioControlBtn.setText("Stop");
                    }

                } else if (audioControlBtn.getText().toString().equalsIgnoreCase("Stop")) {
                    hasRecorded = true;
                    progressOn = false;
                    ar.stopRecording();
                    audioControlBtn.setText("Re-Record");
                }
            }
        });

        //toggles visibility of audio panel, triggers dialog to discard audio message, resets panel to start state
        includeAudioclip.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    stoptimebar();
                    audioPanel.setVisibility(View.VISIBLE);
                }
                else{
                    if(hasRecorded  || progressOn){
                        deselctAudioClip(ReportIssueActivity.this,audioPanel,includeAudioclip);
                    }else
                        audioPanel.setVisibility(View.GONE);
                }
            }
        });


        findViewById(R.id.send_report).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MemLog.ua(this.getClass().getSimpleName(), "ReportIssueActivity send clicked");
                //sendReport();
                String message = input.getText().toString();
                if (includeScreenshot.isChecked())
                    IssueReporter.saveScreenshot(ReportIssueActivity.this, "test_image", true, true);
                if (includeAudioclip.isChecked()) {
                    if (audioControlBtn.getText().toString().equalsIgnoreCase("Stop") && ar != null)
                        ar.stopRecording();
                    File af = new File(ReportIssueActivity.this.getFilesDir(), AUDIO_FILE_NAME);
                    if (af.exists() && af.length() > 0) {
                        try {
                            IssueReporter.saveFile(ReportIssueActivity.this, new FileInputStream(af), AUDIO_FILE_NAME, "3gp", false);
                        } catch (FileNotFoundException e) {
                        }
                    }
                }
                IssueReporter.sendIssueReport(ReportIssueActivity.this, true, message, null ,(IssueParam[]) null);
                finish();
            }
        });
    }

    private void rerecordBtnPressed(final Activity activity) {
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setTitle("Discard Message?");
        alert.setMessage("Discard Previously Recorded Message?");
        alert.setPositiveButton("Discard", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ar.startRecording(activity);
                restarttimebar();
                audioControlBtn.setText("Stop");
                hasRecorded = false;
            }
        });
        alert.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alert.create().show();
    }

    private void deselctAudioClip(final Activity activity, final LinearLayout audioPanel, final CheckBox includeAudioclip) {
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setTitle("Discard Message?");
        alert.setMessage("Discard Previously Recorded Message?");
        alert.setPositiveButton("Discard", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ar.stopRecording();
                ar.discard(activity);
                audioControlBtn.setText("Record");
                hasRecorded = false;
                stoptimebar();
                audioPanel.setVisibility(View.GONE);
            }

        });
        alert.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                includeAudioclip.setChecked(true);
            }
        });
        alert.create().show();
    }

    public static void callMe(final Activity context)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                final IssueReport issueReport = IssueReportHelper.prepareReportIssueManual(context, null);
                context.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        callMe(context, issueReport);
                    }
                });
            }
        }.start();
    }

    public static void callMe(Context context, IssueReport issueReport)
    {
        Intent intent = new Intent(context, ReportIssueActivity.class);
        intent.putExtra(RobotDownActivity.ISSUE_REPORT, issueReport);
        context.startActivity(intent);
    }

    private class AudioRecorder {

        private MediaRecorder mr = null;
        public void startRecording(Context context) {

            if (mr != null)
                stopRecording();

            File af = new File(context.getFilesDir(), AUDIO_FILE_NAME);
            if (af.exists())
                af.delete();

            FileOutputStream fs = null;
            try {
                fs = context.openFileOutput(AUDIO_FILE_NAME, 0);
            } catch (FileNotFoundException e) {
                if (fs == null)
                    return;
            }

            mr = new MediaRecorder();
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            try {
                mr.setOutputFile(fs.getFD());
            } catch (IOException e) {

            }

            try {
                mr.prepare();
            } catch (IOException e) {

            }
            mr.start();
        }

        public void stopRecording() {
            if (mr != null) {
                mr.stop();
                mr.release();
                mr = null;
            }
        }

        public void discard(Context context){
            File af = new File(context.getFilesDir(), AUDIO_FILE_NAME);
            if(af.exists())
                af.delete();
        }
    }

    public static String AUDIO_FILE_NAME = "audio_message.3gp";
    private boolean progressOn = false;

    public void restarttimebar() {
        progressOn = true;
        stoptimebar();
        for (int i = 1; i < (4 * 30); i++) {
            Message msg = timebarhandler.obtainMessage(0, i, 0);
            timebarhandler.sendMessageDelayed(msg, i * 250);
        }
    }

    public void stoptimebar() {
        timebarhandler.removeMessages(0);
        Message msg = timebarhandler.obtainMessage(0, 0, 0);
        timebarhandler.sendMessage(msg);
    }

    Handler timebarhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!progressOn) {
                if(msg.arg1 == 0 && !hasRecorded)
                    audioProgress.setProgress(0);
                return;
            }
            if (msg.arg1 == 0) {
                audioProgress.setProgress(0);
            }
            if (msg.arg1 > 0) {
                audioProgress.setProgress((int) ((float) msg.arg1 * .9));
            }
            if (msg.arg1 == 119) {
                ar.stopRecording();
                audioControlBtn.setText("Record");
            }
        }
    };


}
