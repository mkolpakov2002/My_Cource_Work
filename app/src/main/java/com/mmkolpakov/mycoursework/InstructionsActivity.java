package com.mmkolpakov.mycoursework;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class InstructionsActivity extends AppCompatActivity {
    int length;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_instructions);
        isMusicPlaying = false;

    }

    public void retToMainActivityVoid(View v) {
        onBackPressed();
    }

    static boolean isMusicPlaying;
    public void playMusicVoid(View v){
        if (!isMusicPlaying){
            Intent playMusicService = new Intent(this, MusicService.class);
            startService(playMusicService);
            isMusicPlaying = true;
            Toast outputInfoToast = Toast.makeText(this, "Запуск пасхалки!", Toast.LENGTH_LONG);
            outputInfoToast.show();
        } else {
            stopService(new Intent(this, MusicService.class));
            isMusicPlaying = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isMusicPlaying) {
            MusicService.player.pause();
            length = MusicService.player.getCurrentPosition();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isMusicPlaying) {
            stopService(new Intent(this, MusicService.class));
            isMusicPlaying = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMusicPlaying) {
            MusicService.player.seekTo(length);
            MusicService.player.start();
        }
    }
}
