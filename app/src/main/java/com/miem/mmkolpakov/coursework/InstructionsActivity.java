package com.miem.mmkolpakov.coursework;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class InstructionsActivity extends AppCompatActivity {
    Integer length = null;
    //таг для логов
    private final String TAG = "InstructionsActivity";
    Intent playMusicService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);
        isMusicPlaying = false;
    }

    public void retToMainActivityVoid(View v) {
        onBackPressed();
    }

    static boolean isMusicPlaying;
    public void playMusicVoid(View v){
        if (length == null && !isMusicPlaying){
            playMusicService = new Intent(this, MusicService.class);
            startService(playMusicService);
            isMusicPlaying = true;
            Toast outputInfoToast = Toast.makeText(this, "Запуск пасхалки!", Toast.LENGTH_LONG);
            Log.d(TAG, "запуск проигрывания музыки");
            outputInfoToast.show();
        } else if (!isMusicPlaying){
            Log.d(TAG, "проигрывание музыки с последнего момента");
            MusicService.player.seekTo(length);
            MusicService.player.start();
            isMusicPlaying = true;
        } else {
            MusicService.player.pause();
            Log.d(TAG, "остановка проигрывания музыки на паузу");
            length = MusicService.player.getCurrentPosition();
            isMusicPlaying = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isMusicPlaying) {
            MusicService.player.pause();
            Log.d(TAG, "остановка проигрывания музыки на паузу");
            length = MusicService.player.getCurrentPosition();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isMusicPlaying) {
            stopService(playMusicService);
            isMusicPlaying = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMusicPlaying) {
            Log.d(TAG, "проигрывание музыки с последнего момента");
            MusicService.player.seekTo(length);
            MusicService.player.start();
        }
    }
}
