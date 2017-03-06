package com.example.miguel.bogglegame.AppLogic;

/**
 * Created by Tristan on 2/27/2017.
 * This class is used to store high scores
 */

public class User implements Comparable<User> {
    //All values are public so we can change them easily
    public String name;
    public int score;

    //Constructors
    User(int score) {
        this.score = score;
    }
    User(String name) {
        this.name = name;
    }
    User(String name, int score) {
        this.name = name;
        this.score = score;
    }

    /**
     * Used to compare two Users by their scores
     * Returns 1 if this.score is bigger
     * Returns -1 if this.score is smaller or equal
     * (other User has precedence over this user since they were in the high scores first)
     */
    @Override
    public int compareTo(User other) {
        if(this.score > other.score)
            return 1;
        else
            return -1;
    }
}
