package com.example.miguel.bogglegame;

import android.content.Intent;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SplashScreen extends AppCompatActivity {
    private GameMode mode;
    private int difficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        final RadioButton onePlayer = (RadioButton) findViewById(R.id.singlePlayerButton);
        final RadioButton twoPlayer = (RadioButton) findViewById(R.id.twoPlayerButton);
        final RadioButton easyButton = (RadioButton) findViewById(R.id.easyButton);
        final RadioButton mediumButton = (RadioButton) findViewById(R.id.mediumButton);
        final RadioButton hardButton = (RadioButton) findViewById(R.id.hardButton);
        final RadioButton basicButton = (RadioButton) findViewById(R.id.basicButton);
        final RadioButton cutthroatButton = (RadioButton) findViewById(R.id.cutthroatButton);
        final RadioGroup modeGroup = (RadioGroup) findViewById(R.id.modeGroup);
        final RadioGroup difficultyGroup = (RadioGroup) findViewById(R.id.difficultyGroup);
        final Button startButton = (Button) findViewById(R.id.startButton);

        difficulty = 0;
        mode = GameMode.SinglePlayer;

        easyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                difficulty = 0;
            }
        });

        mediumButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                difficulty = 1;
            }
        });

        hardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                difficulty = 2;
            }
        });

        basicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mode = GameMode.BasicTwoPlayer;
            }
        });

        cutthroatButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mode = GameMode.CutThroatTwoPLayer;
            }
        });

        onePlayer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                modeGroup.setVisibility(View.INVISIBLE);
                mode = GameMode.SinglePlayer;
            }
        });

        twoPlayer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                modeGroup.setVisibility(View.VISIBLE);
                if(cutthroatButton.isChecked()){
                    mode = GameMode.CutThroatTwoPLayer;
                }
                else{
                    mode = GameMode.BasicTwoPlayer;
                }
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("EXTRA_DIFFICULTY", difficulty);
                intent.putExtra("EXTRA_MODE", mode);
                startActivity(intent);
            }
        });
    }
}
