package com.example.noteapp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.noteapp.adapter.NoteAdapter;
import com.example.noteapp.model.NoteModel;
import com.example.noteapp.settings.UserSession;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FragmentNoteList extends Fragment {
    private static final String ARG_NOTES = "notes";
    private static final String USER_ID = "userId";
    private ArrayList<NoteModel> noteList;
    private NoteAdapter noteAdapter;
    private List<NoteModel> pendingNotes;
    private String userId;
    private String searchQuery = "";

    public static FragmentNoteList newInstance(List<NoteModel> notes, String userId) {
        Log.d("DEBUG", "Khởi tạo FragmentNoteList với số lượng ghi chú: " + notes.size());

        FragmentNoteList fragment = new FragmentNoteList();
        Bundle args = new Bundle();
        args.putSerializable(ARG_NOTES, new ArrayList<>(notes));
        args.putSerializable(USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }
    public void setSearchQuery(String query) {
        this.searchQuery = query;
        if (noteAdapter != null) {
            noteAdapter.setSearchQuery(query);
        }
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("DEBUG", "onCreate() được gọi");

        Bundle args = getArguments();
        if (args != null) {
            Serializable serializable_note = args.getSerializable(ARG_NOTES);
            Serializable serializable_userId = args.getSerializable(USER_ID);
            if (serializable_note instanceof ArrayList) {
                noteList = (ArrayList<NoteModel>) serializable_note;
                userId = (String) serializable_userId;
                Log.d("DEBUG", "Dữ liệu noteList nhận được: " + noteList.size());
                Log.d("DEBUG", "Dữ liệu userId nhận được: " + userId);
            }
        }

        if (noteList == null) {
            noteList = new ArrayList<>();
        }
        if (userId == null){
            userId = new UserSession().getUserSession(requireContext());
        }

        SelectionMode selectionMode = new ViewModelProvider(requireActivity()).get(SelectionMode.class);
        noteAdapter = new NoteAdapter(requireContext(), noteList, selectionMode, userId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("DEBUG", "onCreateView() được gọi");
        View view = inflater.inflate(R.layout.fragment_note_list, container, false);
        ListView lvNotes = view.findViewById(R.id.lvNotes);
        lvNotes.setAdapter(noteAdapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pendingNotes != null) {
            updateNotes(pendingNotes);
            pendingNotes = null;
        }
    }

    public void resetSelectionMode() {
        if (noteAdapter != null) {
            noteAdapter.resetSelection();
        }
    }

    public List<NoteModel> getSelectedItems() {
        return (noteAdapter != null) ? noteAdapter.getSelectedItems() : new ArrayList<>();
    }

    public boolean getSizeSelectedItems() {
        return noteAdapter != null && noteAdapter.getSizeSelectItems();
    }

    public List<NoteModel> getListNote() {
        return (noteAdapter != null) ? noteAdapter.getListNote() : new ArrayList<>();
    }

    public void updateNotes(List<NoteModel> updatedNotes) {
        Log.d("DEBUG", "updateNotes() được gọi với " + updatedNotes.size() + " ghi chú");

        if (isAdded() && noteAdapter != null) {
            noteList.clear();
            noteList.addAll(updatedNotes);
            noteAdapter.notifyDataSetChanged();
            Log.d("DEBUG", "Cập nhật danh sách ghi chú trong Fragment");
        } else {
            pendingNotes = updatedNotes;
            Log.d("DEBUG", "Fragment chưa được attach, lưu tạm dữ liệu");
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (pendingNotes != null) {
            updateNotes(pendingNotes);
            pendingNotes = null;
        }
    }
}
