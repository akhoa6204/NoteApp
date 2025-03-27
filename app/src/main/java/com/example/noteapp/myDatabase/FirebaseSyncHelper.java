package com.example.noteapp.myDatabase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.noteapp.MainActivity;
import com.example.noteapp.adapter.UserAdapter;
import com.example.noteapp.interfacePackage.NoteUpdateListener;
import com.example.noteapp.interfacePackage.OnDataSyncListener;
import com.example.noteapp.interfacePackage.OnEmailCheckListener;
import com.example.noteapp.interfacePackage.OnSharedNotePermission;
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
    private ValueEventListener noteDeletionListener, noteUpdationListener,
            sharedNoteUpdationListener, sharedNoteDeletetionListener,
            listenNote, listenSharedNote;
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
    public void stopSharedNoteDeletionListener(){
        if (sharedNoteRef != null && sharedNoteDeletetionListener != null){
            sharedNoteRef.removeEventListener(sharedNoteDeletetionListener);
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
    public void stopListeningForNoteDeletion() {
        if (noteRef != null && noteDeletionListener != null) {
            noteRef.removeEventListener(noteDeletionListener);
        }
    }
    public void listenForNoteUpdation(String noteId, NoteUpdateListener listener) {
        noteRef = FirebaseDatabase.getInstance().getReference("notes").child(noteId);

        noteUpdationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // Cập nhật title
                String newTitle = snapshot.child("title").getValue(String.class);
                if (newTitle != null && listener != null) {
                    listener.onTitleUpdated(newTitle);
                }

                // Cập nhật content (đúng thứ tự)
                DataSnapshot contentSnapshot = snapshot.child("content");
                if (contentSnapshot.exists()) {
                    List<Object> contentList = new ArrayList<>();

                    for (DataSnapshot child : contentSnapshot.getChildren()) {
                        String type = child.child("type").getValue(String.class);

                        if ("text".equals(type)) {
                            String textContent = child.child("textContent").getValue(String.class);
                            contentList.add(textContent != null ? textContent : "");
                        }
                        else if ("table".equals(type)) {
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

                    // Cập nhật giao diện theo đúng thứ tự
                    if (listener != null) {
                        listener.onContentUpdated(contentList);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to listen for updates", error.toException());
            }
        };

        noteRef.addValueEventListener(noteUpdationListener);
    }


    public void stopListeningForNoteUpdation(){
        if (noteRef != null && noteUpdationListener != null){
            noteRef.removeEventListener(noteUpdationListener);
        }
    }
    public void listenForSharedUserNote(String noteId, Consumer<List<User>> callback) {
        sharedNoteRef = FirebaseDatabase.getInstance().getReference("sharedNote").child(noteId);
        sharedNoteUpdationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> userIds = new ArrayList<>();
                for (DataSnapshot sharedNoteSnapshot : snapshot.getChildren()) {
                    SharedNote sharedNote = sharedNoteSnapshot.getValue(SharedNote.class);
                    if (sharedNote != null) {
                        userIds.add(sharedNote.getSharedUser());
                    }
                }

                if (userIds.isEmpty()) {
                    callback.accept(new ArrayList<>()); // Không có user nào, gọi callback ngay
                    return;
                }

                List<User> tempUserList = Collections.synchronizedList(new ArrayList<>());
                AtomicInteger counter = new AtomicInteger(userIds.size());
                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

                for (String userId : userIds) {
                    usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                User user = snapshot.getValue(User.class);
                                if (user != null) {
                                    tempUserList.add(user);
                                }
                            }
                            if (counter.decrementAndGet() == 0) { // Khi tất cả user đã tải xong
                                callback.accept(new ArrayList<>(tempUserList));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Lỗi khi lấy User: " + error.getMessage());
                            if (counter.decrementAndGet() == 0) {
                                callback.accept(new ArrayList<>(tempUserList));
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
    public void stopListeningForSharedNote(){
        if (sharedNoteRef != null && sharedNoteUpdationListener != null){
            sharedNoteRef.removeEventListener(sharedNoteUpdationListener);
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
                                return;
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
                    return;
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
                                Log.d("DEBUG", "userId: " + userId);
                                Log.d("DEBUG", "SharedUser: " + sharedNote.getSharedUser());

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
                                            Log.d("DEBUG", "sharedNote: " + note);
                                        }
                                    }
                                    Log.d("DEBUG", "sharedNote size: " + tempNotes.size());
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
    public void getNote(String noteId, OnDataSyncListener callback) {
        DatabaseReference noteRef = FirebaseDatabase.getInstance().getReference("notes").child(noteId);

        noteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    NoteModel note = snapshot.getValue(NoteModel.class);

                    if (note != null && note.getContent() != null) { // Kiểm tra note và content không null
                        Log.d("DEBUG", "Note content size: " + note.getContent().size());

                        for (int i = 0; i < note.getContent().size(); i++) {
                            NoteContent item = note.getContent().get(i);

                            if (item != null) {
                                Log.d("DEBUG FIREBASE", "Item at index " + i + ": " + item);

                                if (item.getType() != null) {
                                    Log.d("DEBUG FIREBASE", "Item type: " + item.getType());
                                } else {
                                    Log.e("DEBUG FIREBASE", "Item type is null at index " + i);
                                }

                                if ("text".equals(item.getType()) && item.getTextContent() != null) {
                                    Log.d("DEBUG FIREBASE", "Item text content: " + item.getTextContent());
                                } else if ("table".equals(item.getType()) && item.getTableContent() != null) {
                                    Log.d("DEBUG FIREBASE", "Item table content: " + item.getTableContent());
                                } else {
                                    Log.e("DEBUG FIREBASE", "Content is null at index " + i);
                                }
                            } else {
                                Log.e("DEBUG FIREBASE", "Item is null at index " + i);
                            }
                        }
                        callback.onNoteLoaded(note);
                    } else {
                        Log.e("DEBUG", "Note content is null");
                    }
                } else {
                    Log.e("DEBUG", "Snapshot does not exist for noteId: " + noteId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DEBUG FIREBASE", "Database error: " + error.getMessage());
            }
        });
    }
    public void getSharedNote(String noteId, OnDataSyncListener callback) {
        DatabaseReference sharedNoteRef = FirebaseDatabase.getInstance().getReference("sharedNote").child(noteId);
        sharedNoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<String> userIds = new ArrayList<>();
                    List<User> userList = Collections.synchronizedList(new ArrayList<>());
                    AtomicInteger counter = new AtomicInteger(0); // Biến đếm

                    for (DataSnapshot sharedNoteSnapshot : snapshot.getChildren()) {
                        SharedNote sharedNote = sharedNoteSnapshot.getValue(SharedNote.class);
                        if (sharedNote != null) {
                            userIds.add(sharedNote.getSharedUser());
                            Log.d("DEBUG FIREBASE GETSHAREDNOTE", "sharedUser: " + sharedNote.getSharedUser());
                        }
                    }

                    if (userIds.isEmpty()) {
                        callback.onSharedNoteLoaded(userList);
                        return;
                    }

                    DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                    counter.set(userIds.size()); // Đặt số lượng user cần tải

                    for (String userId : userIds) {
                        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    User user = snapshot.getValue(User.class);
                                    if (user != null) {
                                        userList.add(user);
                                    }
                                    Log.d("DEBUG FIREBASE User", "User: " + user);
                                }
                                if (counter.decrementAndGet() == 0) { // Khi tất cả user đã tải xong
                                    callback.onSharedNoteLoaded(new ArrayList<>(userList));
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("Firebase", "Lỗi khi lấy User: " + error.getMessage());
                                if (counter.decrementAndGet() == 0) {
                                    callback.onSharedNoteLoaded(new ArrayList<>(userList));
                                }
                            }
                        });
                    }
                } else {
                    callback.onSharedNoteLoaded(new ArrayList<>());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Lỗi khi lấy Shared Notes: " + error.getMessage());
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
    public void checkIfEmailExists(String email, OnEmailCheckListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        listener.onResult(true, user);
                        return;
                    }
                }
                listener.onResult(false, null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onResult(false, null);
            }
        });
    }

}
