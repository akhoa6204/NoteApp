package com.example.noteapp.myDatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.noteapp.model.NoteModel;
import com.example.noteapp.model.SharedNote;
import com.example.noteapp.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Database extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "noteApp.db";
    private static final int DATABASE_VERSION = 1;

    // Bảng User
    private static final String TABLE_USER = "User";
    private static final String KEY_USER_ID = "id";
    private static final String KEY_FIRST_NAME = "firstname";
    private static final String KEY_LAST_NAME = "lastname";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_IMG = "img";
    private static final String KEY_CREATED_AT = "created_at";

    // Bảng Note
    private static final String TABLE_NOTE = "Note";
    private static final String KEY_NOTE_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_TAG = "tag";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_CREATED_AT_NOTE = "created_at";
    private static final String KEY_UPDATED_AT = "updated_at";

    // Bảng SharedNote
    private static final String TABLE_SHARED_NOTE = "SharedNote";
    private static final String KEY_SHARED_ID = "id";
    private static final String KEY_NOTE_ID_FK = "noteId";
    private static final String KEY_PERMISSION = "permission";
    private static final String KEY_CREATED_AT_SHAREDNOTE= "created_at";
    private static final String KEY_SHARED_USER = "sharedUser";

    // Bảng SyncLastTime
    private static final String TABLE_SYNC_LAST_TIME = "SyncLastTime";
    private static final String KEY_TABLE_NAME = "table_name";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";


    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng User
        String createUserTable = "CREATE TABLE " + TABLE_USER + " ("
                + KEY_USER_ID + " TEXT PRIMARY KEY, "
                + KEY_FIRST_NAME + " TEXT NOT NULL, "
                + KEY_LAST_NAME + " TEXT NOT NULL, "
                + KEY_EMAIL + " TEXT UNIQUE NOT NULL, "
                + KEY_PASSWORD + " TEXT NOT NULL, "
                + KEY_IMG + " TEXT, "
                + KEY_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        // Tạo bảng Note
        String createNoteTable = "CREATE TABLE " + TABLE_NOTE + " ("
                + KEY_NOTE_ID + " TEXT PRIMARY KEY, "
                + KEY_TITLE + " TEXT NOT NULL, "
                + KEY_CONTENT + " TEXT, "
                + KEY_OWNER + " TEXT NOT NULL, "
                + KEY_TAG + " Text, "
                + KEY_CREATED_AT_NOTE + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + KEY_UPDATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY(" + KEY_OWNER + ") REFERENCES " + TABLE_USER + "(" + KEY_USER_ID + ") ON DELETE CASCADE)";

        // Tạo bảng SharedNote
        String createSharedNoteTable = "CREATE TABLE " + TABLE_SHARED_NOTE + " ("
                + KEY_SHARED_ID + " TEXT PRIMARY KEY, "
                + KEY_NOTE_ID_FK + " TEXT NOT NULL, "
                + KEY_SHARED_USER + " TEXT NOT NULL, "
                + KEY_PERMISSION + " INTEGER NOT NULL CHECK(" + KEY_PERMISSION + " IN (0,1)), "
                + KEY_CREATED_AT_SHAREDNOTE + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY(" + KEY_NOTE_ID_FK + ") REFERENCES " + TABLE_NOTE + "(" + KEY_NOTE_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY(" + KEY_SHARED_USER + ") REFERENCES " + TABLE_USER + "(" + KEY_USER_ID + ") ON DELETE CASCADE,"
                + "UNIQUE (" + KEY_NOTE_ID_FK + "," + KEY_SHARED_USER +"))" ;

        // Tạo bảng SyncLastTime
        String createSyncLastTimeTable = "CREATE TABLE " + TABLE_SYNC_LAST_TIME + " ("
                + KEY_TABLE_NAME + " TEXT PRIMARY KEY, "
                + KEY_LAST_SYNC_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        db.execSQL(createUserTable);
        db.execSQL(createNoteTable);
        db.execSQL(createSharedNoteTable);
        db.execSQL(createSyncLastTimeTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHARED_NOTE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }
    public boolean isEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_USER + " WHERE " + KEY_EMAIL + " = ?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
    public List<NoteModel> selectMyNote(String userId) {
        List<NoteModel> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_NOTE +
                " WHERE " + KEY_OWNER + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT));
            String tag = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TAG));
            String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_AT_NOTE));
            String owner = cursor.getString(cursor.getColumnIndexOrThrow(KEY_OWNER));
            String updated_at = cursor.getString(cursor.getColumnIndexOrThrow(KEY_UPDATED_AT));

            notes.add(new NoteModel(id, title, content, tag, owner, createdAt, updated_at));
        }
        cursor.close();
        return notes;
    }
    public NoteModel selectNote(String noteId) {
        SQLiteDatabase db = this.getReadableDatabase();
        NoteModel note = null;


        String query = "SELECT * FROM " + TABLE_NOTE + " WHERE " + KEY_NOTE_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{noteId});

        if (cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT));
            String tag = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TAG));
            String created_at = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_AT));
            String updated_at = cursor.getString(cursor.getColumnIndexOrThrow(KEY_UPDATED_AT));
            String owner = cursor.getString(cursor.getColumnIndexOrThrow(KEY_OWNER));

            note = new NoteModel(id, title, content, tag, owner, created_at, updated_at);
        }

        cursor.close();
        db.close();

        return note;
    }
    public String getUserIdByEmail(String email){
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT " + KEY_USER_ID + " FROM " + TABLE_USER + " WHERE " + KEY_EMAIL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(email)});
        String userId = null;
        if(cursor.moveToFirst()){
            userId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ID));
        }
        cursor.close();
        return userId;
    }
    public User getUserByUserId(String userId) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USER + " WHERE " + KEY_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        User user = null;
        if (cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ID));
            String firstName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FIRST_NAME));
            String lastName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LAST_NAME));
            String password = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL));

            user = new User(id, email, firstName, lastName, password);
        }
        cursor.close();
        return user;
    }
    public boolean updateSharedUser(NoteModel note, String email, String id){
        SQLiteDatabase db = getReadableDatabase();
        ContentValues values = new ContentValues();

        if(isValidUser(note, email)){
            return false;
        }
        values.put(KEY_SHARED_ID, id);
        values.put(KEY_NOTE_ID_FK, note.getId());
        values.put(KEY_SHARED_USER, getUserIdByEmail(email));
        values.put(KEY_PERMISSION, 0);
        db.insert(TABLE_SHARED_NOTE, null, values);
        return true;
    }
    public boolean isValidUser(NoteModel note, String email) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT 1 FROM " + TABLE_SHARED_NOTE + " WHERE "
                + KEY_SHARED_USER + "=? AND " + KEY_NOTE_ID_FK + "=?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(getUserIdByEmail(email)), String.valueOf(note.getId())});

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }
    public boolean checkPermissionNote(String noteId, String userId){
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT 1 FROM " + TABLE_NOTE + " WHERE " +KEY_NOTE_ID + "=? AND " + KEY_OWNER + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(noteId), String.valueOf(userId)});
        boolean permission = cursor.moveToFirst();
        cursor.close();
        db.close();
        return permission;
    }
    public void editPermission(String noteId, String userId, int permission){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PERMISSION, permission);

        db.update(TABLE_SHARED_NOTE, values,
                KEY_NOTE_ID + "=? AND " + KEY_SHARED_USER + " =? ",
                new String[]{noteId, userId});

    }
    public List<User> getAllUser(){
        List<User> listUser = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USER;
        Cursor cursor = db.rawQuery(query, new String[]{});
        while (cursor.moveToNext()){
            String id = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ID));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL));
            String firstName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FIRST_NAME));
            String lastName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LAST_NAME));
            String password = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD));
            listUser.add(new User(id, email, firstName, lastName, password));
        }
        cursor.close();
        db.close();

        return listUser;
    }
    public List<NoteModel> getAllNotes(){
        List<NoteModel> listNote = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NOTE;
        Cursor cursor = db.rawQuery(query, new String[]{});
        while (cursor.moveToNext()){
            String id = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT));
            String tag  = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TAG));
            String owner = cursor.getString(cursor.getColumnIndexOrThrow(KEY_OWNER));
            String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_AT_NOTE));
            String updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_UPDATED_AT));

            listNote.add(new NoteModel(id, title, content, tag, owner, createdAt, updatedAt));
        }
        cursor.close();
        db.close();

        return listNote;
    }
    public List<SharedNote> getSharedNote(String noteId){
        List<SharedNote> sharedNotes= new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " +
                TABLE_SHARED_NOTE + " " +
                "WHERE " + KEY_NOTE_ID_FK + "=?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(noteId)});
        while (cursor.moveToNext()){
            String shareId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHARED_ID));
            int permission = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PERMISSION));
            String sharedUser = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHARED_USER));

            sharedNotes.add(new SharedNote(shareId, noteId, permission, sharedUser));
        }
        cursor.close();
        db.close();

        return sharedNotes;
    }
    public List<NoteModel> getSharedNoteByUserId(String userId) {
        List<NoteModel> sharedNotes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT Note." + KEY_NOTE_ID + ", " +
                "Note." + KEY_TITLE + ", " +
                "Note." + KEY_CONTENT + ", " +
                "Note." + KEY_TAG + ", " +
                "Note." + KEY_OWNER + ", " +
                "Note." + KEY_CREATED_AT_NOTE + ", " +
                "Note." + KEY_UPDATED_AT +
                " FROM " + TABLE_NOTE +
                " JOIN " + TABLE_SHARED_NOTE +
                " ON Note." + KEY_NOTE_ID + " = SharedNote." + KEY_NOTE_ID_FK +
                " WHERE SharedNote." + KEY_SHARED_USER + " =?";


        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        while (cursor.moveToNext()) {
            String noteId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT));
            String tag = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TAG));
            String owner = cursor.getString(cursor.getColumnIndexOrThrow(KEY_OWNER));
            String created_at = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_AT_NOTE));
            String updated_at = cursor.getString(cursor.getColumnIndexOrThrow(KEY_UPDATED_AT));

            sharedNotes.add(new NoteModel(noteId, title, content, tag, owner, created_at, updated_at));
        }
        cursor.close();
        db.close();

        return sharedNotes;
    }
    public boolean checkOwnerNote(String userId, String noteId){
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NOTE +
                " WHERE " + KEY_NOTE_ID + "=? AND " + KEY_OWNER + "=?";
        Cursor cursor=db.rawQuery(query, new String[]{noteId, userId});
        boolean checkOwner = cursor.moveToFirst();
        cursor.close();
        db.close();
        return checkOwner;
    }
    public List<SharedNote> getAllSharedNotes(){
        SQLiteDatabase db = getReadableDatabase();
        List<SharedNote> sharedNotes = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_SHARED_NOTE;
        Cursor cursor = db.rawQuery(query, new String[]{});
        while (cursor.moveToNext()){
            String shareId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHARED_ID));
            String noteId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_ID_FK));
            int permission = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PERMISSION));
            String sharedUser = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHARED_USER));

            sharedNotes.add(new SharedNote(shareId, noteId, permission, sharedUser));
        }
        cursor.close();
        db.close();
        return sharedNotes;
    }
    public NoteModel getNoteByNoteId(String noteId){
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NOTE + " WHERE " + KEY_NOTE_ID + " =?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(noteId)});
        NoteModel note= null;
        if(cursor.moveToFirst()){
            String title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT));
            String tag = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TAG));
            String owner = cursor.getString(cursor.getColumnIndexOrThrow(KEY_OWNER));
            String created_at = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_AT_NOTE));
            String updated_at = cursor.getString(cursor.getColumnIndexOrThrow(KEY_UPDATED_AT));
            note = new NoteModel(noteId, title, content, tag, owner, created_at, updated_at);
        }
        cursor.close();
        db.close();
        return note;
    }
    public int checkPermissionSharedUser(String userId, String noteId){
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SHARED_NOTE +
                " WHERE " + KEY_NOTE_ID_FK + " =? AND " +
                KEY_SHARED_USER + " =?";
        int permission = -1;
        Cursor cursor = db.rawQuery(query, new String[]{noteId, userId});
        if(cursor.moveToFirst()){
            permission = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PERMISSION));
        }
        cursor.close();
        db.close();

        return permission;
    }

    // Chèn hoặc cập nhật danh sách người dùng (User)
    public void insertOrUpdateUsers(List<User> users) {
        if (users == null || users.isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (User user : users) {
                ContentValues values = new ContentValues();
                values.put(KEY_USER_ID, user.getId());
                values.put(KEY_FIRST_NAME, user.getFirstName());
                values.put(KEY_LAST_NAME, user.getLastName());
                values.put(KEY_EMAIL, user.getEmail());
                values.put(KEY_PASSWORD, user.getPassword());

                db.insertWithOnConflict(TABLE_USER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Chèn hoặc cập nhật danh sách ghi chú (NoteModel)
    public void insertOrUpdateNotes(List<NoteModel> notes) {
        if (notes == null || notes.isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (NoteModel note : notes) {
                ContentValues values = new ContentValues();
                values.put(KEY_NOTE_ID, note.getId());
                values.put(KEY_TITLE, note.getTitle());
                values.put(KEY_CONTENT, note.getContent());
                values.put(KEY_TAG, note.getTag());
                values.put(KEY_OWNER, note.getOwner());
                values.put(KEY_CREATED_AT_NOTE, note.getCreatedAt());
                values.put(KEY_UPDATED_AT, note.getUpdatedAt());

                db.insertWithOnConflict(TABLE_NOTE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Chèn hoặc cập nhật danh sách ghi chú được chia sẻ (SharedNote)
    public void insertOrUpdateSharedNotes(List<SharedNote> sharedNotes) {
        if (sharedNotes == null || sharedNotes.isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (SharedNote sharedNote : sharedNotes) {
                ContentValues values = new ContentValues();
                values.put(KEY_SHARED_ID, sharedNote.getShareId());
                values.put(KEY_NOTE_ID_FK, sharedNote.getNoteId());
                values.put(KEY_SHARED_USER, sharedNote.getSharedUser());
                values.put(KEY_PERMISSION, sharedNote.getPermission());

                db.insertWithOnConflict(TABLE_SHARED_NOTE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteUserById(String key) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_USER, "WHERE " + KEY_USER_ID + "=?", new String[]{key});
    }

    public void deleteNoteById(String key) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NOTE, "WHERE " + KEY_NOTE_ID + "=?", new String[]{key});
    }

    public void deleteSharedNoteById(String key) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SHARED_NOTE, "WHERE " + KEY_SHARED_ID + "=?", new String[]{key});
    }

    public int deleteSharedNote(String noteId, String userId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_SHARED_NOTE,
                KEY_SHARED_USER + " =? AND " + KEY_NOTE_ID_FK + " =?",
                new String[]{userId, noteId});
    }
    public void updateNote(String noteId, String title, String content){
        if (noteId == null) return;
        if (title == null && content == null) return;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        if (title != null){
            values.put(KEY_TITLE, title);
        }
        if (content != null){
            values.put(KEY_CONTENT, content);
        }

        db.update(TABLE_NOTE, values, KEY_NOTE_ID + " =?", new String[]{noteId});
        db.close();
    }
    public int deleteMyNote(String noteId){
        if (noteId == null) return 0;
        SQLiteDatabase db = getWritableDatabase();
        int check = db.delete(TABLE_NOTE, KEY_NOTE_ID + " =?", new String[]{noteId});
        db.close();
        return check;
    }
    public void insertNewNote(NoteModel note){
        SQLiteDatabase db= getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_NOTE_ID, note.getId());
        values.put(KEY_TITLE, note.getTitle());
        values.put(KEY_CONTENT, note.getContent());
        values.put(KEY_TAG, note.getTag());
        values.put(KEY_OWNER, note.getOwner());
        values.put(KEY_CREATED_AT_NOTE, note.getCreatedAt());
        values.put(KEY_UPDATED_AT, note.getUpdatedAt());

        db.insert(TABLE_NOTE, null, values);
        db.close();
    }
    // Hàm lấy thời gian đồng bộ cuối cùng của một bảng
    public String getLastSyncTime(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String lastSyncTime = null;

        String query = "SELECT * FROM " + TABLE_SYNC_LAST_TIME + " WHERE " + KEY_TABLE_NAME + "=?";

        Cursor cursor = db.rawQuery(query, new String[]{tableName});

        if (cursor.moveToFirst()) {
            lastSyncTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LAST_SYNC_TIME));
        }
        cursor.close();
        db.close();
        return lastSyncTime;
    }

    public void updateLastSyncTime(String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();
        String currentTime = String.valueOf(System.currentTimeMillis()); // Lấy timestamp hiện tại

        ContentValues values = new ContentValues();
        values.put(KEY_LAST_SYNC_TIME, currentTime);

        // Kiểm tra xem đã có bản ghi nào chưa
        int rowsAffected = db.update(TABLE_SYNC_LAST_TIME, values, KEY_TABLE_NAME + " =?", new String[]{tableName});

        // Nếu chưa có dữ liệu, thêm mới vào bảng
        if (rowsAffected == 0) {
            values.put(KEY_TABLE_NAME , tableName);
            db.insert(TABLE_SYNC_LAST_TIME, null, values);
        }

        db.close();
    }


}

