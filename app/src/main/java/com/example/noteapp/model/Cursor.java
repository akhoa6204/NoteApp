package com.example.noteapp.model;

public class Cursor {
    private String type;   // Loại con trỏ (title, text, table)
    private int row;       // Dòng (dành cho bảng)
    private int column;    // Cột (dành cho bảng)
    private int position;  // Vị trí con trỏ trong EditText hoặc trong bảng
    private int offset;    // Vị trí offset (dùng để tính toán khi có nhiều EditText hoặc bảng)

    // Constructor, getters và setters
    public Cursor(String type, int row, int column, int position, int offset) {
        this.type = type;
        this.row = row;
        this.column = column;
        this.position = position;
        this.offset = offset;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}

