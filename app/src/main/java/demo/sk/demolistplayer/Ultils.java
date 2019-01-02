package demo.sk.demolistplayer;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import java.io.File;
import java.net.URL;
import java.util.List;

public class Ultils {

    public static String Timer(long milliseconds) {
        String finalTimer = "";
        String secondsString = "";
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        if (hours > 0) {
            finalTimer = hours + ":";
        }
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }
        finalTimer = finalTimer + minutes + ":" + secondsString;
        return finalTimer;
    }

    public static int getProgressPercentage(long currentDuration,
                                            long totalDuration) {
        Double percentage = (double) 0;
        long currentSeconds = (int) (currentDuration / 1000);
        long totalSeconds = (int) (totalDuration / 1000);
        percentage = (((double) currentSeconds) / totalSeconds) * 100;
        return percentage.intValue();
    }

    public static int progressToTimer(int progress, int totalDuration) {
        int currentDuration = 0;
        totalDuration = (int) (totalDuration / 1000);
        currentDuration = (int) ((((double) progress) / 100) * totalDuration);
        return currentDuration * 1000;
    }

    public static boolean isConnectingToInternet(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
        }
        return false;
    }

    public static boolean createDirOnSDCard(String dirName) {
        File folder = new File(Environment.getExternalStorageDirectory() + "/"
                + dirName);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        return success;
    }

    public static String getFileName(URL extUrl) {
        String filename = "";
        String path = extUrl.getPath();
        String[] pathContents = path.split("[\\\\/]");
        if (pathContents != null) {
            int pathContentsLength = pathContents.length;
            System.out.println("Path Contents Length: " + pathContentsLength);
            for (int i = 0; i < pathContents.length; i++) {
                System.out.println("Path " + i + ": " + pathContents[i]);
            }
            String lastPart = pathContents[pathContentsLength - 1];
            String[] lastPartContents = lastPart.split("\\.");
            if (lastPartContents != null && lastPartContents.length > 1) {
                int lastPartContentLength = lastPartContents.length;
                System.out
                        .println("Last Part Length: " + lastPartContentLength);
                String name = "";
                for (int i = 0; i < lastPartContentLength; i++) {
                    System.out.println("Last Part " + i + ": "
                            + lastPartContents[i]);
                    if (i < (lastPartContents.length - 1)) {
                        name += lastPartContents[i];
                        if (i < (lastPartContentLength - 2)) {
                            name += ".";
                        }
                    }
                }
                String extension = lastPartContents[lastPartContentLength - 1];
                filename = name + "." + extension;
                System.out.println("Name: " + name);
                System.out.println("Extension: " + extension);
                System.out.println("Filename: " + filename);
            }
        }
        return filename;
    }

    // nguyendx 9-3-2015
    public static boolean isApplicationSentToBackground(final Context context) {
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }

        return false;
    }

    // check play
    public static boolean checkMusicActive(final Context mContext) {
        AudioManager manager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        if (manager.isMusicActive()) {
            return true;
        } else {
            return false;
        }
    }

}
