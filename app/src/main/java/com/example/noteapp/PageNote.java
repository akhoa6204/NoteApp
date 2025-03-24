package com.example.noteapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
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
import androidx.core.content.res.ResourcesCompat;
import androidx.core.text.HtmlCompat;

import com.example.noteapp.adapter.UserAdapter;
import com.example.noteapp.custom.CustomEditText;
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
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageNote extends AppCompatActivity implements View.OnClickListener {
    private TextView btnBack, btMore, btShare, btnAddTable, btMatchCase;
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
    private boolean isBold, isItalic, isUnderline,  isUnderlineCenter = false;
    private Runnable updateUIStyle;
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
                    runOnUiThread(() -> {
                        edTitle.setText(translateSpanned(newTitle));
                    });
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
                    // Hiển thị trong EditText
                    edTitle.setText(translateSpanned(note.getTitle()));

                    lrContent.removeAllViews();

                    for (int i = 0; i < note.getContent().size(); i++) {
                        NoteContent item = note.getContent().get(i);
                        Log.d("DEBUG", "NoteContent: " + item);
                        String type = item.getType();
                        Log.d("DEBUG", "type: " + type);

                        if (type.equals("text")) {
                            CustomEditText editText = createEditText(item.getTextContent(), i);
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

                                LinearLayout table = createTableWithControls(tableData, i);
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
                String newTitle = Html.toHtml(s, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);

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
        btMatchCase.setOnClickListener(this);

    }
    public Spanned translateSpanned(String text) {
        if (text == null) return new SpannableString("");

        Spanned spanned = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT);
        String trimmedText = spanned.toString().replaceAll("\n$", "");

        SpannableString spannable = new SpannableString(trimmedText);
        TextUtils.copySpansFrom(spanned, 0, trimmedText.length(), null, spannable, 0);

        return spannable;
    }
    public String translateHtml(Spanned text){
        return Html.toHtml(text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
    }
    private void setTableEnabled(LinearLayout lrLayout, boolean enabled) {
        for (int i = 0; i < lrLayout.getChildCount(); i++) {
            View tableView = lrLayout.getChildAt(i);
            if (tableView instanceof TableLayout) {
                TableLayout table = (TableLayout) tableView;
                for (int rowIndex = 0; rowIndex < table.getChildCount(); rowIndex++) {
                    View rowView = table.getChildAt(rowIndex);
                    if (rowView instanceof TableRow) {
                        TableRow row = (TableRow) rowView;
                        for (int colIndex = 0; colIndex < row.getChildCount(); colIndex++) {
                            View cell = row.getChildAt(colIndex);
                            if (cell instanceof CustomEditText) {
                                cell.setEnabled(enabled);
                                ((CustomEditText) cell).setTextColor(Color.BLACK);
                            }
                        }
                    }
                }
                break; // Đã tìm thấy bảng thì thoát vòng lặp
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
        btMatchCase = (TextView) findViewById(R.id.btMatchCase);
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
        }
        else if (view.getId() == R.id.btMore){
            showPopUpMenu(view);
        }
        else if (view.getId() == R.id.btShare){
            showPopup(view);
        }
        else if (view.getId() == R.id.btnAddTable){
            addTable();
        }
        else if (view.getId() == R.id.lrContent){
            if (lrContent.getChildCount() > 0){
                View lastChild = lrContent.getChildAt(lrContent.getChildCount() - 1);
                Log.d("DEBUG", "onClick: " + lastChild);
                lastChild.requestFocus();
                ((CustomEditText) lastChild).setSelection(((CustomEditText) lastChild).getText().length());
            }
        }
        else if (view.getId() == R.id.btMatchCase){
            showPopUpTextStyle(view);
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
        LinearLayout newTable = createTableWithControls(emptyTable, newIndex);
        lrContent.addView(newTable);

        CustomEditText newEditText = createEditText("", newIndex + 1);
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
            TableRow row = createTableRow(tableData.get(i), position, i);
            tableLayout.addView(row);

            // Thêm đường kẻ ngang giữa các dòng (trừ dòng cuối)
            if (i < tableData.size() - 1) {
                tableLayout.addView(createHorizontalDivider());
            }
        }

        return tableLayout;
    }
    public CustomEditText createEditText(String content, int position) {
        CustomEditText editText = new CustomEditText(this, null);
        editText.setBackground(null);

        editText.setText(content != null ? translateSpanned(content) : "");
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        editText.setTag(position);

        DatabaseReference noteRef = FirebaseDatabase.getInstance()
                .getReference("notes").child(noteId)
                .child("content").child(String.valueOf(position)).child("textContent");

        editText.addTextChangedListener(new TextWatcher() {
            private String previousText = content != null ? content : "";
            private boolean isUpdating = false;
            private int selectionStart;
            private int selectionEnd;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                selectionStart = start;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return; // Ngăn vòng lặp vô hạn

                String rawText = s.toString();
                if (rawText.equals(previousText)) return;

                String newText = translateHtml(s);
                isUpdating = true;

                int textLength = s.length();
                int changeLength = rawText.length() - previousText.length();
                selectionEnd = Math.min(selectionStart + changeLength, textLength); // 🔥 Tránh lỗi IndexOutOfBoundsException

                if (selectionStart >= 0 && selectionStart <= selectionEnd) {
                    applyTextStyles(s, selectionStart, selectionEnd);
                }

                previousText = rawText; // Lưu lại nội dung gốc để so sánh lần sau

                noteRef.setValue(newText).addOnCompleteListener(task -> {
                    isUpdating = false;
                });
            }
        });


        editText.setOnSelectionChangeListener(new CustomEditText.OnSelectionChangeListener() {
            @Override
            public void onSelectionChanged(int start, int end) {
                updateTextStyleState(editText, start, end);
            }
        });

        return editText;
    }
    private void updateUI(List<Object> contentList) {
        for (int position = 0; position < contentList.size(); position++) {
            Object content = contentList.get(position);

            if (content instanceof String) { // Nếu là văn bản
                if (position < lrContent.getChildCount()) {
                    View viewNow = lrContent.getChildAt(position);
                    if (viewNow instanceof CustomEditText) {
                        ((CustomEditText) viewNow).setText(translateSpanned((String) content));
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
                        updateTable((TableLayout) viewNow, (List<List<String>>) content, position);
                    } else {
                        lrContent.removeViewAt(position);
                        lrContent.addView(createTableWithControls((List<List<String>>) content, position), position);
                    }
                } else {
                    lrContent.addView(createTableWithControls((List<List<String>>) content, position));
                }
            }
        }

        // Xóa phần dư nếu UI nhiều hơn contentList
        while (lrContent.getChildCount() > contentList.size()) {
            lrContent.removeViewAt(lrContent.getChildCount() - 1);
        }
    }
    private void updateTable(TableLayout tableLayout, List<List<String>> newData, int tablePosition) {
        int rowIndex = 0;
        int viewIndex = 0;
        int totalRows = newData.size();

        while (rowIndex < totalRows || viewIndex < tableLayout.getChildCount()) {
            View child = tableLayout.getChildAt(viewIndex);

            if (child instanceof TableRow) {
                TableRow row = (TableRow) child;
                if (rowIndex < totalRows) {
                    updateTableRow(row, newData.get(rowIndex), tablePosition, rowIndex);
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
            TableRow newRow = createTableRow(newData.get(rowIndex), tablePosition, rowIndex);
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
    private void updateTableRow(TableRow row, List<String> rowData, int tablePosition, int rowIndex) {
        int cellCount = row.getChildCount();
        int newCellCount = rowData.size();
        int cellIndex = 0;

        for (int j = 0; j < newCellCount; j++) {
            Spanned data = translateSpanned(rowData.get(j));
            if (cellIndex >= cellCount) {
                row.addView(createTableCell(String.valueOf(data), tablePosition, rowIndex, j));
            } else {
                View cellView = row.getChildAt(cellIndex);
                if (cellView instanceof CustomEditText) {
                    CustomEditText cell = (CustomEditText) cellView;
                    if (!cell.getText().toString().equals(data)) {
                        cell.setText(data);
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
    // Tạo ô mới và thêm TextWatcher để cập nhật Firebase
    private CustomEditText createTableCell(String text, int tablePosition, int rowIndex, int colIndex) {
        CustomEditText cell = new CustomEditText(this, null);
        cell.setBackground(null);
        cell.setText(translateSpanned(text));
        cell.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        DatabaseReference cellRef = FirebaseDatabase.getInstance().getReference("notes")
                .child(noteId).child("content").child(String.valueOf(tablePosition))
                .child("tableContent").child(String.valueOf(rowIndex))
                .child(String.valueOf(colIndex));

        cell.addTextChangedListener(new TextWatcher() {
            private String previousText = text;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String newText = s.toString();
                if (!newText.equals(previousText)) {
                    previousText = newText;
                    cellRef.setValue(translateHtml(s));
                }
            }
        });

        return cell;
    }
    // Tạo hàng mới với TextWatcher để cập nhật Firebase
    private TableRow createTableRow(List<String> rowData, int tablePosition, int rowIndex) {
        TableRow row = new TableRow(this);

        for (int j = 0; j < rowData.size(); j++) {
            CustomEditText cell = createTableCell(rowData.get(j), tablePosition, rowIndex, j);
            row.addView(cell);

            // Nếu không phải cột cuối thì thêm đường kẻ dọc
            if (j < rowData.size() - 1) {
                row.addView(createVerticalDivider());
            }
        }

        return row;
    }    // Tạo đường kẻ dọc
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
    // Tạo nút "btMore" cho hàng hoặc cột
    private TextView createBtMoreButton(boolean isColumn) {
        TextView button = new TextView(this);

        button.setText(isColumn ? "\ue5d3" : "\ue5d4"); // Mã Unicode icon
        button.setTextSize(20);
        button.setTypeface(ResourcesCompat.getFont(this, R.font.material_icons));
        button.setTextColor(Color.parseColor("#A4A4A4"));
        button.setOnClickListener(v -> {
        });

        return button;
    }
    // Hàm thêm hàng
    private void addRow(int tablePosition, int rowIndex) {
        // Xử lý thêm hàng
    }
    // Hàm thêm cột
    private void addColumn(int tablePosition, int colIndex) {
        // Xử lý thêm cột
    }
    public LinearLayout createTableWithControls(List<List<String>> tableData, int position) {
        // Layout chính bọc ngoài bảng
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout rowUpdateColumn = new LinearLayout(this);
        rowUpdateColumn.setOrientation(LinearLayout.HORIZONTAL);
        rowUpdateColumn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        );
        // Tạo khoảng trống bên trái bằng một View ảo
        View spaceView = new View(this);
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
        );
        spaceView.setLayoutParams(spaceParams);

        // Tạo TextView
        TextView buttonColumn = createBtMoreButton(true);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonColumn.setLayoutParams(textParams);

        // Thêm vào rowUpdateColumn
        rowUpdateColumn.addView(spaceView); // Thêm View ảo để đẩy TextView sang phải
        rowUpdateColumn.addView(buttonColumn);
        // Tạo bảng
        TableLayout tableLayout = createTable(tableData, position);

        mainLayout.addView(rowUpdateColumn);
        mainLayout.addView(tableLayout);
        return mainLayout;
    }
    public void showPopUpTextStyle(View anchorView) {
        View popupView = getLayoutInflater().inflate(R.layout.custom_style_text, null);
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
        );

        TextView btnDelete = popupView.findViewById(R.id.btnDelete);
        TextView btnBold = popupView.findViewById(R.id.btnBold);
        TextView btnItalic = popupView.findViewById(R.id.btnItalic);
        TextView btnUnderline = popupView.findViewById(R.id.btnUnderline);
        TextView btnUnderlineCenter = popupView.findViewById(R.id.btnUnderlineCenter);

        btnDelete.setOnClickListener(v -> popupWindow.dismiss());

        updateUIStyle = () -> {
            btnBold.setBackgroundColor(isBold ? Color.parseColor("#FFEB3B") : Color.TRANSPARENT);
            btnBold.setTextColor(isBold ? Color.WHITE : Color.BLACK);

            btnItalic.setBackgroundColor(isItalic ? Color.parseColor("#FFEB3B") : Color.TRANSPARENT);
            btnItalic.setTextColor(isItalic ? Color.WHITE : Color.BLACK);

            btnUnderline.setBackgroundColor(isUnderline ? Color.parseColor("#FFEB3B") : Color.TRANSPARENT);
            btnUnderline.setTextColor(isUnderline ? Color.WHITE : Color.BLACK);

            btnUnderlineCenter.setBackgroundColor(isUnderlineCenter ? Color.parseColor("#FFEB3B") : Color.TRANSPARENT);
            btnUnderlineCenter.setTextColor(isUnderlineCenter ? Color.WHITE : Color.BLACK);
        };

        btnBold.setOnClickListener(v -> {
            View view = getCurrentFocus();
            if (view instanceof CustomEditText) {
                applyStyle((CustomEditText) view, Typeface.BOLD);
            } else {
                isBold = !isBold; // Đảo trạng thái nếu không có focus
            }
            updateUIStyle.run(); // Cập nhật giao diện
        });

        btnItalic.setOnClickListener(v -> {
            View view = getCurrentFocus();
            if (view instanceof CustomEditText) {
                applyStyle((CustomEditText) view, Typeface.ITALIC);
            } else {
                isItalic = !isItalic;
            }
            updateUIStyle.run();
        });

        btnUnderline.setOnClickListener(v -> {
            View view = getCurrentFocus();
            if (view instanceof CustomEditText) {
                applyUnderline((CustomEditText) view);
            } else {
                isUnderline = !isUnderline;
            }
            updateUIStyle.run();
        });

        btnUnderlineCenter.setOnClickListener(v -> {
            View view = getCurrentFocus();
            if (view instanceof CustomEditText) {
                applyStrikethrough((CustomEditText) view);
            } else {
                isUnderlineCenter = !isUnderlineCenter;
            }
            updateUIStyle.run();
        });


        popupWindow.showAtLocation(anchorView, Gravity.BOTTOM, 0, 0);
    }
    private void applyStyle(CustomEditText editText, int style) {
        if (editText == null) return;

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        Spannable spannable = new SpannableStringBuilder(editText.getText());

        if (start != end) { // Có vùng bôi đen
            StyleSpan[] spans = spannable.getSpans(start, end, StyleSpan.class);
            boolean hasStyle = false;

            for (StyleSpan span : spans) {
                if (span.getStyle() == style) {
                    spannable.removeSpan(span);
                    hasStyle = true;
                }
            }

            if (!hasStyle) {
                spannable.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            editText.setText(spannable);
            editText.setSelection(start, end);
        } else { // Không có vùng bôi đen
            if (style == Typeface.BOLD) isBold = !isBold;
            if (style == Typeface.ITALIC) isItalic = !isItalic;
        }

        updateUIStyle.run(); // Cập nhật UI
    }
    private void applyUnderline(CustomEditText editText) {
        if (editText == null) return;

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        Spannable spannable = new SpannableStringBuilder(editText.getText());

        if (start != end) { // Có vùng bôi đen
            UnderlineSpan[] spans = spannable.getSpans(start, end, UnderlineSpan.class);
            boolean hasUnderline = spans.length > 0;

            if (hasUnderline) {
                for (UnderlineSpan span : spans) {
                    spannable.removeSpan(span);
                }
            } else {
                spannable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            editText.setText(spannable);
            editText.setSelection(start, end);
        } else { // Không có vùng bôi đen
            isUnderline = !isUnderline;
        }

        updateUIStyle.run();
    }
    private void applyStrikethrough(CustomEditText editText) {
        if (editText == null) return;

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        Spannable spannable = new SpannableStringBuilder(editText.getText());

        if (start != end) { // Có vùng bôi đen
            StrikethroughSpan[] spans = spannable.getSpans(start, end, StrikethroughSpan.class);
            boolean hasStrikethrough = spans.length > 0;

            if (hasStrikethrough) {
                for (StrikethroughSpan span : spans) {
                    spannable.removeSpan(span);
                }
            } else {
                spannable.setSpan(new StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            editText.setText(spannable);
            editText.setSelection(start, end);
        } else { // Không có vùng bôi đen
            isUnderlineCenter = !isUnderlineCenter;
        }

        updateUIStyle.run();
    }
    private void updateTextStyleState(CustomEditText editText, int start, int end) {
        if (editText == null) return;

        Spannable spannable = editText.getText();

        if (start != end) { // Nếu có vùng bôi đen
            isBold = false;
            isItalic = false;
            isUnderline = false;
            isUnderlineCenter = false;

            for (StyleSpan span : spannable.getSpans(start, end, StyleSpan.class)) {
                if (span.getStyle() == Typeface.BOLD) isBold = true;
                if (span.getStyle() == Typeface.ITALIC) isItalic = true;
            }

            isUnderline = spannable.getSpans(start, end, UnderlineSpan.class).length > 0;
            isUnderlineCenter = spannable.getSpans(start, end, StrikethroughSpan.class).length > 0;
        }

        // Kiểm tra nếu updateUIStyle != null trước khi chạy
        if (updateUIStyle != null) {
            updateUIStyle.run();
        }
    }
    private void applyTextStyles(Editable s, int start, int end) {
        if (!isBold) removeSpan(s, StyleSpan.class, start, end, Typeface.BOLD);
        if (!isItalic) removeSpan(s, StyleSpan.class, start, end, Typeface.ITALIC);
        if (!isUnderline) removeSpan(s, UnderlineSpan.class, start, end, -1);
        if (!isUnderlineCenter) removeSpan(s, StrikethroughSpan.class, start, end, -1);

        // Nếu bật thì áp dụng lại
        if (isBold) {
            s.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (isItalic) {
            s.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (isUnderline) {
            s.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (isUnderlineCenter) {
            s.setSpan(new StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    public <T> void removeSpan(Editable s, Class<T> type, int start, int end, int style) {
        T[] spans = s.getSpans(start, end, type);
        for (T span : spans) {
            int spanStart = s.getSpanStart(span);
            int spanEnd = s.getSpanEnd(span);
            Log.d("DEBUG", "Found span from " + spanStart + " to " + spanEnd);

            if (spanStart >= start && spanEnd <= end) {
                Log.d("DEBUG", "Removing span: " + span);
                s.removeSpan(span);
            }
        }
    }

}
