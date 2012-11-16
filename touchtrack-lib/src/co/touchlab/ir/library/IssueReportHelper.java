package co.touchlab.ir.library;

import android.app.Activity;
import android.content.Context;
import co.touchlab.ir.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 9/14/12
 * Time: 1:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class IssueReportHelper
{
    public static void logActivityCreated(Activity activity)
    {
        UserActionLog.activityCreated(activity.getClass().getSimpleName());
    }

    public static void logActivityResumed(Activity activity)
    {
        UserActionLog.activityResumed(activity.getClass().getSimpleName());
    }

    public static void sendImmediateReport(Context c, String message, Throwable t)
    {
        try
        {
            //IssueReporter.saveDatabase(c, DatabaseHelper.DATABASE_NAME, false);
            IssueReporter.sendIssueReport(c, false, message, t, generateParams(c));
        }
        catch (Exception e)
        {
            //Whoops
            MemLog.e(c.getClass().getSimpleName(),"Failed issue report");
        }
    }

    public static void sendIssueReport(Context c, String message, IssueReport issueReport)
    {
        try
        {
            IssueReporter.sendPendingIssueReport(c, issueReport, message);
        }
        catch (Throwable e)
        {
            //Whoops
            MemLog.e("Failed issue report", e);
        }
    }

    public static IssueReport prepareReportIssueManual(Context c, Throwable t)
    {
        try
        {
            return IssueReporter.pendingIssueReport(c, true, t, generateParams(c));
        }
        catch (Throwable e)
        {
            //Whoops
            MemLog.e("Failed issue report", e);
            return null;
        }
    }

    public static List<IssueParam> generateParams(Context c)
    {
        try
        {

            List<IssueParam> params = new ArrayList<IssueParam>();

            return params;
        }
        catch (Throwable e)
        {
            //Whoops
            MemLog.e("Failed issue report", e);
            return null;
        }
    }

    private static String safeNumber(Number num)
    {
        try
        {
            return num.toString();
        }
        catch (Exception e)
        {
            return "";
        }
    }
}
