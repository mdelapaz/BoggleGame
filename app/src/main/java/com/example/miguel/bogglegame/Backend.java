package com.example.miguel.bogglegame;

import java.util.ArrayList;

/**
 * Created by tpatecky on 2/8/2017.
 */

public class Backend {
    String[] letters = new String[] {
            "A", "B", "C", "D",
            "E", "F", "G", "H",
            "I", "J", "K", "L",
            "M", "N", "O", "P"
    };

    ArrayList<String> submitted_words = new ArrayList<String>();

    int score = 42;

    //return letter in tile at that grid position
    String get_letter(int tile_pos) {
        return letters[tile_pos];
    }

    //return current score
    String get_score() {
        return Integer.toString(score);
    }

    ArrayList<String> get_submitted_words() {
        return submitted_words;
    }

    //submit word, check vs dictionary
    //ret values:
    //0 - success
    //1 - failure, word does not exist in dictionary
    //2 - failure, word already submitted
    //3 - failure, opponent already submitted this word
    int submit_word(int[] submission) {
        String candidate = "";
        for(int i = 0; i < submission.length; i++) {
            candidate += letters[submission[i]];
        }

        for(int i = 0; i < submitted_words.size(); i++) {
            if(candidate.equals(submitted_words.get(i))) {
                return 2;
            }
        }

        submitted_words.add(candidate);
        return 0;
    }

    //start a new game, clear word list, score, etc
    void reset_game() {
        submitted_words = new ArrayList<String>();
    }
}
