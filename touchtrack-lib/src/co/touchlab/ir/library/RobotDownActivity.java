package co.touchlab.ir.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import co.touchlab.ir.IssueReport;
import co.touchlab.ir.MemLog;
import co.touchlab.ir.UserActionLog;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 9/2/12
 * Time: 10:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class RobotDownActivity extends Activity
{
    public static final String ISSUE_REPORT = "ISSUE_REPORT";
    protected IssueReport issueReport = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        UserActionLog.activityCreated(this.getClass().getSimpleName());

        super.onCreate(savedInstanceState);

        if(getIntent() != null && getIntent().hasExtra(ISSUE_REPORT)){
            issueReport = (IssueReport)getIntent().getSerializableExtra(ISSUE_REPORT);
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.robot_down);
        initControls();
    }

    @Override
    protected void onResume()
    {
        UserActionLog.activityResumed(this.getClass().getSimpleName());
        super.onResume();
    }

    protected void initControls(){

        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MemLog.ua(this.getClass().getSimpleName(), "report closed");
                finish();
            }
        });

        findViewById(R.id.report).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MemLog.ua(this.getClass().getSimpleName(), "report closed");
                FeedbackActivity.callMe(RobotDownActivity.this, issueReport);
                finish();
            }
        });

    }

    public static void callMe(Context context, Throwable throwable)
    {
        //This *feels* like it should be in a background process, but I'm not 100% sure of death thread context and rules.
        IssueReport issueReport = IssueReportHelper.prepareReportIssueManual(context, throwable);

        Intent intent = new Intent(context, RobotDownActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ISSUE_REPORT, issueReport);

        context.startActivity(intent);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    public static class RobotDownDefaultExceptionHandler implements Thread.UncaughtExceptionHandler
    {
        private Thread.UncaughtExceptionHandler defExHandler;
        private Context context;

        public RobotDownDefaultExceptionHandler(
                Context context,
                Thread.UncaughtExceptionHandler defExHandler
        )
        {
            this.context = context;
            this.defExHandler = defExHandler;
        }

        public void uncaughtException(final Thread thread, final Throwable throwable)
        {
            //Originally ignoring OutOfMemory, but we'll try to log that now.
            //TODO: init all data objects when setting up the handler, to conserve memory usage when triggered
            try
            {
                //Report is prepared inside 'callMe' and sent in FeedbackActivity
//                IssueReportHelper.sendImmediateReport(context, "Immediate Debug", throwable);

                Log.e("Uncaught Exception", throwable.toString());
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        RobotDownActivity.callMe(context.getApplicationContext(), throwable);
                    }
                }.start();
            }
            catch (Throwable e)
            {
                defExHandler.uncaughtException(thread, throwable);
            }
        }

        public static void replaceExceptionHandler(final Context context)
        {
            Thread.UncaughtExceptionHandler defExHandler = Thread.getDefaultUncaughtExceptionHandler();
            if (!(defExHandler instanceof RobotDownDefaultExceptionHandler))
                Thread.setDefaultUncaughtExceptionHandler(new RobotDownDefaultExceptionHandler(context, defExHandler));
        }
    }

}