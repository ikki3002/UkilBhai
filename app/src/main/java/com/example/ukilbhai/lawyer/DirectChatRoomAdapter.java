package com.example.ukilbhai.lawyer;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.R;
import com.example.ukilbhai.model.DirectChatRoom;
import com.example.ukilbhai.util.ImageStorageHelper;

import java.util.ArrayList;
import java.util.List;

public class DirectChatRoomAdapter extends RecyclerView.Adapter<DirectChatRoomAdapter.ViewHolder> {

    public interface OnRoomClickListener {
        void onRoomClick(DirectChatRoom room);
    }

    private final Context context;
    private final String currentUserId;
    private final boolean isLawyerViewer;
    private final List<DirectChatRoom> roomList = new ArrayList<>();
    private final OnRoomClickListener listener;

    public DirectChatRoomAdapter(Context context, String currentUserId, boolean isLawyerViewer, OnRoomClickListener listener) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.isLawyerViewer = isLawyerViewer;
        this.listener = listener;
    }

    public void setRooms(List<DirectChatRoom> list) {
        roomList.clear();
        if (list != null) {
            roomList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DirectChatRoom room = roomList.get(position);

        String otherName;
        String otherPhoto;
        int unreadCount;

        if (isLawyerViewer) {
            otherName = room.getCitizenName() != null ? room.getCitizenName() : "নাগরিক";
            otherPhoto = room.getCitizenPhoto();
            unreadCount = room.getUnreadLawyerCount();
        } else {
            otherName = room.getLawyerName() != null ? room.getLawyerName() : "আইনজীবী";
            otherPhoto = room.getLawyerPhoto();
            unreadCount = room.getUnreadCitizenCount();
        }

        holder.tvRoomTitle.setText(otherName);
        holder.tvRoomCategory.setText(room.getCaseCategory() != null ? room.getCaseCategory() : "আইনি পরামর্শ");
        holder.tvLastMessage.setText(room.getLastMessage() != null ? room.getLastMessage() : "");

        if (room.getLastMessageTime() > 0) {
            CharSequence timeStr = DateUtils.getRelativeTimeSpanString(
                    room.getLastMessageTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            holder.tvRoomTime.setText(timeStr);
        } else {
            holder.tvRoomTime.setText("");
        }

        if (unreadCount > 0) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadBadge.setText(String.valueOf(unreadCount));
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }

        ImageStorageHelper.loadAvatar(context, holder.ivRoomAvatar, otherPhoto);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRoomClick(room);
        });
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivRoomAvatar;
        final View viewRoomOnlineDot;
        final TextView tvRoomTitle;
        final TextView tvRoomCategory;
        final TextView tvRoomTime;
        final TextView tvLastMessage;
        final TextView tvUnreadBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRoomAvatar = itemView.findViewById(R.id.ivRoomAvatar);
            viewRoomOnlineDot = itemView.findViewById(R.id.viewRoomOnlineDot);
            tvRoomTitle = itemView.findViewById(R.id.tvRoomTitle);
            tvRoomCategory = itemView.findViewById(R.id.tvRoomCategory);
            tvRoomTime = itemView.findViewById(R.id.tvRoomTime);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }
    }
}
