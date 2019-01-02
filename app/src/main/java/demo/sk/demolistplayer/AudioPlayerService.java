package demo.sk.demolistplayer;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AudioPlayerService extends Service implements
        OnCompletionListener, OnBufferingUpdateListener, OnPreparedListener,
        OnErrorListener {

    MediaPlayer mediaPlayer;
    NotificationManager notificationManager;
    boolean isRepeat, isShuffle, isBuffering = false, isPlayAfterBuffering = true;
    String path_audio;
    private final IBinder mBinder = new PlayerBinder();
    private int NOTIFICATION_ID = 111;
    public static final String ON_COMPLETE_BROADCAST_ACTION = "com.bk.lrandom.sharedmp3.audioplayerService";
    Intent intent;
    private int buffer;
    boolean isPush;
    MainActivity activity;

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                // Call Dropped or rejected � Restart service play media
                resume();
            } else {
                pause();
            }
        }
    };

    public class PlayerBinder extends Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return mBinder;
    }

    public void onCreate() {
        super.onCreate();
        // instance player object
        initializeMediaPlayer();
    }

    public void initializeMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnErrorListener(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        intent = new Intent(ON_COMPLETE_BROADCAST_ACTION);
        IntentFilter filter = new IntentFilter();
        filter.addAction(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        // super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        } catch (Exception e) {
            // TODO: handle exception
        }
        unregisterReceiver(receiver);
        Log.d("Service audio::", "DESTROY SERVICE");
    }

    public void play(String path) {

        try {
            if (mediaPlayer != null) {
                this.path_audio = path;
                this.buffer = 0;
                mediaPlayer.reset();
//                mediaPlayer.setDataSource(path_audio);
                mediaPlayer.setDataSource(this, Uri.parse(path));
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                    }
                });
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.prepareAsync();
                isBuffering = true;
                isPlayAfterBuffering = true;
//                showNotification();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public String getTrack() {
        return this.path_audio;
    }

    public void start() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.start();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public void pause() {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
//                cancelNotification();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public void reset() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public void resume() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.start();
//                showNotification();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public void release() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public int getTotalTime() {
        if (isBuffering == false) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public int getElapsedTime() {
        return mediaPlayer.getCurrentPosition();
    }

    public boolean getBuffering() {
        return this.isBuffering;
    }

    public void setBuffering(boolean flag) {
        this.isBuffering = flag;
    }

    public boolean getPlayAfterBuffering() {
        return this.isPlayAfterBuffering;
    }

    public void setPlayAfterBuffering(boolean flag) {
        if (flag == false) {
//            cancelNotification();
        } else {
//            showNotification();
        }
        this.isPlayAfterBuffering = flag;
    }

    public void seek(int pos) {
        mediaPlayer.seekTo(pos);
    }

    public boolean isPlay() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void setRepeat(boolean flag) {
        this.isRepeat = flag;
    }

    public void setShuffle(boolean flag) {
        this.isShuffle = flag;
    }

    public boolean getRepeat() {
        return this.isRepeat;
    }

    public boolean getShuffle() {
        return this.isShuffle;
    }

//    public void showNotification() {
//
//        if (isPush == false) {
////			Log.v("Mydebug", "Normal -showNotification");
//            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
//                    getApplicationContext());
//
//            Intent intent = new Intent(this, PlayerActivity.class);
//            // intent.putExtra("positionPage", PlayerActivity.positionPage);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                    intent, 0);
//            notificationBuilder.setContentTitle(track.getTitle())
//                    .setContentText(track.getDescription())
//                    .setSmallIcon(R.drawable.ic_small_logo)
//                    .setContentIntent(contentIntent);
//            Notification notification = notificationBuilder.build();
//            notificationManager.notify(NOTIFICATION_ID, notification);
//            startForeground(NOTIFICATION_ID, notification);
//
//        } else {
////			Log.v("Mydebug", "Is Push - showNotification");
//            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
//                    getApplicationContext());
//
//            Intent intent = new Intent(this, ReceivePushActivity.class);
//            // intent.putExtra("positionPage", PlayerActivity.positionPage);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                    intent, PendingIntent.FLAG_CANCEL_CURRENT);
//            notificationBuilder.setContentTitle(track.getTitle())
//                    .setContentText(track.getDescription())
//                    .setSmallIcon(R.drawable.ic_small_logo)
//                    .setContentIntent(contentIntent);
//            Notification notification = notificationBuilder.build();
//            notificationManager.notify(NOTIFICATION_ID, notification);
//            startForeground(NOTIFICATION_ID, notification);
//
//        }
//
//    }

    public void cancelNotification() {
//        stopForeground(true);
    }

    public void onCompletion(MediaPlayer mp) {
        // TODO Auto-generated method stub
        mp.seekTo(0);
        mp.start();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // TODO Auto-generated method stub
        if (isPlayAfterBuffering) {
            try {
                mediaPlayer.start();
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
        isBuffering = false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        // TODO Auto-generated method stub
        this.buffer = percent;
    }

    public int getBufferingDownload() {
        return this.buffer;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // TODO Auto-generated method stub
        return true;
    }
}
