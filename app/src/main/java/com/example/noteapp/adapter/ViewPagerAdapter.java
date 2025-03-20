package com.example.noteapp.adapter;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.noteapp.FragmentNoteList;
import com.example.noteapp.model.NoteModel;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private final List<FragmentNoteList> fragmentList = new ArrayList<>(); // Danh sách lưu các Fragment
    private List<NoteModel> myNotes, sharedNotes;
    private final String userId;
    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<NoteModel> myNotes, List<NoteModel> sharedNotes, String userId) {
        super(fragmentActivity);
        this.myNotes = myNotes;
        this.sharedNotes = sharedNotes;
        this.userId = userId;
        fragmentList.add(FragmentNoteList.newInstance(myNotes, userId));
        fragmentList.add(FragmentNoteList.newInstance(sharedNotes, userId));
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }

    // Reset chế độ chọn
    public void resetSelectionMode() {
        for (FragmentNoteList fragment : fragmentList) {
            fragment.resetSelectionMode();
        }
    }
    public List<NoteModel> getSelectedItems(int position) {
        if (position >= 0 && position < fragmentList.size()) {
            return fragmentList.get(position).getSelectedItems();
        }
        return new ArrayList<>();
    }
    public boolean getSizeSelectedItems(int position){
        return fragmentList.get(position).getSizeSelectedItems();
    }

    public List<NoteModel> getListNote(int position) {
        return fragmentList.get(position).getListNote();
    }
    // Thêm phương thức này để cập nhật dữ liệu
    @SuppressLint("NotifyDataSetChanged")
    public void updateDataMyNotes(List<NoteModel> newMyNotes) {
        Log.d("DEBUG", "updateDataMyNotes được gọi, số lượng: " + newMyNotes.size());

        if (!fragmentList.isEmpty() && fragmentList.get(0) != null && fragmentList.get(0).isAdded()) {
            fragmentList.get(0).updateNotes(newMyNotes);
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    public void updateDataSharedNotes(List<NoteModel> newSharedNotes) {
        Log.d("DEBUG", "updateDataSharedNotes được gọi, số lượng: " + newSharedNotes.size());

        if (!fragmentList.isEmpty() && fragmentList.get(1) != null && fragmentList.get(1).isAdded()) {
            fragmentList.get(1).updateNotes(newSharedNotes);
        }
    }

}



