package com.example.miguel.bogglegame.AppLogic;

import java.io.ObjectStreamException;
import java.util.ArrayList;

/**
 * Created by pkara on 3/9/2017.
 */

public class CutThroatWord {

    public int[] sequenceOfTiles;
    public String word;

    public CutThroatWord(int[] sequenceOfTiles, String word) {
        this.sequenceOfTiles = sequenceOfTiles;
        this.word = word;
    }

    @Override
    public boolean equals(Object o) {

        CutThroatWord other = (CutThroatWord)o;

        if(!word.equals(other.word)){
            return false;
        }

        if(other.sequenceOfTiles.length == sequenceOfTiles.length){

            int index = 0;

            for(int i : sequenceOfTiles){

                if(i != other.sequenceOfTiles[index++]) return false;
            }
        }

        return true;
    }
}
