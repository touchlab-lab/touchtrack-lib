package co.touchlab.ir.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import co.touchlab.ir.IssueReport;
import co.touchlab.ir.MemLog;
import co.touchlab.ir.UserActionLog;


/**
 * Created with IntelliJ IDEA.
 * User: brianplummer
 * Date: 10/5/12
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class FeedbackActivity extends Activity
{
    protected IssueReport issueReport = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        UserActionLog.activityCreated(this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback);

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

    protected void initControls()
    {

        findViewById(R.id.send_feedback).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                MemLog.ua(this.getClass().getSimpleName(), "FeedbackActivity send clicked");
                sendReport();
            }
        });
    }

    protected void sendReport()
    {
        final String msg = ((EditText) findViewById(R.id.feedbackText)).getText().toString();

        new Thread()
        {
            @Override
            public void run()
            {
                IssueReportHelper.sendIssueReport(FeedbackActivity.this, msg, issueReport);
            }
        }.start();

        Toast.makeText(FeedbackActivity.this, "Thank You", android.widget.Toast.LENGTH_LONG).show();

        finish();


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
        Intent intent = new Intent(context, FeedbackActivity.class);
        intent.putExtra(RobotDownActivity.ISSUE_REPORT, issueReport);
        context.startActivity(intent);
    }
}
