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
    private TextView btnBack, btMore, btShare, btnAddTable, btMatchCase, btnList;
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
    private boolean isEditing = false;

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
        btnList.setOnClickListener(this);
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
                Log.d("DEBUG_PageNote", "sharedUserListReturn: " + sharedUserListReturn);
                sharedUserList.clear();
                sharedUserList.addAll(sharedUserListReturn);

                if (adapter == null) {
                    adapter = new UserAdapter(getBaseContext(), sharedUserList, checkPermission, noteId);
                } else {
                    runOnUiThread(() ->{
                        Log.d("DEBUG_PageNote", "Cập nhật Adapter với danh sách: " + sharedUserList);
                        adapter.setListSharedUser(sharedUserList);
                    });
                }

                syncHelper.listenForSharedUserNote(noteId, tempUserList -> {
                    runOnUiThread(() -> {
                        sharedUserList.clear();
                        sharedUserList.addAll(tempUserList);
                        Log.d("DEBUG", "sharedUserListUpdate: " + sharedUserList);
                        if (adapter != null) {
                            adapter.setListSharedUser(sharedUserList);
                        } else {
                            adapter = new UserAdapter(getBaseContext(), sharedUserList, checkPermission, noteId);
                        }
                    });
                });
            }
        });
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
        btnList = (TextView) findViewById(R.id.btnList);
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
        }else if (view.getId() == R.id.btnList) {
            View currentView = getCurrentFocus();
            if (currentView instanceof EditText) {
                EditText editText = (EditText) currentView;
                String text = editText.getText().toString();

                if (isEditing) {
                    if (text.endsWith("\n• ") || text.endsWith("• ")) {
                        text = text.substring(0, text.length() - 2); // Xóa "• " cuối cùng
                    } else {
                        text += "\n";
                    }
                    isEditing = false;
                }
                else {
                    if (!text.isEmpty() && !text.endsWith("\n")){
                        text += "\n• ";
                    }else{
                        text += "• ";
                    }
                    isEditing = true;
                }

                editText.setText(text);
                editText.setSelection(editText.getText().length()); // Giữ vị trí con trỏ
            }
        }

    }
    // Hàm đếm số lần xuất hiện của "• "
    private int countOccurrences(String text, String target) {
        int count = 0, index = 0;
        while ((index = text.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }
        return count;
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

        String title = (edTitle.getText() != null && !edTitle.getText().toString().trim().isEmpty())
                ? edTitle.getText().toString()
                : "Tiêu đề ghi chú";

        tvTitle.setText("Chia sẻ \"" + title + "\"");

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

        if ( lvMember.getAdapter() == null){
            lvMember.setAdapter(adapter);
        }

        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);

        btnAdd.setOnClickListener(v -> {
            String email = edAddMember.getText().toString().trim();
            if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                Toast.makeText(getBaseContext(), "Email is not valid", Toast.LENGTH_SHORT).show();
                edAddMember.setText("");
                return;
            }

            syncHelper.checkIfEmailExists(email, (exists, user) -> {
                if (exists) {
                    Toast.makeText(getBaseContext(), "Add User success", Toast.LENGTH_SHORT).show();

                    syncHelper.updateFirebaseSharedNote(noteId, "add", user.getId());
                    edAddMember.setText("");
                    sharedUserList.add(user);
                    runOnUiThread(() -> {
                        adapter.setListSharedUser(sharedUserList);
                    });
                } else {
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

        int newIndex = lrContent.getChildCount();
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

        editText.addTextChangedListener(new TextWatcher() {
            private String previousText = content != null ? content : "";
            private boolean isUpdating = false;
            private int selectionStart;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                selectionStart = start;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return; // Ngăn vòng lặp vô hạn

                String rawText = s.toString();
                if (rawText.equals(previousText)) return;

                isUpdating = true;
                int cursorPosition = editText.getSelectionStart(); // Giữ vị trí con trỏ

                // Đếm số lượng danh sách trước khi thay đổi
                int countBefore = countOccurrences(previousText, "• ");

                // Nếu có dòng bắt đầu bằng "- ", chuyển thành "• "
                if (rawText.contains("\n- ") || rawText.startsWith("- ")) {
                    rawText = rawText.replaceAll("(?m)^- ", "• ");
                    editText.setText(rawText);
                    editText.setSelection(Math.min(cursorPosition + 2, rawText.length()));
                    isEditing = true;
                }

                // 🔥 Khi nhấn ENTER trong chế độ danh sách, thêm "• "
                if (isEditing && rawText.endsWith("\n")) {
                    rawText += "• "; // Thêm "• " vào cuối
                    editText.setText(rawText);
                    editText.setSelection(rawText.length()); // Giữ vị trí con trỏ
                }


                // Đếm số lượng danh sách sau khi thay đổi
                int countAfter = countOccurrences(rawText, "• ");
                int countDashAfter = countOccurrences(rawText, "\n- ");

                // Bật isEditing nếu vừa thêm dấu "• "
                if (!isEditing && rawText.endsWith("• ")) {
                    isEditing = true;
                }

                // Nếu số lượng "• " giảm hoặc không còn "- ", tắt isEditing
                if (countAfter < countBefore && countDashAfter == 0) {
                    isEditing = false;
                }

                // 🔥 Cập nhật Firebase nếu có thay đổi
                if (!rawText.equals(previousText)) {
                    DatabaseReference noteRef = FirebaseDatabase.getInstance()
                            .getReference("notes").child(noteId)
                            .child("content").child(String.valueOf(editText.getTag()))
                            .child("textContent");

                    noteRef.setValue(translateHtml(s)).addOnCompleteListener(task -> {
                        isUpdating = false;
                    });
                }

                previousText = rawText; // Cập nhật nội dung trước đó
                isUpdating = false;
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

        cellRef.setValue("");
        cell.setTag(new int[]{tablePosition, rowIndex, colIndex}); // Lưu vị trí vào Tag

        cell.addTextChangedListener(new TextWatcher() {
            private String previousText = text;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String newText = s.toString();
                if (newText.equals(previousText)) return;

                previousText = newText;

                // 🔥 Lấy vị trí mới nhất từ Tag
                int[] positions = (int[]) cell.getTag();
                int tablePos = positions[0];
                int rowPos = positions[1];
                int colPos = positions[2];

                DatabaseReference cellRef = FirebaseDatabase.getInstance().getReference("notes")
                        .child(noteId).child("content").child(String.valueOf(tablePos))
                        .child("tableContent").child(String.valueOf(rowPos))
                        .child(String.valueOf(colPos));

                cellRef.setValue(translateHtml(s));
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
        return button;
    }
    @SuppressLint("SetTextI18n")
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
        LinearLayout rowUpdateRow = new LinearLayout(this);
        rowUpdateRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        rowUpdateRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView buttonRow = createBtMoreButton(false);
        // Tạo bảng
        TableLayout tableLayout = createTable(tableData, position);
        rowUpdateRow.addView(buttonRow);
        rowUpdateRow.addView(tableLayout);

        mainLayout.addView(rowUpdateColumn);
        mainLayout.addView(rowUpdateRow);

        tableLayout.setTag(position);
        buttonColumn.setTag(tableLayout);
        buttonRow.setTag(tableLayout);

        buttonColumn.setOnClickListener(v -> {
            View view = getLayoutInflater().inflate(R.layout.custom_update_table_column, null);
            PopupWindow popupWindow = new PopupWindow(
                    view,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );
            int[] location = new int[2];
            v.getLocationOnScreen(location);

            TextView addColumn = (TextView) view.findViewById(R.id.addColumn);
            TextView deleteColumn = (TextView) view.findViewById(R.id.deleteColumn);

            TableLayout currentTable = (TableLayout) v.getTag();
            addColumn.setOnClickListener(v1 -> {
                if (currentTable != null) {
                    int tablePosition = (int) currentTable.getTag(); // Vị trí bảng
                    int rowCount = currentTable.getChildCount();

                    // Lấy số cột hiện tại từ hàng đầu tiên (nếu có)
                    int colIndex = 0;
                    if (rowCount > 0) {
                        TableRow firstRow = (TableRow) currentTable.getChildAt(0);
                        for (int i = 0; i < firstRow.getChildCount(); i++) {
                            if (firstRow.getChildAt(i) instanceof EditText) {
                                colIndex++;
                            }
                        }
                    }
                    // Giới hạn số cột tối đa là 3
                    int colCount = colIndex;
                    if (colCount >= 3) {
                        Toast.makeText(v1.getContext(), "Maximum 3 columns", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Thêm cột vào tất cả các hàng
                    int tableRowIndex = 0; // Biến đếm số thứ tự thực sự của TableRow

                    for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                        View rowView = currentTable.getChildAt(rowIndex);

                        if (!(rowView instanceof TableRow)) continue; // Nếu không phải TableRow thì bỏ qua

                        TableRow row = (TableRow) rowView;

                        View divider = createVerticalDivider();
                        CustomEditText newCell = createTableCell("", tablePosition, tableRowIndex, colIndex);
                        row.addView(divider);
                        row.addView(newCell);

                        tableRowIndex++;
                    }

                }
            });
            deleteColumn.setOnClickListener(v1 -> {
                if (currentTable != null){
                    int tablePosition = (int) currentTable.getTag();
                    int rowCount = currentTable.getChildCount();
                    int colIndex = 0;
                    if (rowCount > 0){
                        TableRow firstRow = (TableRow) currentTable.getChildAt(0);
                        for (int i = 0; i < firstRow.getChildCount(); i++){
                            if (firstRow.getChildAt(i) instanceof EditText){
                                colIndex++;
                            }
                        }
                    }
                    // Không cho xóa nếu chỉ còn 1 cột
                    if (colIndex <= 1) {
                        Toast.makeText(this, "At least 1 column required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int tableRowIndex = 0;
                    for (int i = 0; i < rowCount; i++){
                        View rowView = currentTable.getChildAt(i);
                        if (!(rowView instanceof TableRow)) continue;

                        TableRow row = (TableRow) rowView;
                        row.removeViewAt(row.getChildCount() - 1);
                        row.removeViewAt(row.getChildCount() - 1);

                        DatabaseReference cellRef = FirebaseDatabase.getInstance().getReference("notes")
                                .child(noteId).child("content").child(String.valueOf(tablePosition))
                                .child("tableContent").child(String.valueOf(tableRowIndex))
                                .child(String.valueOf(colIndex - 1));
                        cellRef.removeValue();
                        tableRowIndex++;
                    }
                }
            });

            // Hiển thị popup ngay bên dưới buttonColumn + cách 10dp
            popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, location[0], location[1] + v.getHeight());

        });
        buttonColumn.setOnLongClickListener(v -> {
            View view = getLayoutInflater().inflate(R.layout.delete_table, null);
            PopupWindow popupWindow = new PopupWindow(
                    view,
                    dpToPx(140),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );
            TextView btnDeleteTable = (TextView) view.findViewById(R.id.btnDeleteTable);
            btnDeleteTable.setOnClickListener(v1 ->     {
                int tableIndex = (int) tableLayout.getTag();

                EditText editTextAhead = (EditText) lrContent.getChildAt(tableIndex - 1);
                EditText editTextBehind = (EditText) lrContent.getChildAt(tableIndex + 1);
                if (!editTextBehind.getText().toString().isEmpty()) {
                    editTextAhead.setText(editTextAhead.getText().toString() + "\n" + editTextBehind.getText().toString());
                }

                lrContent.removeViewAt(tableIndex  + 1);
                lrContent.removeViewAt(tableIndex);

                // 🚀 Cập nhật lại contentList
                if (tableIndex >= 0 && tableIndex < contentList.size()) {
                    contentList.remove(tableIndex);
                    if (tableIndex < contentList.size() && "text".equals(contentList.get(tableIndex).getType())) {
                        contentList.get(tableIndex).setTextContent(editTextAhead.getText().toString());
                    }
                }

                // 🔥 Cập nhật lại tag cho tất cả EditText còn lại
                for (int i = 0; i < lrContent.getChildCount(); i++) {
                    View child = lrContent.getChildAt(i);
                    if (child instanceof EditText) {
                        child.setTag(i);
                    } else if (child instanceof LinearLayout) {
                        updateTableTags((LinearLayout) child, i);
                    }
                }

                updateFirebase();
                popupWindow.dismiss();
            });
            int[] location = new int[2];
            v.getLocationOnScreen(location);
            popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, location[0], location[1] + v.getHeight());
            return true;
        });

        buttonRow.setOnClickListener(v -> {
            View view = getLayoutInflater().inflate(R.layout.custom_update_table_row, null);
            PopupWindow popupWindow = new PopupWindow(
                    view,
                    dpToPx(140),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );
            TextView btnDeleteRow = (TextView) view.findViewById(R.id.btnDeleteRow);
            TextView btnAddRow = (TextView) view.findViewById(R.id.btnAddRow);
            TableLayout currentTable = (TableLayout) v.getTag();
            currentTable.setTag(tableLayout.getTag());
            Log.d("DEBUG", "currentTable: " + tableLayout.getTag());
            btnDeleteRow.setOnClickListener(v1 -> {
                if (currentTable != null){
                    int tablePos = (int) currentTable.getTag();
                    int lastRow = currentTable.getChildCount() - 1;
                    int rowCount = 0;
                    for (int i = 0; i < currentTable.getChildCount(); i++){
                        if (currentTable.getChildAt(i) instanceof TableRow){
                            rowCount++;
                        }
                    }
                    if (currentTable.getChildCount() < 2) {
                        Toast.makeText(v1.getContext(), "At least 1 row required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    currentTable.removeViewAt(lastRow);
                    currentTable.removeViewAt(lastRow - 1);

                    DatabaseReference noteRef = FirebaseDatabase.getInstance().getReference("notes")
                            .child(noteId).child("content").child(String.valueOf(tablePos))
                            .child("tableContent").child(String.valueOf(rowCount - 1));
                    noteRef.removeValue();
                }
            });
            btnAddRow.setOnClickListener(v1 -> {
                if (currentTable != null) {
                    int tablePos = (int) currentTable.getTag();
                    Log.d("DEBUG", "currentTable: " + tableLayout.getTag());
                    int colTotal = 0;
                    int rowCount = 0;

                    if (currentTable.getChildCount() > 0) {
                        TableRow firstRow = (TableRow) currentTable.getChildAt(0);
                        for (int i = 0; i < firstRow.getChildCount(); i++) {
                            if (firstRow.getChildAt(i) instanceof EditText) {
                                colTotal++;
                            }
                        }
                    }

                    for (int i = 0; i < currentTable.getChildCount(); i++) {
                        if (currentTable.getChildAt(i) instanceof TableRow) {
                            rowCount++;
                        }
                    }

                    if (colTotal > 0) {
                        currentTable.addView(createHorizontalDivider()); // Thêm đường kẻ ngang
                        TableRow newRow = new TableRow(this);

                        for (int i = 0; i < colTotal; i++) {
                            CustomEditText cell = createTableCell("", tablePos, rowCount, i);
                            View divider = createVerticalDivider();
                            newRow.addView(cell);
                            if (i != colTotal - 1) {
                                newRow.addView(divider);
                            }

                        }
                        Log.d("DEBUG", "rowIndex: " + rowCount);
                        currentTable.addView(newRow);
                    }
                }
            });

            int[] location = new int[2];
            v.getLocationOnScreen(location);
            popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, location[0], location[1] + v.getHeight());
        });
        return mainLayout;
    }
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
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
    private void updateFirebase() {
        DatabaseReference noteRef = FirebaseDatabase.getInstance()
                .getReference("notes")
                .child(noteId)
                .child("content");

        List<Map<String, Object>> updatedContent = new ArrayList<>();

        for (int position = 0; position < lrContent.getChildCount(); position++) {
            View view = lrContent.getChildAt(position);
            Map<String, Object> itemData = new HashMap<>();

            if (view instanceof CustomEditText) {
                CustomEditText editText = (CustomEditText) view;
                itemData.put("type", "text");
                itemData.put("textContent", translateHtml(editText.getText()));
            } else if (view instanceof LinearLayout) {
                LinearLayout mainLayout = (LinearLayout) view;

                if (mainLayout.getChildCount() > 1 && mainLayout.getChildAt(1) instanceof LinearLayout) {
                    LinearLayout subLayout = (LinearLayout) mainLayout.getChildAt(1);

                    if (subLayout.getChildCount() > 1 && subLayout.getChildAt(1) instanceof TableLayout) {
                        TableLayout tableLayout = (TableLayout) subLayout.getChildAt(1);
                        itemData.put("type", "table");
                        itemData.put("tableContent", extractTableData(tableLayout));
                    } else {
                        Log.e("Firebase", "TableLayout không hợp lệ");
                    }
                } else {
                    Log.e("Firebase", "LinearLayout không hợp lệ");
                }
            }

            updatedContent.add(itemData);
        }

        noteRef.setValue(updatedContent)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Update successful"))
                .addOnFailureListener(e -> Log.e("Firebase", "Update failed", e));
    }
    private List<List<String>> extractTableData(TableLayout tableLayout) {
        List<List<String>> tableData = new ArrayList<>();

        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            View rowView = tableLayout.getChildAt(i);
            if (rowView instanceof TableRow) {
                TableRow row = (TableRow) rowView;
                List<String> rowData = new ArrayList<>();

                for (int j = 0; j < row.getChildCount(); j++) {
                    View cell = row.getChildAt(j);
                    if (cell instanceof CustomEditText) {
                        String data = translateHtml(((CustomEditText) cell).getText());
                        rowData.add(data);
                    }
                }
                if (!rowData.isEmpty()) {
                    tableData.add(rowData);
                }
            }
        }
        return tableData;
    }
    private void updateTableTags(LinearLayout mainLayout, int newTablePos) {
        if (mainLayout.getChildCount() > 1 && mainLayout.getChildAt(1) instanceof LinearLayout) {
            LinearLayout subLayout = (LinearLayout) mainLayout.getChildAt(1);
            int rowIndex = 0;
            if (subLayout.getChildCount() > 1 && subLayout.getChildAt(1) instanceof TableLayout) {
                TableLayout tableLayout = (TableLayout) subLayout.getChildAt(1);
                tableLayout.setTag(newTablePos);
                for (int i = 0; i < tableLayout.getChildCount(); i++) {
                    View rowView = tableLayout.getChildAt(i);
                    if (rowView instanceof TableRow) {
                        TableRow row = (TableRow) rowView;
                        int colIndex = 0;
                        for (int j = 0; j < row.getChildCount(); j++) {
                            View cellView = row.getChildAt(j);
                            if (cellView instanceof CustomEditText) {
                                cellView.setTag(new int[]{newTablePos, rowIndex, colIndex});
                                colIndex++;
                            }
                        }
                        rowIndex++;
                    }
                }
            } else {
                Log.e("UpdateTags", "TableLayout không hợp lệ");
            }
        } else {
            Log.e("UpdateTags", "LinearLayout không hợp lệ");
        }
    }
}
