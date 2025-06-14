package com.example.noteapp.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.noteapp.R;
import com.example.noteapp.interfacePackage.OnSharedNotePermission;
import com.example.noteapp.model.User;
import com.example.noteapp.myDatabase.FirebaseSyncHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserAdapter extends BaseAdapter {
    private final Context mContext;
    private List<User> listSharedUser;
    private final boolean checkPermission;
    private final String noteId;
    private final ExecutorService executor;
    private final FirebaseSyncHelper syncHelper;
    private final ArrayAdapter<String> permissionAdapter;
    private final Handler uiHandler;

    public UserAdapter(Context mContext, List<User> listSharedUser, boolean checkPermission, String noteId) {
        this.mContext = mContext;
        this.listSharedUser = new ArrayList<>(listSharedUser);
        this.checkPermission = checkPermission;
        this.noteId = noteId;
        this.executor = Executors.newSingleThreadExecutor();
        this.syncHelper = new FirebaseSyncHelper(mContext);
        this.uiHandler = new Handler(Looper.getMainLooper());

        String[] permissions = {mContext.getString(R.string.read_only), mContext.getString(R.string.edit)};
        this.permissionAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, permissions);
        this.permissionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public int getCount() {
        return listSharedUser.size();
    }

    @Override
    public Object getItem(int position) {
        return listSharedUser.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.item_member, parent, false);

            holder = new ViewHolder();
            holder.tvName = convertView.findViewById(R.id.tvName);
            holder.sPermission = convertView.findViewById(R.id.sPermission);
            holder.btnDelete = convertView.findViewById(R.id.btnDelete);
            holder.isFirstLoad = true;

            holder.sPermission.setAdapter(permissionAdapter);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        User user = listSharedUser.get(position);

        if (user != null) {
            holder.tvName.setText(user.getName());
        } else {
            holder.tvName.setText(R.string.no_find);
        }

        // Lấy quyền từ Firebase
        syncHelper.getPermissionOfNoteForSharedUser(noteId, user.getId(), permission -> {
            holder.sPermission.setSelection(permission);
        });

        holder.sPermission.setEnabled(checkPermission);
        holder.sPermission.setClickable(checkPermission);

        holder.sPermission.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (holder.isFirstLoad) {
                    holder.isFirstLoad = false;
                    return;
                }
                if (!checkPermission) return;

                executor.execute(() -> syncHelper.updatePermission(noteId, user.getId(), pos));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (checkPermission) {
                executor.execute(() -> {
                    syncHelper.updateFirebaseSharedNote(noteId, "remove", user.getId());

                    // Xóa item an toàn trên UI thread
                    uiHandler.post(() -> {
                        listSharedUser.remove(position);
                        notifyDataSetChanged();
                    });

                    uiHandler.post(() -> Toast.makeText(mContext, "Delete success", Toast.LENGTH_SHORT).show());
                });
            } else {
                Toast.makeText(mContext, "User is not Owner", Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;
    }
    public void setListSharedUser(List<User> listUser) {
        if (listUser == null) return; // Tránh lỗi null

        Log.d("DEBUG UserAdapter", "Cập nhật danh sách user: " + listUser);

        if (!this.listSharedUser.equals(listUser)) {
            this.listSharedUser.clear();
            this.listSharedUser.addAll(listUser);

            new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
        }
    }
    static class ViewHolder {
        TextView tvName;
        Spinner sPermission;
        ImageView btnDelete;
        boolean isFirstLoad;
    }
}
