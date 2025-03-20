package com.example.noteapp.model;

import java.io.Serializable;
import java.util.List;

public class NoteContent implements Serializable{
    private String type;
    private String textContent; // Nếu type = "text"
    private List<List<String>> tableContent; // Nếu type = "table"

    // Constructor cho text
    public NoteContent() {
    }
    public NoteContent(String text) {
        this.type = "text";
        this.textContent = text;
        this.tableContent = null;
    }

    // Constructor cho table
    public NoteContent(List<List<String>> table) {
        this.type = "table";
        this.tableContent = table;
        this.textContent = null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public void setTableContent(List<List<String>> tableContent) {
        this.tableContent = tableContent;
    }

    public String getTextContent() {
        return textContent;
    }

    public List<List<String>> getTableContent() {
        return tableContent;
    }
}
