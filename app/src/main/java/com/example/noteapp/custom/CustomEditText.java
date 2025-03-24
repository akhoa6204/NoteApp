package com.example.noteapp.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class CustomEditText extends androidx.appcompat.widget.AppCompatEditText {
    private OnSelectionChangeListener selectionChangeListener;
    private Context context;
    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selStart, selEnd);
        }
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int start, int end);
    }
}
