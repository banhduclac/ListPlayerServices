package demo.sk.demolistplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivityUseService extends Activity {

    private AudioItemAdapter audioItemAdapter;
    private AudioPlayerService audioPlayerService;
    private ArrayList<AudioItem> lstAudioItems;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(getApplicationContext(), AudioPlayerService.class);
        getApplicationContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
        // arrange cells in vertical column
        rv.setLayoutManager(new LinearLayoutManager(this));
        // add 256 stub audio items
        lstAudioItems = new ArrayList<>();
        loadData();
        audioItemAdapter = new AudioItemAdapter(lstAudioItems);
        rv.setAdapter(audioItemAdapter);
    }

    private void loadData() {
        for (int i = 0; i < 256; i++) {
            lstAudioItems.add(new AudioItem(R.raw.mp3));
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioItemAdapter.stopPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(getApplicationContext(), AudioPlayerService.class);
        getApplicationContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private static class AudioItem {
        final int audioResId;

        private AudioItem(int audioResId) {
            this.audioResId = audioResId;
        }
    }

    private class AudioItemAdapter extends RecyclerView.Adapter<AudioItemAdapter.AudioItemsViewHolder> implements Handler.Callback {

        private static final int MSG_UPDATE_SEEK_BAR = 1845;

        private Handler uiUpdateHandler;
        Handler handler = new Handler();

        private List<AudioItem> audioItems;
        private int playingPosition;
        private AudioItemsViewHolder playingHolder;

        AudioItemAdapter(List<AudioItem> audioItems) {
            this.audioItems = audioItems;
            this.playingPosition = -1;
            uiUpdateHandler = new Handler(this);
        }

        @Override
        public AudioItemsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new AudioItemsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cell, parent, false));
        }

        @Override
        public void onBindViewHolder(AudioItemsViewHolder holder, int position) {
            if (position == playingPosition) {
                playingHolder = holder;
                // this view holder corresponds to the currently playing audio cell
                // update its view to show playing progress
                updatePlayingView();
            } else {
                // and this one corresponds to non playing
                updateNonPlayingView(holder);
            }
            holder.tvIndex.setText(String.format(Locale.US, "%d", position));
        }

        @Override
        public int getItemCount() {
            return audioItems.size();
        }

        // Clean all elements of the recycler
        public void clear() {
            audioItems.clear();
            notifyDataSetChanged();
        }

        // Add a list of items -- change to type used
        public void addAll(List<AudioItem> list) {
            audioItems.addAll(list);
            notifyDataSetChanged();
        }

        @Override
        public void onViewRecycled(AudioItemsViewHolder holder) {
            super.onViewRecycled(holder);
            if (playingPosition == holder.getAdapterPosition()) {
                // view holder displaying playing audio cell is being recycled
                // change its state to non-playing
                updateNonPlayingView(playingHolder);
                playingHolder = null;
            }
        }


        /**
         * Changes the view to non playing state
         * - icon is changed to play arrow
         * - seek bar disabled
         * - remove seek bar updater, if needed
         *
         * @param holder ViewHolder whose state is to be chagned to non playing
         */
        private void updateNonPlayingView(AudioItemsViewHolder holder) {
            if (holder == playingHolder) {
                uiUpdateHandler.removeMessages(MSG_UPDATE_SEEK_BAR);
            }
            holder.sbProgress.setEnabled(false);
            holder.sbProgress.setProgress(0);
            holder.ivPlayPause.setImageResource(R.drawable.ic_play_arrow);
        }

        /**
         * Changes the view to playing state
         * - icon is changed to pause
         * - seek bar enabled
         * - start seek bar updater, if needed
         */
        private void updatePlayingView() {

            if (audioPlayerService.isPlay() || audioPlayerService.getBuffering()) {
                uiUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_SEEK_BAR, 100);
                playingHolder.ivPlayPause.setImageResource(R.drawable.ic_pause);
            } else {
                uiUpdateHandler.removeMessages(MSG_UPDATE_SEEK_BAR);
                playingHolder.ivPlayPause.setImageResource(R.drawable.ic_play_arrow);
            }

            updateProgress();
        }

        private Runnable updateTime = new Runnable() {
            public void run() {
                // TODO Auto-generated method stub
                playingHolder.sbProgress.setMax(audioPlayerService.getTotalTime());
                playingHolder.sbProgress.setProgress(audioPlayerService.getElapsedTime());
                playingHolder.sbProgress.setEnabled(true);
                handler.postDelayed(this, 100);
            }
        };

        public void updateProgress() {
            handler.postDelayed(updateTime, 100);
        }

        private void startMediaPlayer(int audioResId) {
            String path = "android.resource://" + getPackageName() + "/raw/mp3";
            audioPlayerService.play(path);
        }

        void stopPlayer() {
            if (null != audioPlayerService.mediaPlayer) {
                releaseMediaPlayer();
            }
        }

        public void releaseMediaPlayer() {
            if (null != playingHolder) {
                updateNonPlayingView(playingHolder);
            }
            audioPlayerService.release();
            audioPlayerService = null;
            playingPosition = -1;
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_SEEK_BAR: {
                    playingHolder.sbProgress.setProgress(audioPlayerService.getElapsedTime());
                    uiUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_SEEK_BAR, 100);
                    return true;
                }
            }
            return false;
        }

        // Interaction listeners e.g. click, seekBarChange etc are handled in the view holder itself. This eliminates
        // need for anonymous allocations.
        class AudioItemsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
            SeekBar sbProgress;
            ImageView ivPlayPause;
            TextView tvIndex;

            AudioItemsViewHolder(View itemView) {
                super(itemView);
                ivPlayPause = (ImageView) itemView.findViewById(R.id.ivPlayPause);
                ivPlayPause.setOnClickListener(this);
                sbProgress = (SeekBar) itemView.findViewById(R.id.sbProgress);
                sbProgress.setOnSeekBarChangeListener(this);
                tvIndex = (TextView) itemView.findViewById(R.id.tvIndex);
            }

            @Override
            public void onClick(View v) {

                if (getAdapterPosition() == playingPosition) {
                    // toggle between play/pause of audio
                    if (audioPlayerService.isPlay()) {
                        audioPlayerService.pause();
                    } else {
                        audioPlayerService.start();
                    }
                } else {
                    // start another audio playback
                    playingPosition = getAdapterPosition();
                    if (audioPlayerService.mediaPlayer != null) {
                        if (null != playingHolder) {
                            updateNonPlayingView(playingHolder);
                        }
                    }
                    playingHolder = this;
                    startMediaPlayer(audioItems.get(playingPosition).audioResId);
                }
                updatePlayingView();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioPlayerService.seek(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                handler.removeCallbacks(updateTime);
//                audioPlayerService.seek(audioPlayerService.getElapsedTime());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                handler.removeCallbacks(updateTime);
//                audioPlayerService.seek(audioPlayerService.getElapsedTime());
//                updateProgress();
            }
        }

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            audioPlayerService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            audioPlayerService = ((AudioPlayerService.PlayerBinder) service).getService();
        }
    };

}
