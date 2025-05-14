package com.example.noteapp.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.noteapp.PageNote;
import com.example.noteapp.SelectionMode;
import com.example.noteapp.interfacePackage.OnDataSyncListener;
import com.example.noteapp.model.NoteModel;
import com.example.noteapp.R;
import com.example.noteapp.model.User;
import com.example.noteapp.myDatabase.FirebaseSyncHelper;

import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends BaseAdapter {
    private final Context mContext;
    private final List<NoteModel> listNote;
    private final SelectionMode selectionMode;
    private final List<NoteModel> selectedItems = new ArrayList<>();
    public final String userId;
    public NoteAdapter(Context mContext, List<NoteModel> listNote, SelectionMode selectionMode, String userId){
        this.mContext=mContext;
        this.listNote=listNote;
        this.selectionMode = selectionMode;
        this.userId = userId;
    }
    @Override
    public int getCount(){
        return listNote.size();
    }
    @Override
    public Object getItem(int i){
        return null;
    }
    @Override
    public long getItemId(int i){
        return 0;
    }
    public List<NoteModel> getListNote(){
        return listNote;
    }
    private String searchQuery = "";
    public void setSearchQuery(String query) {
        this.searchQuery = query;
        notifyDataSetChanged();
    }
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(final int i, View convertView, ViewGroup viewGroup) {
        View rowView = convertView;
        ViewHolder holder;

        boolean checkOwner = userId.equals(listNote.get(i).getOwner());
        FirebaseSyncHelper syncHelper = new FirebaseSyncHelper(mContext);

        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            rowView = inflater.inflate(R.layout.item_note, viewGroup, false);

            holder = new ViewHolder();
            holder.tvTitle = rowView.findViewById(R.id.tvTitle);
            holder.tvOwner = rowView.findViewById(R.id.tvOwner);
            holder.tvCreated_at = rowView.findViewById(R.id.tvCreated_at);
            holder.tvTag = rowView.findViewById(R.id.tvTag);
            holder.cbSelect = rowView.findViewById(R.id.cbSelect);
            rowView.setTag(holder);
        }
        else {
            holder = (ViewHolder) rowView.getTag();
        }

        NoteModel note = listNote.get(i);
        Spanned spannedText = translateSpanned(note.getTitle());

        holder.tvTitle.setText(spannedText);

        if (checkOwner) {
            holder.tvOwner.setVisibility(View.GONE);
            holder.tvCreated_at.setVisibility(View.VISIBLE);
            holder.tvTag.setVisibility(View.VISIBLE);

            holder.tvCreated_at.setText(note.getCreatedAt());
            holder.tvTag.setText(note.getTag());
        }
        else {
            holder.tvOwner.setVisibility(View.VISIBLE);
            holder.tvCreated_at.setVisibility(View.GONE);
            holder.tvTag.setVisibility(View.GONE);


            holder.tvOwner.setText(R.string.loading);

            syncHelper.getUser(note.getOwner(), new OnDataSyncListener() {
                @Override
                public void onNotesUpdated(List<NoteModel> updatesNotes) {}
                @Override
                public void onUserLoaded(User user) {
                    if (user != null) {
                        String name = user.getName();
                        holder.tvOwner.setText(name + " " + mContext.getString(R.string.shared));
                    }
                }

                @Override
                public void onNoteLoaded(NoteModel note) {

                }

                @Override
                public void onSharedNoteLoaded(List<User> sharedUserList) {

                }
            });

        }

        selectionMode.getSelectionMode().observe((LifecycleOwner) mContext, isSelection -> {
            holder.cbSelect.setVisibility(isSelection ? View.VISIBLE : View.GONE);
        });

        holder.cbSelect.setChecked(selectedItems.contains(note));

        rowView.setOnClickListener(v -> {
            if (Boolean.FALSE.equals(selectionMode.getSelectionMode().getValue())) {
                Intent intent = new Intent(mContext, PageNote.class);
                intent.putExtra("KEY_NOTE_ID", note.getId());
                intent.putExtra("OWNER", note.getOwner());
                ((Activity) mContext).startActivity(intent);
            } else {
                if (selectedItems.contains(note)) {
                    selectedItems.remove(note);
                    holder.cbSelect.setChecked(false);
                } else {
                    selectedItems.add(note);
                    holder.cbSelect.setChecked(true);
                }
            }
        });
        holder.cbSelect.setOnClickListener(v -> {
            if (selectedItems.contains(note)) {
                selectedItems.remove(note);
                holder.cbSelect.setChecked(false);
            } else {
                selectedItems.add(note);
                holder.cbSelect.setChecked(true);
            }
        });
        rowView.setOnLongClickListener(v -> {
            if(Boolean.TRUE.equals(selectionMode.getSelectionMode().getValue())){
                resetSelection();
            }else{
                selectedItems.clear();
                selectionMode.setSelectionMode(true);
            }
            notifyDataSetChanged();
            return true;
        });

        return rowView;
    }
    public boolean getSizeSelectItems(){
        return selectedItems.isEmpty();
    }
    public List<NoteModel> getSelectedItems(){
        return selectedItems;
    }
    public void resetSelection() {
        selectionMode.setSelectionMode(false);
        selectedItems.clear();
        notifyDataSetChanged();
    }
    static class ViewHolder {
        TextView tvTitle, tvOwner,tvCreated_at, tvTag;
        CheckBox cbSelect;
    }
    public Spanned translateSpanned(String text) {
        if (text == null) return new SpannableString("");

        Spanned spanned = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT);
        String trimmedText = spanned.toString().replaceAll("\n$", "");

        SpannableString spannable = new SpannableString(trimmedText);
        TextUtils.copySpansFrom(spanned, 0, trimmedText.length(), null, spannable, 0);

        if (!TextUtils.isEmpty(searchQuery)) {
            String lowerText = trimmedText.toLowerCase();
            String lowerQuery = searchQuery.toLowerCase();

            int index = lowerText.indexOf(lowerQuery);
            while (index >= 0) {
                int end = index + lowerQuery.length();
                spannable.setSpan(
                        new BackgroundColorSpan(Color.parseColor("#FFD95F")),
                        index, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                index = lowerText.indexOf(lowerQuery, end);
            }
        }

        return spannable;
    }


}
