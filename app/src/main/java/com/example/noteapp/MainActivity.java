package com.example.noteapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.noteapp.adapter.ViewPagerAdapter;
import com.example.noteapp.interfacePackage.OnDataSyncListener;
import com.example.noteapp.method.TimeUtil;
import com.example.noteapp.model.Cursor;
import com.example.noteapp.model.NoteContent;
import com.example.noteapp.model.NoteModel;
import com.example.noteapp.model.SharedNote;
import com.example.noteapp.model.User;
import com.example.noteapp.myDatabase.FirebaseSyncHelper;
import com.example.noteapp.settings.UserSession;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter adapter;
    private TextView btnAdd, btnUser, btnDelete;
    private List<NoteModel> selectedItems, listNote;
    private UserSession userSession;
    private SelectionMode selectionMode;
    private String userId;
    private List<NoteModel> myNotes, shareNotes;
    private FirebaseSyncHelper syncHelper;
    private EditText edSearch;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        String email = getIntent().getStringExtra("email");
        if(email != null){
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
            userRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()){
                        for (DataSnapshot userSnapshot : snapshot.getChildren()){
                            userId = userSnapshot.getKey();
                            userSession.saveUserSession(getBaseContext(), userId);
                            break;
                        }
                        loadNotes(myNotes, shareNotes);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });

        }
        else{
            userId = userSession.getUserSession(this);
            loadNotes(myNotes, shareNotes);
        }

        adapter = new ViewPagerAdapter(this, myNotes, shareNotes, userId);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? R.string.my_note : R.string.shared_note);
        }).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                adapter.resetSelectionMode();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        selectionMode.getSelectionMode().observe(this, isSelectionMode -> {
            btnDelete.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        });

        btnAdd.setOnClickListener(this);
        btnUser.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        edSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                Log.d("DEBUG", "query: "+query);
                searchNotesByTitle(query);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
    private void searchNotesByTitle(String query) {
        int currentTab = viewPager.getCurrentItem();
        List<NoteModel> originalList = currentTab == 0 ? myNotes : shareNotes;

        if (query.isEmpty()) {
            adapter.setSearchQuery("");
            if (currentTab == 0) {
                adapter.updateDataMyNotes(new ArrayList<>(originalList));
            } else {
                adapter.updateDataSharedNotes(new ArrayList<>(originalList));
            }
            return;
        }

        List<NoteModel> filteredList = new ArrayList<>(originalList);
        filteredList.sort((a, b) -> {
            boolean aMatch = a.getTitle().toLowerCase().contains(query.toLowerCase());
            boolean bMatch = b.getTitle().toLowerCase().contains(query.toLowerCase());
            return Boolean.compare(!aMatch, !bMatch);
        });

        adapter.setSearchQuery(query);
        if (currentTab == 0) {
            adapter.updateDataMyNotes(filteredList);
        } else {
            adapter.updateDataSharedNotes(filteredList);
        }
    }


    public void init(){
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        btnAdd = findViewById(R.id.btnAdd);
        btnUser = findViewById(R.id.btnUser);
        btnDelete = findViewById(R.id.btnDelete);
        myNotes = new ArrayList<NoteModel>();
        shareNotes = new ArrayList<NoteModel>();
        syncHelper = new FirebaseSyncHelper(this);
        selectionMode = new ViewModelProvider(this).get(SelectionMode.class);
        userSession = new UserSession();
        edSearch = (EditText) findViewById(R.id.edSearch);
    }
    @Override
    protected void onResume() {
        super.onResume();
        viewPager.postDelayed(() -> loadNotes(myNotes, shareNotes), 500);
    }
    @Override
    protected void onStop() {
        super.onStop();
        syncHelper.stoplistenNote();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        syncHelper.stoplistenNote();
    }
    private void loadNotes(List<NoteModel> myNotes, List<NoteModel> shareNotes) {
        Log.d("DEBUG", "userId: " + userId);
        syncHelper.listenForMyNotes(userId, new OnDataSyncListener() {
            @Override
            public void onNotesUpdated(List<NoteModel> updatesNotes) {
                myNotes.clear();
                myNotes.addAll(updatesNotes);
                adapter.updateDataMyNotes(myNotes);
                Log.d("DEBUG", "myNotes: " + myNotes);
            }

            @Override
            public void onUserLoaded(User user) {

            }

            @Override
            public void onNoteLoaded(NoteModel note) {

            }

            @Override
            public void onSharedNoteLoaded(List<User> sharedUserList) {

            }


        });
        syncHelper.listenforSharedNotes(userId, new OnDataSyncListener() {
            @Override
            public void onNotesUpdated(List<NoteModel> updatesNotes) {
                shareNotes.clear();
                shareNotes.addAll(updatesNotes);
                adapter.updateDataSharedNotes(shareNotes);
                Log.d("DEBUG", "shareNotes: " + shareNotes);

            }

            @Override
            public void onUserLoaded(User user) {

            }

            @Override
            public void onNoteLoaded(NoteModel note) {

            }

            @Override
            public void onSharedNoteLoaded(List<User> sharedUserList) {

            }
        });
    }
    private void onAccount(){
        Intent intent = new Intent(this, Account.class);
        startActivity(intent);
    }
    @Override
    public void onClick(View view){
        if(view.getId() == R.id.btnAdd){
            onAdd();
        } else if (view.getId() == R.id.btnUser) {
            onAccount();
        }else if(view.getId() == R.id.btnDelete){
            boolean checkSizeSelectedItem = adapter.getSizeSelectedItems(viewPager.getCurrentItem());
            selectedItems=adapter.getSelectedItems(viewPager.getCurrentItem());
            listNote=adapter.getListNote(viewPager.getCurrentItem());

            if(!checkSizeSelectedItem){
                showPopupWindow(selectedItems, listNote);
            }else{
                Toast.makeText(this, "List Delete is empty", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void showPopupWindow(List<NoteModel> selectedItems, List<NoteModel> listNote) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.check_remove, null);
        PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true);
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);

        Button btnAccept = (Button) popupView.findViewById(R.id.btnAccept);
        Button btnBack = (Button) popupView.findViewById(R.id.btnBack);
        RelativeLayout wrap = (RelativeLayout) popupView.findViewById(R.id.wrap);
        LinearLayout popup = (LinearLayout) popupView.findViewById(R.id.popup);

        wrap.setOnClickListener(v1 -> {
            popupWindow.dismiss();
        });
        popup.setOnClickListener(v1 -> {
        });

        btnAccept.setOnClickListener(v -> {
            ArrayList<NoteModel> noteErrorList = new ArrayList<>();
            List<NoteModel> deletedNotes = new ArrayList<>();
            for (NoteModel note : selectedItems) {
                boolean checkPermission = note.getOwner().equals(userId);
                if(checkPermission){
                    DatabaseReference db_notes = FirebaseDatabase.getInstance().getReference("notes");
                    DatabaseReference sharedNoteRef = FirebaseDatabase.getInstance().getReference("sharedNote");
                    db_notes.child(note.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> deletedNotes.add(note))
                            .addOnFailureListener(e -> noteErrorList.add(note));
                    sharedNoteRef.child(note.getId()).removeValue();
                }else{
                    DatabaseReference db_sharedNotes = FirebaseDatabase.getInstance().getReference("sharedNotes");
                    db_sharedNotes.child(note.getId()).child("shareUsers").child(userId).removeValue()
                            .addOnSuccessListener(aVoid -> deletedNotes.add(note))
                            .addOnFailureListener(e -> noteErrorList.add(note));
                }
            }
            listNote.removeAll(deletedNotes);

            if (!noteErrorList.isEmpty()) {
                Toast.makeText(this, "Remove error", Toast.LENGTH_SHORT).show();
            }

            adapter.resetSelectionMode();
            popupWindow.dismiss();
        });
        btnBack.setOnClickListener(v -> {
            popupWindow.dismiss();
        });

    }
    private void onAdd(){
        DatabaseReference noteRef = FirebaseDatabase.getInstance().getReference("notes");

        // Tạo key tự động cho ghi chú mới
        String noteId = noteRef.push().getKey();

        String currentTime = TimeUtil.getCurrentTimeString();

        NoteContent content = new NoteContent(Html.toHtml(new SpannableString("Nội dung ghi chú mới"), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE));
        List<NoteContent> contentList = new ArrayList<>();
        contentList.add(content);

        NoteModel newNote = new NoteModel(noteId,
                "Tiêu đề mới",
                contentList,
                null,
                userId,
                currentTime,
                currentTime);

        noteRef.child(noteId).setValue(newNote)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseNotes", "Ghi chú đã được thêm!");
                })
                .addOnFailureListener(e -> Log.e("FirebaseNotes", "Lỗi khi thêm ghi chú!", e));

        Intent intent = new Intent(this, PageNote.class);
        intent.putExtra("KEY_NOTE_ID", noteId);
        intent.putExtra("OWNER", userId);
        startActivity(intent);
        finish();
    }

}


