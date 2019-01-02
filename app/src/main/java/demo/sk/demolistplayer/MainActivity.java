package demo.sk.demolistplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private AudioItemAdapter audioItemAdapter;
    private AudioPlayerService audioPlayerService;
    private SwipeRefreshLayout swipeContainer;
    private ArrayList<AudioItem> lstAudioItems;
    private RecyclerView mRecyclerView;
    private int page = 15;
    private Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        Intent intent = new Intent(getApplicationContext(), AudioPlayerService.class);
        getApplicationContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        mRecyclerView = (RecyclerView) findViewById(R.id.rv);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        lstAudioItems = new ArrayList<>();
        loadData();
        audioItemAdapter = new AudioItemAdapter(this, lstAudioItems);
        mRecyclerView.setAdapter(audioItemAdapter);

        audioItemAdapter.setLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (lstAudioItems.size() > 0) {
                            loadMoreData();
                        }
                    }
                });
            }
        });

        // Lookup the swipe container view
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
//                mRecyclerView.setVisibility(View.GONE);
                Handler mHander = new Handler();
                mHander.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshData();
                    }
                }, 1500);
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

    }

    public void refreshData() {
        lstAudioItems.clear();
        audioItemAdapter.clear();
        loadData();
        audioItemAdapter.addAll(lstAudioItems);
//        mRecyclerView.setVisibility(View.VISIBLE);
        swipeContainer.setRefreshing(false);// Now we call setRefreshing(false) to signal refresh has finished
        audioItemAdapter.resetMediaPlayer();
        page = 15;
    }

    private void loadData() {
        for (int i = 0; i < 15; i++) {
            lstAudioItems.add(new AudioItem(R.raw.mp3));
        }
    }

    private void loadMoreData() {
        audioItemAdapter.addLoadMore(lstAudioItems.size() - 1);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                audioItemAdapter.removeLoadMore(lstAudioItems.size() - 1);
                if (page < 256) {
                    List<AudioItem> list = new ArrayList<>();
                    for (int i = 0; i < page; i++) {
                        list.add(new AudioItem(R.raw.mp3));
                    }
                    lstAudioItems.addAll(list);
                    audioItemAdapter.mNotifyDataSetChanged();
                } else {
                    Toast.makeText(mContext, "Is Finish Load!", Toast.LENGTH_LONG).show();
                }
            }
        }, 1500);
    }


    @Override
    protected void onPause() {
        super.onPause();
        audioPlayerService.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioItemAdapter.releaseMediaPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        audioItemAdapter.resetMediaPlayer();
    }

    private static class AudioItem {
        final int audioResId;

        private AudioItem(int audioResId) {
            this.audioResId = audioResId;
        }
    }

    private class AudioItemAdapter extends RecyclerView.Adapter<AudioItemAdapter.AudioItemsViewHolder> {

        private Handler handler = new Handler();
        private List<AudioItem> audioItems;
        private int playingPosition;
        private AudioItemsViewHolder playingHolder;


        private Context mContext;
        public final int TYPE_ITEM = 0;
        public final int TYPE_LOADING = 1;
        private OnLoadMoreListener loadMoreListener;
        private boolean isLoading = false, isMoreDataAvailable = true;

        AudioItemAdapter(Context mContext, List<AudioItem> audioItems) {
            this.mContext = mContext;
            this.audioItems = audioItems;
            this.playingPosition = -1;
        }

        @Override
        public AudioItemsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            return new AudioItemsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cell, parent, false));
            LayoutInflater inflater = LayoutInflater.from(mContext);
            if (viewType == TYPE_ITEM) {
                return new AudioItemsViewHolder(inflater.inflate(R.layout.cell, parent, false));
            } else {
                return new LoadMoreHolder(inflater.inflate(R.layout.item_load_more, parent, false));
            }
        }


        @Override
        public int getItemViewType(int position) {
            return audioItems.get(position) == null ? TYPE_LOADING : TYPE_ITEM;
        }

        @Override
        public void onBindViewHolder(AudioItemsViewHolder holder, int position) {

            if (position >= getItemCount() - 1 && isMoreDataAvailable && !isLoading && loadMoreListener != null) {
                isLoading = true;
                loadMoreListener.onLoadMore();
            }

            if (getItemViewType(position) == TYPE_ITEM) {
                final AudioItem item = audioItems.get(position);
                holder.bindData(item, position);

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

        }

        @Override
        public int getItemCount() {
            return audioItems.size();
        }


        public void clear() {
            audioItems.clear();
            notifyDataSetChanged();
        }

        public void addAll(List<AudioItem> list) {
            audioItems.addAll(list);
            mNotifyDataSetChanged();
        }

        private void updateNonPlayingView(AudioItemsViewHolder holder) {
            if (holder == playingHolder) {
                handler.removeCallbacks(updateTime);
            }
            if (holder.sbProgress != null) {
                holder.sbProgress.setEnabled(false);
                holder.sbProgress.setProgress(0);
                holder.ivPlayPause.setImageResource(R.drawable.ic_play_arrow);
            }
        }

        private void updatePlayingView() {

            if (audioPlayerService.isPlay() || audioPlayerService.getBuffering()) {
                playingHolder.ivPlayPause.setImageResource(R.drawable.ic_pause);
            } else {
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

        private void startMediaPlayer(int audioId) {
            String path = "android.resource://" + getPackageName() + "/raw/mp3";
            audioPlayerService.play(path);
        }


        public void resetMediaPlayer() {
            playingPosition = -1;
            if (null != playingHolder) {
                updateNonPlayingView(playingHolder);
            }
            if (audioPlayerService != null && null != audioPlayerService.mediaPlayer) {
                audioPlayerService.reset();
            }
        }

        public void releaseMediaPlayer() {
            playingPosition = -1;
            if (null != playingHolder) {
                updateNonPlayingView(playingHolder);
            }
            if (audioPlayerService != null && audioPlayerService.mediaPlayer != null) {
                audioPlayerService.pause();
                audioPlayerService = null;
            }
        }


        class AudioItemsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
            SeekBar sbProgress;
            ImageView ivPlayPause;
            TextView tvIndex;

            AudioItemsViewHolder(View itemView) {
                super(itemView);
                ivPlayPause = (ImageView) itemView.findViewById(R.id.ivPlayPause);
                sbProgress = (SeekBar) itemView.findViewById(R.id.sbProgress);
                tvIndex = (TextView) itemView.findViewById(R.id.tvIndex);
            }

            private void bindData(AudioItem item, int position) {
                ivPlayPause.setOnClickListener(this);
                sbProgress.setOnSeekBarChangeListener(this);
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
                    if (audioPlayerService != null && audioPlayerService.mediaPlayer != null) {
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
                handler.removeCallbacks(updateTime);
                audioPlayerService.seek(audioPlayerService.getElapsedTime());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateTime);
                audioPlayerService.seek(audioPlayerService.getElapsedTime());
                updateProgress();
            }
        }

        public class LoadMoreHolder extends AudioItemsViewHolder {
            public LoadMoreHolder(View itemView) {
                super(itemView);
            }
        }

        public void setMoreDataAvailable(boolean moreDataAvailable) {
            isMoreDataAvailable = moreDataAvailable;
        }

        public void mNotifyDataSetChanged() {
            notifyDataSetChanged();
            isLoading = false;
        }

        private boolean isValidPos(int position) {
            return position >= 0 && position < audioItems.size();
        }

        public void addLoadMore(int position) {
            if (isValidPos(position)) {
                audioItems.add(null);
                notifyItemInserted(audioItems.size() - 1);
                notifyItemRangeChanged(position, audioItems.size());
            }
        }

        public void removeLoadMore(int position) {
            if (isValidPos(position)) {
                audioItems.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, audioItems.size());
            }
        }

        public void setLoadMoreListener(OnLoadMoreListener loadMoreListener) {
            this.loadMoreListener = loadMoreListener;
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore();

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            audioPlayerService = null;
            Log.d("service::", "onServiceDisconnected");
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            audioPlayerService = ((AudioPlayerService.PlayerBinder) service).getService();
            Log.d("service::", "--onServiceDisconnected");
        }
    };

}
