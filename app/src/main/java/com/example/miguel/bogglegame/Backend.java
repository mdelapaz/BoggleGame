package com.example.miguel.bogglegame;

import com.example.miguel.bogglegame.AppLogic.BoggleBoard;

import java.util.ArrayList;

/**
 * Created by tpatecky on 2/8/2017.
 */

public class Backend {

     String[] letters;
    //0 is easy, 1 is normal, 2 is difficult
     private int difficulty_level = 0;
    //length to a N*N boggle board
    private int boggleBoardLength = 4;

    public Backend(String[] wordsInDictionary){

        BoggleBoard boggle = new BoggleBoard(boggleBoardLength, wordsInDictionary, difficulty_level);
        letters = boggle.exportBoard();
    }

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
    int submit_word(int[] submission, int word_length) {
        String candidate = "";
        for(int i = 0; i < word_length; i++) {
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
