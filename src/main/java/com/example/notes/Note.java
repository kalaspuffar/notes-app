package com.example.notes;

import org.json.simple.JSONObject;

public class Note {
    private static int counter = 0;

    private final int id;
    private String text;
    private int votes;

    public Note(String text) {
        this.id = ++counter;
        this.text = text;
        this.votes = 0;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public int getVotes() {
        return votes;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void upvote() {
        this.votes++;
    }

    public void downvote() {
        this.votes--;
    }

    public JSONObject getJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("text", text);
        obj.put("votes", votes);
        return obj;
    }
}
