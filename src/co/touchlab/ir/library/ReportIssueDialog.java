package co.touchlab.ir.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import co.touchlab.ir.IssueParam;
import co.touchlab.ir.IssueReporter;


import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: brianplummer
 * Date: 1/10/12
 * Time: 3:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReportIssueDialog {

    public static final String CANNED_TITLE = "Report Issue";
    public static final String CANNED_MESSAGE = "Report issue will send an error log and other info to our admins.";
    public static final boolean CANNED_SHOULD_SHOW_SCREENSHOT = true;
    public static final boolean CANNED_SHOULD_SHOW_RECORD_AUDIO = true;


    public static ReportIssueDialog getInstance() {
        return new ReportIssueDialog();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean hasRecorded = false;
    private ProgressBar audioProgress;
    private Button audioControlBtn;
    private final AudioRecorder ar = new AudioRecorder();


    public void showBasicDialog(final Activity a) {
        showReportDialog(a, CANNED_TITLE, CANNED_MESSAGE, CANNED_SHOULD_SHOW_SCREENSHOT, CANNED_SHOULD_SHOW_RECORD_AUDIO);
    }

    public void showReportDialog(final Activity activity, String title, String message, boolean includeScreenshotOption, boolean includeRecordAudioOption) {


        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setTitle(title);
        alert.setMessage(message);
        View issueReportView = LayoutInflater.from(activity).inflate(R.layout.report_issue_dialog, null);

        final EditText input = (EditText) issueReportView.findViewById(R.id.issueText);
        final CheckBox includeScreenshot = (CheckBox) issueReportView.findViewById(R.id.includeScreenshot);
        if (!includeScreenshotOption)
            includeScreenshot.setVisibility(View.GONE);

        final CheckBox includeAudioclip = (CheckBox) issueReportView.findViewById(R.id.includeAudioclip);
        if (!includeRecordAudioOption)
            includeAudioclip.setVisibility(View.GONE);

        final LinearLayout audioPanel = (LinearLayout) issueReportView.findViewById(R.id.audioPanel);
        this.audioControlBtn = (Button) issueReportView.findViewById(R.id.audioControlBtn);
        final Button audioControlBtn = this.audioControlBtn;
        this.audioProgress = (ProgressBar) issueReportView.findViewById(R.id.audioProgress);
        this.audioProgress.setHorizontalScrollBarEnabled(true);

        audioControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (audioControlBtn.getText().toString().equalsIgnoreCase("Record") || audioControlBtn.getText().toString().equalsIgnoreCase("Re-Record")) {

                    if(hasRecorded){
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setTitle("Discard Message?");
                        alert.setMessage("Discard Previously Recorded Message?");
                        alert.setPositiveButton("Discard",new DialogInterface.OnClickListener() {
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
                    else{
                        ar.startRecording(activity);
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

        includeAudioclip.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    stoptimebar();
                    audioPanel.setVisibility(View.VISIBLE);
                }
                else{
                     if(hasRecorded  || progressOn){
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setTitle("Discard Message?");
                        alert.setMessage("Discard Previously Recorded Message?");
                        alert.setPositiveButton("Discard",new DialogInterface.OnClickListener() {
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
                    }else
                         audioPanel.setVisibility(View.GONE);

                }
            }
        });

        alert.setView(issueReportView);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String message = input.getText().toString();
                if (includeScreenshot.isChecked())
                    IssueReporter.saveScreenshot(activity, "test_image", true, true);
                if (includeAudioclip.isChecked()) {
                    if (audioControlBtn.getText().toString().equalsIgnoreCase("Stop") && ar != null)
                        ar.stopRecording();
                    File af = new File(activity.getFilesDir(), AUDIO_FILE_NAME);
                    if (af.exists() && af.length() > 0) {
                        try {
                            IssueReporter.saveFile(activity, new FileInputStream(af), AUDIO_FILE_NAME, "3gp", false);
                        } catch (FileNotFoundException e) {
                        }
                    }
                }
                IssueReporter.sendMemLoggerReport(activity, message, null, (IssueParam[]) null);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (audioControlBtn.getText().toString().equalsIgnoreCase("Stop") && ar != null)
                        ar.stopRecording();
            }
        });
        alert.show();
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
