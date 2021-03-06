package com.example.miguel.bogglegame;

import java.util.Arrays;

/**
 * Created by tpatecky on 3/9/17.
 */

public class BoggleMessage {
    //type of message
    public int type;

    //if we're telling the client how to build the game
    String[] letters;
    GameMode mode;
    boolean is_multiround;
    int difficulty;

    //if we're playing the game
    int[] word_submission;
    int word_length;

    int score;

    //construct from byte array sent by other phone
    BoggleMessage(byte[] input) {
        //figure out contents of byte stream
        type = (int)input[0];
        byte[] letter;
        switch(type) {
            case MessageType.SupplyBoard:
                letter = new byte[1];
                if((int)input[1] == 0) {
                    mode = GameMode.BasicTwoPlayer;
                } else {
                    mode = GameMode.CutThroatTwoPLayer;
                }
                difficulty = (int)input[2];
                if((int)input[3] == 0) {
                    is_multiround = false;
                } else {
                    is_multiround = true;
                }
                letters = new String[16];
                for(int i=0; i < letters.length; i++){
                    letter[0] = input[i+4];
                    letters[i] = new String(letter);
                }
                break;
            case MessageType.SubmitWord:
            case MessageType.AcceptWord:
                word_length = (int)input[1];
                word_submission = new int[word_length];
                for(int i = 0;i<word_length;i++){
                    word_submission[i] = (int)input[i+2];
                }
                break;
            case MessageType.SendScore:
                score = (int)input[1];
                break;
            case MessageType.HostRoundDone:
                letter = new byte[1];
                letters = new String[16];
                for(int i = 0; i < letters.length; i++) {
                    letter[0] = input[i+1];
                    letters[i] = new String(letter);
                }
                break;
        }
    }

    //construct message with no data
    BoggleMessage(int p_type) {
        type = p_type;
    }
    //construct message with board & game mode
    BoggleMessage(String[] p_letters, GameMode p_mode, int p_difficulty, boolean is_multi) {
        type = MessageType.SupplyBoard;
        letters = p_letters;
        mode = p_mode;
        difficulty = p_difficulty;
        is_multiround = is_multi;
    }
    //construct message with word submission
    BoggleMessage(int ptype, int[] submit, int length) {
        type = ptype;
        word_length = length;
        word_submission = submit;
    }

    //construct message with score
    BoggleMessage(int ptype, int pscore){
        type = ptype;
        score = pscore;
    }

    //construct host is done with round message
    BoggleMessage(int ptype, String[] p_letters) {
        type = ptype;
        letters = p_letters;
    }

    //turn this into a byte array to send across bluetooth
    public byte[] output() {
        byte[] retval;
        byte typebyte = (byte)type;
        switch(type) {
            case MessageType.SupplyBoard:
                byte[] ltrbytes = new byte[letters.length];
                for(int i = 0; i < letters.length; i++){
                    ltrbytes[i] = letters[i].getBytes()[0];
                }
                retval = new byte[4+ltrbytes.length];
                retval[0] = typebyte;
                if(mode == GameMode.BasicTwoPlayer) {
                    retval[1] = 0;
                } else {
                    retval[1] = 1;
                }
                retval[2] = (byte)difficulty;
                if(is_multiround) {
                    retval[3] = 1;
                } else {
                    retval[3] = 0;
                }

                for(int i=0;i<ltrbytes.length;i++) {
                    retval[i+4] = ltrbytes[i];
                }
                break;
            case MessageType.SubmitWord:
            case MessageType.AcceptWord:
                retval = new byte[2+word_length];
                retval[0] = typebyte;
                retval[1] = (byte)word_length;
                for(int i=0;i<word_length;i++) {
                    retval[i+2] = (byte)word_submission[i];
                }
                break;
            case MessageType.SendScore:
                retval = new byte[2];
                retval[0] = typebyte;
                retval[1] = (byte)score;
                break;
            case MessageType.HostRoundDone:
                byte[] bytes = new byte[letters.length];
                for(int i = 0; i < letters.length; i++){
                    bytes[i] = letters[i].getBytes()[0];
                }
                retval = new byte[1+bytes.length];
                retval[0] = typebyte;
                for(int i = 0; i < bytes.length; i++) {
                    retval[i+1] = bytes[i];
                }
                break;
            default: //no data with message
                retval = new byte[1];
                retval[0] = typebyte;
                break;
        }
        return retval;
    }
}