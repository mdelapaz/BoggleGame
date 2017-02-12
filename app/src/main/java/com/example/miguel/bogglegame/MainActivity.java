package com.example.miguel.bogglegame;

import android.content.res.AssetManager;
import android.graphics.Color;
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

    public MainActivity() throws IOException {
    }
    /*
    static final String[] letters = new String[] {
            "A", "B", "C", "D",
            "E", "F", "G", "H",
            "I", "J", "K", "L",
            "M", "N", "O", "P"
    };

    static int lastPicked = -1;
    static boolean[] clicked = new boolean[]
            { false, false, false, false,
                false, false, false, false,
                false, false, false, false,
                false, false, false, false };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //read words from dictionary.txt
        String text = "";
        String []words;
        try {
            InputStream is = getAssets().open("dictionary.txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            text = new String(buffer);
            words = text.split("\r\n");
            frontend = new frontend(words);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final GridView gridview = (GridView) findViewById(R.id.LetterGrid);
        final Button clearButton = (Button) findViewById(R.id.ClearButton);
        final Button submitButton = (Button) findViewById(R.id.SubmitButton);
        final Button resetButton = (Button) findViewById(R.id.ResetButton);
        final TextView currentWord = (TextView) findViewById(R.id.CurrentWord);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, frontend.get_letters());

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

                /*
                int lastRow = lastPicked / 4;
                int lastCol = lastPicked % 4;
                int row = position / 4;
                int col = position % 4;

                if(!clicked[position] &&
                        (lastPicked == -1 ||
                                (Math.abs(lastRow-row) <= 1 &&
                                    Math.abs(lastCol-col) <= 1)) &&
                        (lastRow != row || lastCol != col)
                        ) {

                    lastPicked = position;
                    System.out.print("Me rike cricky cricky!\n");
                    clicked[position] = true;

                    if (currentWord.getText() == null) {
                        currentWord.setText(((TextView) v).getText());
                    } else {
                        currentWord.append(((TextView) v).getText());
                    }

                    v.setBackgroundColor(Color.parseColor("#FF0000"));
                }*/
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(frontend.clear_click()) {
                    for(int i=0; i< gridview.getChildCount(); i++) {
                        TextView child = (TextView) gridview.getChildAt(i);
                        child.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    }
                    currentWord.setText(frontend.get_candidate_word());
                }
                /*
                currentWord.setText(null);

                for(int i=0; i< gridview.getChildCount(); i++) {
                    TextView child = (TextView) gridview.getChildAt(i);

                    child.setBackgroundColor(Color.parseColor("#FFFFFF"));
                }

                for(int j = 0; j < 16; j++){
                    clicked[j] = false;
                }

                lastPicked = -1;*/
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(frontend.submit_click()) {
                    for(int i=0; i< gridview.getChildCount(); i++) {
                        TextView child = (TextView) gridview.getChildAt(i);
                        if(frontend.tile_state[i] == true) {
                            child.setBackgroundColor(Color.parseColor("#FF0000"));
                        } else {
                            child.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        }
                    }
                    currentWord.setText(frontend.get_candidate_word());
                }
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(frontend.reset_click()) {
                    for(int i=0; i< gridview.getChildCount(); i++) {
                        TextView child = (TextView) gridview.getChildAt(i);
                        if(frontend.tile_state[i] == true) {
                            child.setBackgroundColor(Color.parseColor("#FF0000"));
                        } else {
                            child.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        }
                    }
                    currentWord.setText(frontend.get_candidate_word());
                }
            }
        });
    }
}
