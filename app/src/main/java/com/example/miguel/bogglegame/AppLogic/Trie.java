package com.example.miguel.bogglegame.AppLogic;

class Trie {

    private Node root;

    public Trie() {

        root = new Node();
    }

    // add string into trie
    public void add(String string) {

        Node node = root;

        for(int i = 0; i < string.length(); i++){

            char c = string.charAt(i);
            int subRootIndex = c - 'a';

            if(node.aTozNodeArray[subRootIndex] == null){

                Node tempNode = new Node();
                node.aTozNodeArray[subRootIndex] = tempNode;
                node = tempNode;
            }else{

                node = node.aTozNodeArray[subRootIndex];
            }
        }

        node.isEnd=true;
    }

    // Return true if a string in trie starts with prefix. Else, return false
    public boolean hasPrefix(String prefix) {

        if(searchNode(prefix) == null){
            return false;
        }else{
            return true;
        }
    }

    // search for string in trie
    // if string is in trie, return true. Else, return false
    // this method uses searchNode
    public boolean search(String string) {

        Node p = searchNode(string);

        if(p == null){
            return false;
        }else{

            if(p.isEnd){
                return true;
            }
        }

        return false;
    }

    //return node containing the last letter of string s
    //if no such node exist, return null
    private Node searchNode(String s){

        Node node = root;

        for( int i=0; i < s.length(); i++){

            char c= s.charAt(i);
            int subRootIndex = c - 'a';

            if(node.aTozNodeArray[subRootIndex] != null){

                node = node.aTozNodeArray[subRootIndex];
            }else{

                return null;
            }
        }

        if(node == root) {

            return null;
        }

        return node;
    }

    private class Node {

        Node[] aTozNodeArray;
        boolean isEnd;

        public Node() {
            this.aTozNodeArray = new Node[26];      //references for child nodes representing a through z
        }
    }
}


