package com.example.miguel.bogglegame;

/**
 * Created by tpatecky on 2/8/2017.
 */

public class Frontend {
    //Data
    Backend backend = new Backend();
    //array of tile clicks for the current candidate word
    int current_submission[] = new int[16];
    //index in current_submission of last user click
    int last_click = -1;
    //state of tiles on grid (t clicked/f not clicked)
    boolean tile_state[] = new boolean[16];

    static final int adjacency[][] = {
            {1,4,5},{0,2,4,5,6},{1,3,5,6,7},{2,6,7},
            {0,1,5,8,9},{0,1,2,4,6,8,9,10},{1,2,3,5,7,9,10,11},{2,3,6,10,11},
            {4,5,9,12,13},{4,5,6,8,10,12,13,14},{5,6,7,9,11,13,14,15},{6,7,10,14,15},
            {8,9,13},{8,9,10,12,14},{9,10,11,13,15},{10,11,14}
    };

    //Constructor
    public Frontend() {
        for(int i = 0; i < 16; i++) {
            tile_state[i] = false;
        }
    }

    //Public Methods

    public String[] get_letters() {
        return backend.letters;
    }

    //returns the current candidate word as a string
    public String get_candidate_word() {
        String word = "";
        for(int i = 0; i <= last_click; i++) {
            word += backend.get_letter(current_submission[i]);
        }
        System.out.println(word);
        return word;
    }

    //check for valid click, if so update candidate word and submissionarray
    //returns true if activity needs to update the tile
    public boolean tile_click(int click_pos) {
        if(last_click >= 0) {
            //user clicks the tile they last clicked, remove that tile from submission
            if(click_pos == current_submission[last_click]) {
                System.out.println("Deactivate tile");
                tile_state[click_pos] = false;
                last_click--;
                return true;
            } else { //user clicked a different tile, check for adjacency & not already clicked
                if(tile_state[click_pos] == true) { //already active
                    System.out.println("Tile already active");
                    return false;
                } else if(is_adjacent(current_submission[last_click], click_pos)) {
                    System.out.println("clicked adjacent tile");
                    tile_state[click_pos] = true;
                    last_click++;
                    current_submission[last_click] = click_pos;
                    return true;
                } else {
                    System.out.println("clicked non-adjacent tile");
                    return false;
                }
            }
        } else { //first click of this submission
            System.out.println("First click.");
            tile_state[click_pos] = true;
            last_click = 0;
            current_submission[last_click] = click_pos;
            return true;

        }
    }

    public boolean clear_click() {
        System.out.println("clear submission");
        for(int i = 0; i < 16; i++) {
            tile_state[i] = false;
        }
        last_click = -1;
        return true;
    }

    public boolean submit_click() {
        System.out.println("Clicked the submit button");
        if(last_click >= 2) { //more than 3 letters long
            int result = backend.submit_word(current_submission);
            if(result == 0) { //success
                System.out.println("Word submission successful");
                clear_click();
                return true;
            }
            else if(result == 2) { //word already submitted
                System.out.println("Word submission failed, word already in list.");
                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean reset_click() {
        System.out.println("Reset button clicked");
        backend.reset_game();
        clear_click();
        return true;
    }

    //Private Methods
    private boolean is_adjacent(int pos1, int pos2) {
        for(int i = 0; i < adjacency[pos1].length; i++) {
            if(adjacency[pos1][i] == pos2) {
                return true;
            }
        }
        return false;
    }


}
