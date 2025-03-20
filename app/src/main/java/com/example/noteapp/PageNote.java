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
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.noteapp.adapter.UserAdapter;
import com.example.noteapp.interfacePackage.NoteUpdateListener;
import com.example.noteapp.interfacePackage.OnDataSyncListener;
import com.example.noteapp.model.NoteContent;
import com.example.noteapp.model.NoteModel;
import com.example.noteapp.model.User;
import com.example.noteapp.myDatabase.FirebaseSyncHelper;
import com.example.noteapp.settings.UserSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageNote extends AppCompatActivity implements View.OnClickListener {
    private TextView btnBack, btMore, btShare, btnAddTable;
    private ScrollView scrollViewContent;
    private LinearLayout lrContent;
    private EditText edTitle;
    private final List<User> sharedUserList = new ArrayList<>();
    private NoteModel note;
    private UserSession userSession;
    private String userId, owner, noteId;
    private boolean checkPermission;
    private FirebaseSyncHelper syncHelper;
    private UserAdapter adapter;
    private List<NoteContent> contentList;

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
            syncHelper.listenForNoteUpdation(noteId, new NoteUpdateListener() {
                @Override
                public void onTitleUpdated(String newTitle) {
                    runOnUiThread(() -> edTitle.setText(newTitle));
                }

                @Override
                public void onContentUpdated(List<Object> contentList) {
                    runOnUiThread(() -> updateUI(contentList));
                }
            });
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
                contentList = note.getContent();

                runOnUiThread(() -> {
                    edTitle.setText(note.getTitle());
                    lrContent.removeAllViews();

                    for (int i = 0; i < note.getContent().size(); i++) {
                        NoteContent item = note.getContent().get(i);
                        Log.d("DEBUG", "NoteContent: " + item);
                        String type = item.getType();
                        Log.d("DEBUG", "type: " + type);

                        if (type.equals("text")) {
                            EditText editText = createEditText(item.getTextContent(), i);
                            lrContent.addView(editText);

                            if (checkPermission) {
                                editText.setEnabled(true);
                            } else {
                                editText.setEnabled(false);
                                editText.setTextColor(Color.BLACK);
                            }
                        }
                        else if (type.equals("table")) {
                            List<List<String>> tableData = item.getTableContent(); // Lấy dữ liệu bảng từ NoteContent

                            if (tableData != null && !tableData.isEmpty()) {
                                Log.d("DEBUG", "tableData: " + tableData);

                                TableLayout table = createTable(tableData, i);
                                lrContent.addView(table);
                                table.setEnabled(checkPermission);
                                setTableEnabled(table, checkPermission);
                            } else {
                                Log.e("ERROR", "Table data is null or empty");
                            }
                        }
                    }
                });
            }


            @Override
            public void onSharedNoteLoaded(List<User> sharedUserList) {

            }


        });


        if (checkPermission) {
            edTitle.setEnabled(true);
        }
        else {
            edTitle.setEnabled(false);
            edTitle.setTextColor(Color.BLACK);
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

        btnBack.setOnClickListener(this);
        btMore.setOnClickListener(this);
        btShare.setOnClickListener(this);
        if (checkPermission){
            btnAddTable.setOnClickListener(this);
            lrContent.setOnClickListener(this);
        }
    }
    private void setTableEnabled(TableLayout table, boolean enabled) {
        for (int i = 0; i < table.getChildCount(); i++) {
            View child = table.getChildAt(i);
            if (child instanceof TableRow) {
                TableRow row = (TableRow) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View cell = row.getChildAt(j);
                    if (cell instanceof EditText) {
                        cell.setEnabled(enabled);
                        ((EditText) cell).setTextColor(Color.BLACK);
                    }
                }
            }
        }
    }

    public void initView(){
        btnBack = (TextView) findViewById(R.id.btnBack);
        btMore= (TextView) findViewById(R.id.btMore);
        edTitle =(EditText) findViewById(R.id.edTitle);
//        edContent=(EditText) findViewById(R.id.edContent);
        btShare=(TextView) findViewById(R.id.btShare);
        userSession = new UserSession();
        syncHelper = new FirebaseSyncHelper(this);
        btnAddTable = (TextView) findViewById(R.id.btnAddTable);
        lrContent = (LinearLayout) findViewById(R.id.lrContent);
        contentList = new ArrayList<>();
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
        }else if (view.getId() == R.id.btMore){
            showPopUpMenu(view);
        }else if (view.getId() == R.id.btShare){
            showPopup(view);
        }else if (view.getId() == R.id.btnAddTable){
            addTable();
        }else if (view.getId() == R.id.lrContent){
            if (lrContent.getChildCount() > 0){
                View lastChild = lrContent.getChildAt(lrContent.getChildCount() - 1);
                Log.d("DEBUG", "onClick: " + lastChild);
                lastChild.requestFocus();
                ((EditText) lastChild).setSelection(((EditText) lastChild).getText().length());
            }
        }
    }
    public void addTable() {
        // Tạo bảng trống
        List<List<String>> emptyTable = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                row.add(""); // Ô trống
            }
            emptyTable.add(row);
        }

        int newIndex = contentList.size(); // Dùng size của contentList

        // Cập nhật danh sách cục bộ
        contentList.add(new NoteContent(emptyTable));
        contentList.add(new NoteContent(""));

        // Hiển thị trên UI
        TableLayout newTable = createTable(emptyTable, newIndex);
        lrContent.addView(newTable);

        EditText newEditText = createEditText("", newIndex + 1);
        lrContent.addView(newEditText);

        // Cập nhật Firebase
        DatabaseReference noteRef = FirebaseDatabase.getInstance().getReference("notes").child(noteId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("content/" + newIndex + "/type", "table");
        updates.put("content/" + newIndex + "/tableContent", emptyTable); // Đúng key JSON

        updates.put("content/" + (newIndex + 1) + "/type", "text");
        updates.put("content/" + (newIndex + 1) + "/textContent", ""); // Đúng key JSON

        noteRef.updateChildren(updates);
    }
    public TableLayout createTable(List<List<String>> tableData, int position) {
        TableLayout tableLayout = new TableLayout(this);
        tableLayout.setBackgroundResource(R.drawable.cell_border);
        tableLayout.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
        ));

        for (int i = 0; i < tableData.size(); i++) {
            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
            ));

            List<String> rowData = tableData.get(i);

            for (int j = 0; j < rowData.size(); j++) {
                EditText cell = new EditText(this);
                cell.setBackground(null);
                cell.setText(rowData.get(j));

                cell.setLayoutParams(new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT,
                        1.0f
                ));

                // Lắng nghe thay đổi để cập nhật Firebase
                final int rowIndex = i;
                final int colIndex = j;
                cell.addTextChangedListener(new TextWatcher() {
                    private String previousText = cell.getText().toString(); // Lưu nội dung cũ để so sánh

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        String newText = s.toString();
                        if (!newText.equals(previousText)) { // Chỉ cập nhật nếu có thay đổi
                            previousText = newText;

                            // Cập nhật dữ liệu cục bộ
                            tableData.get(rowIndex).set(colIndex, newText);

                            // Cập nhật Firebase
                            DatabaseReference noteRef = FirebaseDatabase.getInstance().getReference("notes").child(noteId);
                            noteRef.child("content").child(String.valueOf(position))
                                    .child("tableContent").child(String.valueOf(rowIndex))
                                    .child(String.valueOf(colIndex)).setValue(newText);
                        }
                    }

                });

                row.addView(cell);

                // Nếu không phải cột cuối thì thêm đường kẻ dọc
                if (j < rowData.size() - 1) {
                    View divider = new View(this);
                    divider.setLayoutParams(new TableRow.LayoutParams(
                            3, // Độ rộng đường kẻ
                            TableRow.LayoutParams.MATCH_PARENT));
                    divider.setBackgroundColor(Color.BLACK);
                    row.addView(divider);
                }
            }
            tableLayout.addView(row);

            // Thêm đường kẻ ngang giữa các dòng
            if (i < tableData.size() - 1) {
                View horizontalDivider = new View(this);
                horizontalDivider.setLayoutParams(new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        3)); // Độ cao đường kẻ
                horizontalDivider.setBackgroundColor(Color.BLACK);
                tableLayout.addView(horizontalDivider);
            }
        }

        return tableLayout;
    }
    public EditText createEditText(String content, int position) {
        EditText editText = new EditText(this);
        editText.setBackground(null);
        editText.setText(content != null ? content : "");
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        editText.setTag(position);

        DatabaseReference noteRef = FirebaseDatabase.getInstance()
                .getReference("notes").child(noteId)
                .child("content").child(String.valueOf(position)).child("textContent");


        editText.addTextChangedListener(new TextWatcher() {
            private String previousText = content != null ? content : ""; // Lưu nội dung trước đó
            private boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return; // Ngăn vòng lặp vô hạn

                String newText = s.toString();
                if (newText.equals(previousText)) return; // Không cập nhật nếu không có thay đổi

                previousText = newText; // Cập nhật nội dung trước đó
                isUpdating = true;

                noteRef.setValue(newText).addOnCompleteListener(task -> isUpdating = false);
            }
        });

        return editText;
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

    private void updateUI(List<Object> contentList) {
        for (int position = 0; position < contentList.size(); position++) {
            Object content = contentList.get(position);

            if (content instanceof String) { // Nếu là văn bản
                if (position < lrContent.getChildCount()) {
                    View viewNow = lrContent.getChildAt(position);
                    if (viewNow instanceof EditText) {
                        ((EditText) viewNow).setText((String) content);
                    }
                    else {
                        lrContent.removeViewAt(position);
                        lrContent.addView(createEditText((String) content, position), position);
                    }
                }
                else {
                    lrContent.addView(createEditText((String) content, position));
                }
            }
            else if (content instanceof List) { // Nếu là bảng
                if (position < lrContent.getChildCount()) {
                    View viewNow = lrContent.getChildAt(position);
                    if (viewNow instanceof TableLayout) {
                        updateTable((TableLayout) viewNow, (List<List<String>>) content);
                    } else {
                        lrContent.removeViewAt(position);
                        lrContent.addView(createTable((List<List<String>>) content, position), position);
                    }
                } else {
                    lrContent.addView(createTable((List<List<String>>) content, position));
                }
            }
        }

        // Xóa phần dư nếu UI nhiều hơn contentList
        while (lrContent.getChildCount() > contentList.size()) {
            lrContent.removeViewAt(lrContent.getChildCount() - 1);
        }
    }
    private void updateTable(TableLayout tableLayout, List<List<String>> newData) {
        int rowIndex = 0;
        int viewIndex = 0;
        int totalRows = newData.size();

        while (rowIndex < totalRows || viewIndex < tableLayout.getChildCount()) {
            View child = tableLayout.getChildAt(viewIndex);

            if (child instanceof TableRow) {
                TableRow row = (TableRow) child;
                if (rowIndex < totalRows) {
                    updateTableRow(row, newData.get(rowIndex));
                    rowIndex++;
                    viewIndex++;

                    // Kiểm tra xem sau mỗi TableRow có divider không, nếu không thì thêm mới
                    if (rowIndex < totalRows) { // Chỉ thêm nếu không phải dòng cuối
                        if (viewIndex >= tableLayout.getChildCount() || !(tableLayout.getChildAt(viewIndex) instanceof View)) {
                            tableLayout.addView(createHorizontalDivider(), viewIndex);
                        }
                        viewIndex++; // Bỏ qua đường kẻ
                    }
                } else {
                    tableLayout.removeViewAt(viewIndex); // Xóa hàng dư thừa
                }
            } else if (child instanceof View) {
                if (rowIndex < totalRows - 1) { // Giữ lại đường kẻ ngang giữa các hàng
                    viewIndex++;
                } else {
                    tableLayout.removeViewAt(viewIndex); // Xóa đường kẻ dư thừa
                }
            }
        }

        // Thêm hàng mới nếu cần
        while (rowIndex < totalRows) {
            TableRow newRow = createTableRow(newData.get(rowIndex));
            tableLayout.addView(newRow);
            rowIndex++;

            // Thêm đường kẻ ngang sau mỗi hàng (trừ hàng cuối)
            if (rowIndex < totalRows) {
                View divider = createHorizontalDivider();
                tableLayout.addView(divider);
            }
        }
    }

    // Cập nhật hàng hiện có
    private void updateTableRow(TableRow row, List<String> rowData) {
        int cellCount = row.getChildCount();
        int newCellCount = rowData.size();
        int cellIndex = 0;

        for (int j = 0; j < newCellCount; j++) {
            if (cellIndex >= cellCount) {
                row.addView(createTableCell(rowData.get(j))); // Thêm ô mới nếu cần
            } else {
                View cellView = row.getChildAt(cellIndex);
                if (cellView instanceof EditText) {
                    EditText cell = (EditText) cellView;
                    if (!cell.getText().toString().equals(rowData.get(j))) {
                        cell.setText(rowData.get(j));
                    }
                }
                cellIndex++;
            }

            // Nếu không phải cột cuối, đảm bảo có đường kẻ dọc
            if (j < newCellCount - 1) {
                if (cellIndex >= cellCount) {
                    row.addView(createVerticalDivider()); // Thêm đường kẻ mới nếu thiếu
                } else {
                    cellIndex++; // Bỏ qua đường kẻ cũ
                }
            }
        }

        // Xóa ô dư thừa
        while (row.getChildCount() > cellIndex) {
            row.removeViewAt(row.getChildCount() - 1);
        }
    }

    // Tạo ô mới
    private EditText createTableCell(String text) {
        EditText cell = new EditText(this);
        cell.setBackground(null);
        cell.setText(text);
        cell.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT,
                1.0f
        ));
        return cell;
    }

    // Tạo hàng mới
    private TableRow createTableRow(List<String> rowData) {
        TableRow row = new TableRow(this);
        for (int j = 0; j < rowData.size(); j++) {
            row.addView(createTableCell(rowData.get(j)));
            if (j < rowData.size() - 1) {
                row.addView(createVerticalDivider());
            }
        }
        return row;
    }

    // Tạo đường kẻ dọc
    private View createVerticalDivider() {
        View divider = new View(this);
        divider.setLayoutParams(new TableRow.LayoutParams(3, TableRow.LayoutParams.MATCH_PARENT));
        divider.setBackgroundColor(Color.BLACK);
        return divider;
    }

    // Tạo đường kẻ ngang
    private View createHorizontalDivider() {
        View divider = new View(this);
        divider.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, 3));
        divider.setBackgroundColor(Color.BLACK);
        return divider;
    }
}
