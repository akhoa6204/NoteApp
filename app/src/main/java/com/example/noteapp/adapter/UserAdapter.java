package com.example.noteapp.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserAdapter extends BaseAdapter {
    private final Context mContext;
    private final List<User> listSharedUser;
    private final boolean checkPermission;
    private final String noteId;
    public UserAdapter(Context mContext, List<User> listSharedUser, boolean checkPermission, String noteId) {
        this.mContext = mContext;
        this.listSharedUser = listSharedUser;
        this.checkPermission = checkPermission;
        this.noteId = noteId;
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
            holder.btnDelete =convertView.findViewById(R.id.btnDelete);
            holder.isFirstLoad = true;

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        User user  = listSharedUser.get(position);

        if (user != null) {
            holder.tvName.setText(user.getFirstName() + " " + user.getLastName());
        } else {
            holder.tvName.setText("Không tìm thấy");
        }

        // Danh sách quyền
        String[] permissions = {"Write"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, permissions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.sPermission.setAdapter(adapter);
        FirebaseSyncHelper syncHelper = new FirebaseSyncHelper(mContext);
        syncHelper.getPermissionOfNoteForSharedUser(noteId, user.getId(), new OnSharedNotePermission() {
            @Override
            public void onPermission(int permission) {
                // Gán quyền mặc định
                holder.sPermission.setSelection(permission);
            }
        });

        holder.sPermission.setEnabled(checkPermission);
        holder.sPermission.setClickable(checkPermission);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        holder.sPermission.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (holder.isFirstLoad) {
                    holder.isFirstLoad = false;
                    return;
                }

                if (!checkPermission) return;

                executor.execute(() -> {
                    syncHelper.updatePermission(noteId, listSharedUser.get(position).getId(), pos);
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if(checkPermission){
                syncHelper.updateFirebaseSharedNote(noteId, "remove", listSharedUser.get(position).getId());

                listSharedUser.remove(position);
                notifyDataSetChanged();
                Toast.makeText(mContext, "Delete success", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(mContext, "User is not Owner", Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;
    }

    static class ViewHolder {
        TextView tvName;
        Spinner sPermission;
        ImageView btnDelete;
        boolean isFirstLoad;

    }
}
