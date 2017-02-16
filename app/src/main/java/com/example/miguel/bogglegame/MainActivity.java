package com.example.miguel.bogglegame;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    frontend frontend = null;
    private SensorManager sManager;
    private Sensor mAccelerometer;
    private ShakeListener mShakeListener;
    CountDownTimer gameTimer = null;

    public MainActivity() throws IOException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //read words from dictionary.txt
        String text = "";
        final String []words;
        try {
            InputStream is = getAssets().open("dictionary.txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            text = new String(buffer);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        words = text.split("\r\n");
        frontend = new frontend(words);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final GridView gridview = (GridView) findViewById(R.id.LetterGrid);
        final TextView timeview = (TextView) findViewById(R.id.TimeRemaining);
        final TextView scoreview = (TextView) findViewById(R.id.CurrentScore);
        final Button clearButton = (Button) findViewById(R.id.ClearButton);
        final Button submitButton = (Button) findViewById(R.id.SubmitButton);
        final Button resetButton = (Button) findViewById(R.id.ResetButton);
        final TextView currentWord = (TextView) findViewById(R.id.CurrentWord);
        final TextView foundWords = (TextView) findViewById(R.id.foundWords);


        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, frontend.get_letters());

        sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeListener = new ShakeListener();
        mShakeListener.setOnShakeListener(new ShakeListener.OnShakeListener() {
            @Override
            public void onShake() {
                gameTimer.cancel();
                frontend = new frontend(words);
                String[] letters = frontend.get_letters();
                for(int i = 0; i < gridview.getChildCount(); i++) {
                    TextView child = (TextView) gridview.getChildAt(i);
                    child.setText(letters[i]);
                }
                refresh(gridview, currentWord, scoreview, foundWords);
                gameTimer = start_timer(timeview);
                System.out.println("Restarted the game");
            }
        });

        gridview.setAdapter(adapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if(frontend.tile_click(position) == true) {
                    if(frontend.tile_state[position] == true) {
                        v.setBackgroundColor(Color.parseColor("#FF0000"));
                    } else {
                        v.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    }
                    currentWord.setText(frontend.get_candidate_word());
                }
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(frontend.clear_click()) {
                    refresh(gridview, currentWord, scoreview, foundWords);
                }
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(frontend.submit_click()) {
                    refresh(gridview, currentWord, scoreview, foundWords);
                }
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gameTimer.cancel();
                frontend = new frontend(words);
                String[] letters = frontend.get_letters();
                for(int i = 0; i < gridview.getChildCount(); i++) {
                    TextView child = (TextView) gridview.getChildAt(i);
                    child.setText(letters[i]);
                }
                refresh(gridview, currentWord, scoreview, foundWords);
                gameTimer = start_timer(timeview);
                System.out.println("Restarted the game");
            }
        });
        gameTimer = start_timer(timeview);
    }

    //refreshes the views according to current game state
    public void refresh(GridView gridview, TextView current_word, TextView current_score, TextView words_found) {
        for(int i = 0; i < gridview.getChildCount(); i++) {
            TextView child = (TextView) gridview.getChildAt(i);
            if(frontend.tile_state[i] == true) {
                child.setBackgroundColor(Color.parseColor("#FF0000"));
            } else {
                child.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }
        current_word.setText(frontend.get_candidate_word());
        current_score.setText("Score: " + Integer.toString(frontend.boggleBoard.getScore()));
        String found_string = "";
        for(String word : frontend.boggleBoard.validWordsFoundByUser) {
            found_string += word;
            found_string += "  ";
        }
        words_found.setText(found_string);
    }

    public CountDownTimer start_timer(final TextView timeview) {
        return new CountDownTimer(180000, 1000) {

            public void onTick(long millisUntilFinished) {
                timeview.setText("Time Left: " + (millisUntilFinished / 1000) + " s");
            }

            public void onFinish() {
                timeview.setText("Game Over");
                frontend.game_over = true;
            }
        }.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register the Session Manager Listener onResume
        sManager.registerListener(mShakeListener, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        // Unregister the Sensor Manager onPause
        sManager.unregisterListener(mShakeListener);
        super.onPause();
    }
}
