package com.example.miguel.bogglegame;

/**
 * Created by tpatecky on 3/9/17.
 */

public class BoggleMessage {
    public MessageType type;

    //if we're telling the client how to build the game
    String[] letters;
    GameMode mode;
    int difficulty;

    //if we're playing the game
    int[] word_submission;

    //construct from byte array
    BoggleMessage(byte[] input) {
        //figure out contents of byte stream
    }
    //construct board state to send
    BoggleMessage(String[] letters, String[] word_list) {
        type = MessageType.SupplyBoard;
        letters = letters;
        word_list = word_list;
    }

    //turn this into a byte array to send across bluetooth
    public byte[] output() {
        return null;
    }
}