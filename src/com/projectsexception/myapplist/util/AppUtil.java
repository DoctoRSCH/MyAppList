package com.projectsexception.myapplist.util;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
import android.widget.Toast;

import com.projectsexception.myapplist.R;
import com.projectsexception.myapplist.model.AppInfo;

public class AppUtil {
    
    private static final String SCHEME = "package";
    private static final String APP_PKG_NAME_21 = "com.android.settings.ApplicationPkgName";
    private static final String APP_PKG_NAME_22 = "pkg";
    private static final String APP_DETAILS_PACKAGE_NAME = "com.android.settings";
    private static final String APP_DETAILS_CLASS_NAME = "com.android.settings.InstalledAppDetails";
    
    public static List<AppInfo> loadAppInfoList(PackageManager mPm) {
        List<ApplicationInfo> apps = mPm.getInstalledApplications(PackageManager.GET_META_DATA);
        if (apps == null) {
            apps = new ArrayList<ApplicationInfo>();
        }

        // Create corresponding array of entries and load their labels.
        List<AppInfo> entries = new ArrayList<AppInfo>();
        AppInfo entry;
        for (ApplicationInfo applicationInfo : apps) {
            if (!isSystemPackage(applicationInfo)) {
                entry = createAppInfo(mPm, applicationInfo);
                entries.add(entry);
            }
        }
        return entries;
    }
    
    public static AppInfo loadAppInfo(PackageManager mPm, String packageName) {
        ApplicationInfo applicationInfo = loadApplicationInfo(mPm, packageName);
        AppInfo appInfo = null;
        if (applicationInfo != null) {
            appInfo = createAppInfo(mPm, applicationInfo);
        }
        return appInfo;
    }
    
    public static ApplicationInfo loadApplicationInfo(PackageManager mPm, String packageName) {
        try {
            return mPm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);            
        } catch (NameNotFoundException e) {
            return null;
        }
    }
    
    public static PackageInfo loadPackageInfo(PackageManager mPm, String packageName) {
        try {
            return mPm.getPackageInfo(packageName, 
                    PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e) {
            return null;
        }
    }
    
    public static boolean isFromGooglePlay(PackageManager mPm, String packageName) {
        String installPM = mPm.getInstallerPackageName(packageName);
        if ( installPM == null ) {
            // Definitely not from Google Play
            return false;
        } else if (installPM.equals("com.google.android.feedback") || installPM.equals("com.android.vending")) {
            // Installed from the Google Play
            return true;
        }
        return false;
    }
    
    public static boolean isRunning(Context context, String packageName) {
        boolean running = false;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);        
        List<RunningAppProcessInfo> procInfos = am.getRunningAppProcesses();
        for(int i = 0; i < procInfos.size(); i++){
            if (procInfos.get(i).processName.equals(packageName)) {
                running = true;
                break;
            }
        }
        return running;
    }
    
    public static Intent getApplicationIntent(PackageManager pm, PackageInfo packageInfo) {
        Intent intent = null;
        intent = pm.getLaunchIntentForPackage(packageInfo.packageName);
        if (intent != null) {
            intent = intent.cloneFilter();
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            return intent;
        }
        if (packageInfo.activities.length == 1) {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            intent.setClassName(packageInfo.packageName, packageInfo.activities[0].name);
            return intent;
        }
        intent = getIntent(packageInfo.packageName, pm);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            return intent;
        }
        return null;
    }
    
    private static Intent getIntent(String packageName, PackageManager pm) {
        List<ResolveInfo> list = getRunableList(pm, false);
        for (ResolveInfo info : list) {
            // System.out.println(packageName + " == " + info.activityInfo.packageName);
            if (packageName.equals(info.activityInfo.packageName)) {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setClassName(packageName, info.activityInfo.name);
                return i;
            }
        }
        return null;
    }
    
    private static synchronized List<ResolveInfo> getRunableList(PackageManager pm, boolean reload) {
        Intent baseIntent = new Intent(Intent.ACTION_MAIN);
        baseIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        return pm.queryIntentActivities(baseIntent, 0);
    }
    
    private static boolean isSystemPackage(ApplicationInfo pkgInfo) {
        return ((pkgInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) ? true : false;
    }
    
    private static AppInfo createAppInfo(PackageManager mPm, ApplicationInfo applicationInfo) {
        AppInfo entry = new AppInfo();
        entry.setPackageName(applicationInfo.packageName);
        entry.setName(applicationInfo.loadLabel(mPm).toString());
        entry.setIcon(applicationInfo.loadIcon(mPm));
        entry.setInstalled(true);
        return entry;
    }
    
    public static void showInstalledAppDetails(Context context, String packageName) {
        Intent intent = new Intent();
        final int apiLevel = Build.VERSION.SDK_INT;
        if (apiLevel >= 9) { // above 2.3
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts(SCHEME, packageName, null);
            intent.setData(uri);
        } else { // below 2.3
            final String appPkgName = (apiLevel == 8 ? APP_PKG_NAME_22 : APP_PKG_NAME_21);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName(APP_DETAILS_PACKAGE_NAME, APP_DETAILS_CLASS_NAME);
            intent.putExtra(appPkgName, packageName);
        }
        context.startActivity(intent);
    }
    
    public static void showPlayGoogleApp(Context context, String packageName) {
        String url = context.getString(R.string.play_google, packageName);
        try {
        	context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		} catch (Exception e) {
			Toast.makeText(context, R.string.problem_no_google_play, Toast.LENGTH_SHORT).show();
		}
    }
    
    public static Spanned appInfoToHTML(Context ctx, List<AppInfo> lst, boolean html) {
        final StringBuilder sb = new StringBuilder();
        if (lst != null) {
            for (AppInfo appInfo : lst) {
                sb.append("<a href=\"");
                sb.append(ctx.getString(R.string.play_google_web, appInfo.getPackageName()));
                sb.append("\">");            
                sb.append(appInfo.getName());
                sb.append("</a><br/>\n");
            }
        }
        sb.append("<br/>\n"); 
        sb.append(ctx.getString(R.string.share_file_html));
        return Html.fromHtml(sb.toString());
    }
    
    public static String appInfoToText(Context ctx, List<AppInfo> lst, boolean html) {
        final StringBuilder sb = new StringBuilder();
        if (lst != null) {
            for (AppInfo appInfo : lst) {
                sb.append(appInfo.getName());
                sb.append(": ");
                sb.append(ctx.getString(R.string.play_google_web, appInfo.getPackageName()));
                sb.append("\n"); 
            }
        }
        sb.append("\n"); 
        sb.append(ctx.getString(R.string.share_file_text));
        return sb.toString();
    }

}
