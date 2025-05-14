package com.example.noteapp.myDatabase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.noteapp.MainActivity;
import com.example.noteapp.adapter.UserAdapter;
import com.example.noteapp.custom.CursorSpan;
import com.example.noteapp.custom.CustomEditText;
import com.example.noteapp.interfacePackage.NoteUpdateListener;
import com.example.noteapp.interfacePackage.OnCheckExistsSharedUser;
import com.example.noteapp.interfacePackage.OnDataSyncListener;
import com.example.noteapp.interfacePackage.OnEmailCheckListener;
import com.example.noteapp.interfacePackage.OnEmailExists;
import com.example.noteapp.interfacePackage.OnSharedNotePermission;
import com.example.noteapp.interfacePackage.OnSharedUserUpdate;
import com.example.noteapp.interfacePackage.PermissionCallback;
import com.example.noteapp.model.NoteContent;
import com.example.noteapp.model.NoteModel;
import com.example.noteapp.model.SharedNote;
import com.example.noteapp.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class FirebaseSyncHelper {
    private Context mContext;
    private ValueEventListener noteDeletionListener,
            sharedNoteUpdationListener, sharedNoteDeletetionListener,
            listenNote, listenSharedNote, titleListener, contentListener;
    private DatabaseReference titleRef, contentRef;
    private DatabaseReference noteRef, sharedNoteRef;
    public FirebaseSyncHelper(Context context) {
        this.mContext = context;
    }
    public void listenForDeleteShareUserOnNote(Context context, String noteId, String userId) {
        DatabaseReference sharedNoteRef = FirebaseDatabase.getInstance()
                .getReference("sharedNote").child(noteId);

        sharedNoteDeletetionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean userExists = false;

                for (DataSnapshot shareIdSnapshot : snapshot.getChildren()) {
                    String sharedUser = shareIdSnapshot.child("sharedUser").getValue(String.class);

                    if (sharedUser != null && sharedUser.equals(userId)) {
                        userExists = true;
                        break;
                    }
                }

                // Nếu userId không còn trong danh sách sharedUser, chuyển về MainActivity
                if (!userExists) {
                    Log.d("Firebase", "User " + userId + " không còn quyền, chuyển về MainActivity");
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Lỗi khi lắng nghe sharedNote", error.toException());
            }
        };

        sharedNoteRef.addValueEventListener(sharedNoteDeletetionListener);
    }
    public void stopSharedNoteDeletionListener(String noteId){
        if (sharedNoteRef != null && sharedNoteDeletetionListener != null){
            sharedNoteRef.child(noteId).removeEventListener(sharedNoteDeletetionListener);
        }
    }
    public void listenForNoteDeletion(Context context, String noteId) {
        noteRef = FirebaseDatabase.getInstance().getReference("notes").child(noteId);

        noteDeletionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) { // Nếu ghi chú bị xóa
                    deleteShareNoteOnFirebase(noteId);
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    ((Activity) context).startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to listen for note deletion", error.toException());
            }
        };

        noteRef.addValueEventListener(noteDeletionListener);
    }
    public void stopListeningForNoteDeletion(String noteId) {
        if (noteRef != null && noteDeletionListener != null) {
            noteRef.child(noteId).removeEventListener(noteDeletionListener);
        }
    }
    public void listenforNoteTitle(String noteId, NoteUpdateListener listener){
        titleRef = FirebaseDatabase.getInstance().getReference("notes").child(noteId).child("title");
        titleListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String newTitle = snapshot.getValue(String.class);
                if (newTitle != null && listener != null) {
                    Log.d("DEBUG TITLE", "onDataChange: " + newTitle);
                    listener.onTitleUpdated(newTitle);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        titleRef.addValueEventListener(titleListener);
    }
    public void listenForNoteContent(String noteId, NoteUpdateListener listener) {
        contentRef = FirebaseDatabase.getInstance().getReference("notes")
                                        .child(noteId).child("content");

        contentListener  = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                List<Object> contentList = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String type = child.child("type").getValue(String.class);

                    if ("text".equals(type)) {
                        String textContent = child.child("textContent").getValue(String.class);
                        contentList.add(textContent != null ? textContent : "");
                    } else if ("table".equals(type)) {
                        List<List<String>> tableData = new ArrayList<>();
                        for (DataSnapshot row : child.child("tableContent").getChildren()) {
                            List<String> rowData = new ArrayList<>();
                            for (DataSnapshot cell : row.getChildren()) {
                                rowData.add(cell.getValue(String.class));
                            }
                            tableData.add(rowData);
                        }
                        contentList.add(tableData);
                    }
                }

                if (listener != null) {
                    listener.onContentUpdated(contentList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to listen for updates", error.toException());
            }
        };

        contentRef.addValueEventListener(contentListener);
    }
    public void stopListeningForNoteUpdation(String noteId){
        if (titleRef != null && titleListener != null) {
            titleRef.removeEventListener(titleListener);
        }
        if (contentRef != null && contentListener != null) {
            contentRef.removeEventListener(contentListener);
        }
    }
    public void listenForSharedUserNote(String noteId, OnSharedUserUpdate callback) {
        sharedNoteRef = FirebaseDatabase.getInstance().getReference("sharedNote").child(noteId);
        sharedNoteUpdationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> tempUserList = Collections.synchronizedList(new ArrayList<>());
                int total = (int) snapshot.getChildrenCount();
                AtomicInteger counter = new AtomicInteger(total);
                if (total == 0){
                    callback.updateSharedUser(null);
                    return;
                }
                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                for (DataSnapshot sharedNoteSnapshot : snapshot.getChildren()) {
                    SharedNote sharedNote = sharedNoteSnapshot.getValue(SharedNote.class);
                    if (sharedNote == null || sharedNote.getSharedUser() == null) {
                        if (counter.decrementAndGet() == 0) {
                            callback.updateSharedUser(tempUserList);
                        }
                        continue;
                    }

                    usersRef.child(sharedNote.getSharedUser()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                User user = snapshot.getValue(User.class);
                                if (user != null) {
                                    tempUserList.add(user);
                                }
                            }
                            if (counter.decrementAndGet() == 0) {
                                callback.updateSharedUser(tempUserList);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Lỗi khi lấy User: " + error.getMessage());
                            if (counter.decrementAndGet() == 0) {
                                callback.updateSharedUser(tempUserList);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Lỗi khi lắng nghe sharedNote", error.toException());
            }
        };
        sharedNoteRef.addValueEventListener(sharedNoteUpdationListener);
    }
//    public void listenForSharedUserNote(String noteId, Consumer<List<User>> callback) {
//        sharedNoteRef = FirebaseDatabase.getInstance().getReference("sharedNote").child(noteId);
//        sharedNoteUpdationListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                List<String> userIds = new ArrayList<>();
//                for (DataSnapshot sharedNoteSnapshot : snapshot.getChildren()) {
//                    SharedNote sharedNote = sharedNoteSnapshot.getValue(SharedNote.class);
//                    if (sharedNote != null) {
//                        userIds.add(sharedNote.getSharedUser());
//                    }
//                }
//
//                if (userIds.isEmpty()) {
//                    callback.accept(new ArrayList<>()); // Không có user nào, gọi callback ngay
//                }
//
//                List<User> tempUserList = Collections.synchronizedList(new ArrayList<>());
//                AtomicInteger counter = new AtomicInteger(userIds.size());
//                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
//
//                for (String userId : userIds) {
//                    usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            if (snapshot.exists()) {
//                                User user = snapshot.getValue(User.class);
//                                if (user != null) {
//                                    tempUserList.add(user);
//                                }
//                            }
//                            if (counter.decrementAndGet() == 0) { // Khi tất cả user đã tải xong
//                                callback.accept(new ArrayList<>(tempUserList));
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                            Log.e("Firebase", "Lỗi khi lấy User: " + error.getMessage());
//                            if (counter.decrementAndGet() == 0) {
//                                callback.accept(new ArrayList<>(tempUserList));
//                            }
//                        }
//                    });
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e("Firebase", "Lỗi khi lắng nghe sharedNote", error.toException());
//            }
//        };
//        sharedNoteRef.addValueEventListener(sharedNoteUpdationListener);
//    }
    public void stopListeningForSharedNote(String noteId){
        if (sharedNoteRef != null && sharedNoteUpdationListener != null){
            sharedNoteRef.child(noteId).removeEventListener(sharedNoteUpdationListener);
        }
    }

    public void updateFirebaseNote(String noteId, String field, String value) {
    if (noteId == null || field == null || value == null) {
        Log.e("Firebase", "Invalid parameters: noteId, field, or value is null");
        return;
    }

    DatabaseReference noteRef = FirebaseDatabase.getInstance()
            .getReference("notes").child(noteId);

    Map<String, Object> updates = new HashMap<>();
    updates.put(field, value);

    noteRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> Log.d("Firebase", field + " updated successfully"))
            .addOnFailureListener(e -> Log.e("Firebase", "Failed to update " + field, e));
}
    public void updateFirebaseSharedNote(String noteId, String type, String userId){
        DatabaseReference shareNoteRef = FirebaseDatabase.getInstance().getReference("sharedNote").child(noteId);
        if (type.equals("add")){
            String sharedId = shareNoteRef.push().getKey();
            if (sharedId == null) return;

            SharedNote sharedNote = new SharedNote(sharedId, noteId, 0, userId);

            shareNoteRef.child(sharedId).setValue(sharedNote)
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "User " + userId + " added with " + 0))
                    .addOnFailureListener(e -> Log.e("Firebase", "Failed to add shared user", e));
        }
        else if (type.equals("remove")) {
            shareNoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot sharedSnapshot : snapshot.getChildren()) {
                        SharedNote sharedUser = sharedSnapshot.getValue(SharedNote.class);
                        Log.d("DEBUG", "User " + sharedUser);
                        if (sharedUser != null && sharedUser.getSharedUser().equals(userId)) {
                            String key = sharedSnapshot.getKey();
                            if (key == null) {
                                Log.e("Firebase", "sharedId is null, cannot remove");
                            }
                            shareNoteRef.child(key).removeValue()
                                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "User " + userId + " removed"))
                                    .addOnFailureListener(e -> Log.e("Firebase", "Failed to remove shared user", e));
                            break;
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Firebase", "Failed to remove shared user", error.toException());
                }
            });
        }
    }
    public void deleteShareUserOnFirebase(String noteId, String userId){
        DatabaseReference sharedNoteRef = FirebaseDatabase.getInstance()
                                            .getReference("sharedNote")
                                            .child(noteId);
        Log.d("Firebase", "Bắt đầu xóa userId: " + userId + " khỏi noteId: " + noteId);
        sharedNoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("Firebase", "Path: sharedNote/" + noteId);

                if (!snapshot.exists()) {
                    Log.e("Firebase", "sharedNote/" + noteId + " không có dữ liệu!");
                }

                for (DataSnapshot shareIdSnapshot : snapshot.getChildren()) {
                    String sharedUser = shareIdSnapshot.child("sharedUser").getValue(String.class);

                    if (sharedUser != null && sharedUser.equals(userId)) {
                        Log.d("Firebase", "Xóa userId: " + userId);

                        // Xóa node chứa userId
                        shareIdSnapshot.getRef().removeValue()
                                .addOnSuccessListener(aVoid -> Log.d("Firebase", "User " + userId + " removed"))
                                .addOnFailureListener(e -> Log.e("Firebase", "Failed to remove shared user", e));

                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Lỗi khi đọc sharedNote", error.toException());
            }
        });


    }
    public void deleteNoteAndSharedNotes(String noteId) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        Map<String, Object> updates = new HashMap<>();
        updates.put("notes/" + noteId, null);
        updates.put("sharedNote/" + noteId, null);
        dbRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Ghi chú và danh sách chia sẻ đã bị xóa"))
                .addOnFailureListener(e -> Log.e("Firebase", "Lỗi khi xóa ghi chú và danh sách chia sẻ", e));
    }
    public void deleteShareNoteOnFirebase(String noteId){
        sharedNoteRef = FirebaseDatabase.getInstance()
                        .getReference("sharedNote")
                        .child(noteId);

        sharedNoteRef.removeValue()
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Đã xóa sharedNote cho noteId: " + noteId))
                .addOnFailureListener(e -> Log.e("Firebase", "Lỗi khi xóa sharedNote", e));
    }
    public void listenForMyNotes(String userId, OnDataSyncListener callback){
        noteRef = FirebaseDatabase.getInstance().getReference("notes");

        listenNote = noteRef.orderByChild("owner").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<NoteModel> tempNotes = new ArrayList<>();
                if (snapshot.exists()){
                    for (DataSnapshot noteSnapshot : snapshot.getChildren()){
                        NoteModel note = noteSnapshot.getValue(NoteModel.class);
                        tempNotes.add(note);
                    }
                }
                callback.onNotesUpdated(tempNotes);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        Log.d("DEBUG", "myNote load success");
    }
    public void listenforSharedNotes(String userId, OnDataSyncListener callback){
        sharedNoteRef = FirebaseDatabase.getInstance().getReference("sharedNote");
        noteRef = FirebaseDatabase.getInstance().getReference("notes");

        listenSharedNote = sharedNoteRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> listId = new ArrayList<>();
                List<NoteModel> tempNotes = new ArrayList<>();
                if (snapshot.exists()){
                    for (DataSnapshot noteId : snapshot.getChildren()){
                        for (DataSnapshot shareData : noteId.getChildren()){
                            SharedNote sharedNote = shareData.getValue(SharedNote.class);
                            if (sharedNote != null && sharedNote.getSharedUser().equals(userId)){
                                listId.add(sharedNote.getNoteId());
                            }
                        }
                    }
                    if (!listId.isEmpty()){
                        noteRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()){
                                    for (DataSnapshot noteSnapshot : snapshot.getChildren()){
                                        NoteModel note = noteSnapshot.getValue(NoteModel.class);
                                        if (note != null && listId.contains(note.getId())){
                                            tempNotes.add(note);
                                        }
                                    }
                                    callback.onNotesUpdated(tempNotes);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }else{
                        Log.d("DEBUG", "sharedNote size" + String.valueOf(tempNotes.size()));
                        callback.onNotesUpdated(tempNotes);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        Log.d("DEBUG", "sharedNote load success");
    }
    public void stoplistenNote(){
        if (noteRef != null && listenNote != null){
            noteRef.removeEventListener(listenNote);
        }
        if (sharedNoteRef != null && listenSharedNote != null){
            sharedNoteRef.removeEventListener(listenSharedNote);
        }
    }
    public void getUser(String userId, OnDataSyncListener callback) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        callback.onUserLoaded(user);
                    }
                } else {
                    Log.d("Firebase", "User không tồn tại");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Lỗi Firebase: " + error.getMessage());
            }
        });
    }
    public void getPermissionOfNoteForSharedUser(String noteId, String userId, OnSharedNotePermission callback){
        DatabaseReference sharedNoteRef = FirebaseDatabase.getInstance().getReference("sharedNote").child(noteId);
        sharedNoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int permission = 0;
                if (snapshot.exists()){
                    for (DataSnapshot shareNoteData: snapshot.getChildren()){
                        SharedNote shareNote = shareNoteData.getValue(SharedNote.class);
                        if (shareNote != null){
                            if (shareNote.getSharedUser().equals(userId)){
                                permission = shareNote.getPermission();
                                break;
                            }
                        }
                    }
                }
                callback.onPermission(permission);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onPermission(0);
            }
        });
    }
    public void updatePermission(String noteId, String userId, int permission){
        DatabaseReference sharedNoteRef = FirebaseDatabase.getInstance().getReference("sharedNote").child(noteId);
        sharedNoteRef.orderByChild("sharedUser").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot sharedNoteData : snapshot.getChildren()) {
                        sharedNoteData.getRef().child("permission").setValue(permission)
                                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Cập nhật quyền thành công"))
                                .addOnFailureListener(e -> Log.e("Firebase", "Lỗi cập nhật quyền: " + e.getMessage()));
                    }
                } else {
                    Log.e("Firebase", "Không tìm thấy userId trong sharedNote");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Lỗi Firebase: " + error.getMessage());
            }
        });
    }
    public void checkIfEmailExists(String noteId, String email, OnEmailCheckListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference noteRef = FirebaseDatabase.getInstance().getReference("notes").child(noteId);
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        noteRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot noteSnapshot) {
                                String owner = noteSnapshot.getValue(NoteModel.class).getOwner();
                                if (owner != null && owner.equals(user.getId())){
                                    listener.onResult(true, user, "owner");
                                }else{
                                    listener.onResult(true, user, null);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                }
                else{listener.onResult(false, null, null);}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onResult(false, null, null);
            }
        });
    }
    public void listenForUpdatePermission(String noteId, String userId, PermissionCallback callback){
        DatabaseReference sharedNoteRef = FirebaseDatabase.getInstance().getReference("sharedNote").child(noteId);
        sharedNoteRef.orderByChild("sharedUser").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot sharedNoteData : snapshot.getChildren()) {
                    Integer permission = sharedNoteData.child("permission").getValue(Integer.class);
                    callback.onPermissionResult(permission);
                    return;
                }

                callback.onPermissionResult(null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onPermissionResult(null);
            }
        });
    }
    public void checkSharedUserForNote(String noteId, String email, OnCheckExistsSharedUser listener){
        DatabaseReference sharedNoteRef = FirebaseDatabase.getInstance().getReference("sharedNote").child(noteId);
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        if (user != null && user.getId() == null) {
                            user.setId(userSnapshot.getKey()); // Gán thủ công ID nếu bị null
                        }

                        Log.d("DEBUG", "user: " + user.getId());

                        sharedNoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                boolean found = false;
                                for (DataSnapshot sharedNoteSnapshot : snapshot.getChildren()) {
                                    SharedNote sharedNote = sharedNoteSnapshot.getValue(SharedNote.class);
                                    if (sharedNote != null && sharedNote.getSharedUser().equals(user.getId())) {
                                        Log.d("DEBUG", "sharedNote: " + true);
                                        listener.checkExists(true);
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    Log.d("DEBUG", "sharedNote: " + false);
                                    listener.checkExists(false);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });

                        break; // Vì chỉ cần xử lý user đầu tiên match email
                    }
                } else {
                    listener.checkExists(false);
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void checkEmailExists(String email, OnEmailExists listener) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
        userRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Nếu snapshot tồn tại (tức là có ít nhất 1 user với email khớp)
                        if (snapshot.exists()) {
                            listener.onResult(true);
                        } else {
                            listener.onResult(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onResult(false);
                    }
                });
    }
}
