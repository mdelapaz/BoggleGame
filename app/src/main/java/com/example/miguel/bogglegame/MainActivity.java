package com.example.miguel.bogglegame;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    static final String[] letters = new String[] {
            "A", "B", "C", "D",
            "E", "F", "G", "H",
            "I", "J", "K", "L",
            "M", "N", "O", "P"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final GridView gridview = (GridView) findViewById(R.id.LetterGrid);
        final Button clearButton = (Button) findViewById(R.id.ClearButton);
        final TextView currentWord = (TextView) findViewById(R.id.CurrentWord);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, letters);

        gridview.setAdapter(adapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {

                if(currentWord.getText() == null) {
                    currentWord.setText(((TextView) v).getText());
                }
                else {
                    currentWord.append(((TextView) v).getText());
                }

                v.setBackgroundColor(Color.parseColor("#FF0000"));
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentWord.setText(null);

                for(int i=0; i< gridview.getChildCount(); i++) {
                    TextView child = (TextView) gridview.getChildAt(i);

                    child.setBackgroundColor(Color.parseColor("#FFFFFF"));
                }
            }
        });
    }
}
