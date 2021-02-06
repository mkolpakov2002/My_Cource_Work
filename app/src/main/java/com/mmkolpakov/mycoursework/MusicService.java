package com.mmkolpakov.mycoursework;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.annotation.Nullable;


public class MusicService extends Service {

    //Необязательный сервис - пасхалка, проигрывающая гимн ВШЭ
    static MediaPlayer player;
    int playerCurrentPosition;
    int playerContentDuration;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //not implemented
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                player = MediaPlayer.create(MusicService.this, R.raw.hse_anthem);
                player.setLooping(true);
                player.setVolume(100, 100);
                player.start();
            }
        }).start();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.stop();
        player.release(); //освобождение используемых проигрывателем ресурсов
    }

}

