package com.example.noteapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.noteapp.adapter.UserAdapter;
import com.example.noteapp.interfacePackage.OnDataSyncListener;
import com.example.noteapp.model.NoteModel;
import com.example.noteapp.model.SharedNote;
import com.example.noteapp.model.User;
import com.example.noteapp.myDatabase.Database;
import com.example.noteapp.myDatabase.FirebaseSyncHelper;
import com.example.noteapp.settings.UserSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PageNote extends AppCompatActivity implements View.OnClickListener {
    private TextView btnBack, btMore, btShare;
    private EditText edTitle, edContent;
    private String noteId;
    private final List<User> sharedUserList = new ArrayList<>();
    private NoteModel note;
    private UserSession userSession;
    private String userId;
    private String owner;
    private boolean checkPermission;
    private FirebaseSyncHelper syncHelper;
    private UserAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        initView();

        Intent intent = getIntent();
        if (intent != null ) {
            if (intent.hasExtra("KEY_NOTE_ID")){
                noteId = intent.getStringExtra("KEY_NOTE_ID");
            }
            if (intent.hasExtra("OWNER")){
                owner = intent.getStringExtra("OWNER");
            }
        }

        userId = userSession.getUserSession(this);

        checkPermission = owner.equals(userId);

        Log.d("DEBUG", "owner: " + owner);
        Log.d("DEBUG", "userId: " + userId);

        if (!checkPermission){
            syncHelper.listenForNoteDeletion(this, noteId);
            syncHelper.listenForNoteUpdation(edTitle, edContent, noteId);
            syncHelper.listenForDeleteShareUserOnNote(this, noteId, userId);
        }

        syncHelper.getNote(noteId, new OnDataSyncListener() {
            @Override
            public void onNotesUpdated(List<NoteModel> updatesNotes) {

            }

            @Override
            public void onUserLoaded(User user) {

            }

            @Override
            public void onNoteLoaded(NoteModel noteReturn) {
                note = noteReturn;
                runOnUiThread(() ->{
                    edTitle.setText(note.getTitle());
                    edContent.setText(note.getContent());
                });
            }

            @Override
            public void onSharedNoteLoaded(List<User> sharedUserList) {

            }


        });


        if (checkPermission) {
            edTitle.setEnabled(true);
            edContent.setEnabled(true);
        }
        else {
            edTitle.setEnabled(false);
            edContent.setEnabled(false);
            edTitle.setTextColor(Color.BLACK);
            edContent.setTextColor(Color.BLACK);
        }

        edTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String newTitle = s.toString().trim();
                if (!newTitle.isEmpty()) {
                    syncHelper.updateFirebaseNote(noteId, "title", newTitle);
                }else{
                    syncHelper.updateFirebaseNote(noteId,"title", "Title");
                }
            }
        });
        edContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String newContent = s.toString().trim();
                if (!newContent.isEmpty()) {
                    syncHelper.updateFirebaseNote(noteId, "content", newContent);
                }else{
                    syncHelper.updateFirebaseNote(noteId, "content", "Content");
                }
            }
        });

        btnBack.setOnClickListener(this);
        btMore.setOnClickListener(this);
        btShare.setOnClickListener(this);
    }
    public void initView(){
        btnBack = (TextView) findViewById(R.id.btnBack);
        btMore= (TextView) findViewById(R.id.btMore);
        edTitle =(EditText) findViewById(R.id.edTitle);
        edContent=(EditText) findViewById(R.id.edContent);
        btShare=(TextView) findViewById(R.id.btShare);
        userSession = new UserSession();
        syncHelper = new FirebaseSyncHelper(this);

    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(!checkPermission){
            syncHelper.stopListeningForNoteDeletion();
            syncHelper.stopListeningForNoteUpdation();
            syncHelper.stopSharedNoteDeletionListener();
            syncHelper.stopListeningForSharedNote();
        }
    }
    @Override
    public void onClick(View view){
        if(view.getId() == R.id.btnBack){
            onBack();
        }else if(view.getId() == R.id.btMore){
            showPopUpMenu(view);
        }else if(view.getId() == R.id.btShare){
                showPopup(view);
        }
    }
    private void showPopUpMenu(View anchorView) {
        View popupView = getLayoutInflater().inflate(R.layout.custom_popup_menu, null);
        LinearLayout btnDelete = popupView.findViewById(R.id.btnDelete);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        btnDelete.setOnClickListener(v -> {
            popupWindow.dismiss();  // Đóng popup cũ trước

            // Tạo một popup mới
            View confirmView = getLayoutInflater().inflate(R.layout.check_remove, null);
            PopupWindow confirmPopup = new PopupWindow(
                    confirmView,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    true);
            RelativeLayout wrap = (RelativeLayout) confirmView.findViewById(R.id.wrap);
            LinearLayout popup = (LinearLayout) confirmView.findViewById(R.id.popup);

            wrap.setOnClickListener(v1 -> {
                confirmPopup.dismiss();
            });
            popup.setOnClickListener(v1 -> {
            });

            confirmPopup.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
            Button btnBack = (Button) confirmView.findViewById(R.id.btnBack);
            Button btnAccept= (Button) confirmView.findViewById(R.id.btnAccept);
            btnBack.setOnClickListener(v1 -> {
                confirmPopup.dismiss();
            });
            btnAccept.setOnClickListener(v1 -> {

                if(checkPermission) {
                    syncHelper.deleteNoteAndSharedNotes(noteId);
                }
                else{
                    syncHelper.deleteShareUserOnFirebase(noteId, userId);
                }
                Toast.makeText(this, "Note is removed", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();

            });
        });

        popupWindow.showAtLocation(anchorView, Gravity.TOP | Gravity.END, 20, 250);
    }
    private void onBack(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity((intent));
        finish();
    }
    @SuppressLint("SetTextI18n")
    private void showPopup(View anchorView) {
        View popupView = getLayoutInflater().inflate(R.layout.custom_popup_add_permission, null);
        RelativeLayout wrap = popupView.findViewById(R.id.wrap);
        LinearLayout popup = popupView.findViewById(R.id.popup);
        TextView tvTitle = popupView.findViewById(R.id.tvTitle);
        LinearLayout lrAdd = popupView.findViewById(R.id.lrAdd);
        ListView lvMember = popupView.findViewById(R.id.lvMember);
        EditText edAddMember = popupView.findViewById(R.id.edAddMember);
        Button btnAdd = popupView.findViewById(R.id.btnAdd);

        tvTitle.setText("Chia sẻ " + note.getTitle());

        if (!checkPermission){
            lrAdd.setVisibility(View.GONE);
        }

        // Tạo PopupWindow
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true
        );
        wrap.setOnClickListener(v -> {
            popupWindow.dismiss();
        });

        popup.setOnClickListener(v -> {
        });
        syncHelper.getSharedNote(noteId, new OnDataSyncListener() {
            @Override
            public void onNotesUpdated(List<NoteModel> updatesNotes) {

            }

            @Override
            public void onUserLoaded(User user) {

            }

            @Override
            public void onNoteLoaded(NoteModel note) {

            }

            @Override
            public void onSharedNoteLoaded(List<User> sharedUserListReturn) {
                Log.d("DEBUG", "sharedUserListReturn size: " + sharedUserListReturn.size());
                sharedUserList.clear();
                sharedUserList.addAll(sharedUserListReturn); // Cập nhật danh sách mới

                // Kiểm tra adapter đã tồn tại hay chưa
                if (lvMember.getAdapter() == null) {
                    adapter = new UserAdapter(getBaseContext(), sharedUserList, checkPermission, noteId);
                    lvMember.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }

                // Lắng nghe thay đổi từ Firebase
                syncHelper.listenForSharedUserNote(noteId, sharedUserList, adapter);
            }
        });


        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);

            btnAdd.setOnClickListener(v -> {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                String email = edAddMember.getText().toString().trim();
                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    Toast.makeText(getBaseContext(), "Email is not valid", Toast.LENGTH_SHORT).show();
                    edAddMember.setText("");
                    return;
                }

                syncHelper.checkIfEmailExists(email, (exists, user) -> {
                    if (exists) {
                        Toast.makeText(getBaseContext(), "Add User success", Toast.LENGTH_SHORT).show();

                        String shareId = syncHelper.updateFirebaseSharedNote(noteId, "add", user.getId());
                        edAddMember.setText("");

                        sharedUserList.add(user);
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }                    } else {
                        Toast.makeText(getBaseContext(), "Email does not exist", Toast.LENGTH_SHORT).show();
                        edAddMember.setText("");
                    }
                });
            });
    }


}
