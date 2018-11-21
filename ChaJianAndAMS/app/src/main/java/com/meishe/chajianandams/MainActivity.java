package com.meishe.chajianandams;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {
    private final String TAG = getClass().getName();
    private TextView mMainTextview;
    private VideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mVideoView = findViewById(R.id.video_view);
//        setupVideo();
    }

    private void setupVideo() {
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mVideoView.start();
            }
        });
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
//                stopPlaybackVideo();
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
//                stopPlaybackVideo();
                return true;
            }
        });
        try {
            //本地的视频  需要在手机SD卡根目录添加一个 fl1234.mp4 视频
            String videoUrl1 = Environment.getExternalStorageDirectory().getPath()+"/output.mp4" ;
            Log.e(TAG, "setupVideo: "+videoUrl1 );
            Uri uri = Uri.parse(videoUrl1);
//            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.test_video);
            mVideoView.setVideoURI(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initView() {
        mMainTextview = (TextView) findViewById(R.id.main_textview);
    }
}
