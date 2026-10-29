package com.example.ukilbhai.lawyer;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.R;
import com.example.ukilbhai.model.DirectChatMessage;
import com.example.ukilbhai.util.ImageStorageHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DirectChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_OUT = 1;
    private static final int VIEW_TYPE_IN = 2;

    private final Context context;
    private final String currentUserId;
    private final List<DirectChatMessage> messageList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public DirectChatAdapter(Context context, String currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<DirectChatMessage> list) {
        messageList.clear();
        if (list != null) {
            messageList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        DirectChatMessage msg = messageList.get(position);
        if (currentUserId != null && currentUserId.equals(msg.getSenderId())) {
            return VIEW_TYPE_OUT;
        }
        return VIEW_TYPE_IN;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_OUT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_direct_message_out, parent, false);
            return new OutMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_direct_message_in, parent, false);
            return new InMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DirectChatMessage msg = messageList.get(position);
        String timeStr = msg.getTimestamp() > 0 ? timeFormat.format(new Date(msg.getTimestamp())) : "";

        if (holder instanceof OutMessageViewHolder) {
            OutMessageViewHolder out = (OutMessageViewHolder) holder;
            out.tvMessageText.setText(msg.getText());
            out.tvMessageTime.setText(timeStr);

            if (!TextUtils.isEmpty(msg.getAttachmentUrl()) && !"none".equals(msg.getAttachmentType())) {
                out.ivAttachment.setVisibility(View.VISIBLE);
                ImageStorageHelper.loadAvatar(context, out.ivAttachment, msg.getAttachmentUrl());
            } else {
                out.ivAttachment.setVisibility(View.GONE);
            }

            if (msg.isRead()) {
                out.ivMessageStatus.setImageResource(R.drawable.ic_pin_filled);
            } else {
                out.ivMessageStatus.setImageResource(R.drawable.ic_pin);
            }
        } else if (holder instanceof InMessageViewHolder) {
            InMessageViewHolder in = (InMessageViewHolder) holder;
            in.tvMessageText.setText(msg.getText());
            in.tvMessageTime.setText(timeStr);

            if (!TextUtils.isEmpty(msg.getAttachmentUrl()) && !"none".equals(msg.getAttachmentType())) {
                in.ivAttachment.setVisibility(View.VISIBLE);
                ImageStorageHelper.loadAvatar(context, in.ivAttachment, msg.getAttachmentUrl());
            } else {
                in.ivAttachment.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class OutMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessageText;
        final TextView tvMessageTime;
        final ImageView ivMessageStatus;
        final ImageView ivAttachment;

        OutMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            ivMessageStatus = itemView.findViewById(R.id.ivMessageStatus);
            ivAttachment = itemView.findViewById(R.id.ivAttachment);
        }
    }

    static class InMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessageText;
        final TextView tvMessageTime;
        final ImageView ivAttachment;

        InMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            ivAttachment = itemView.findViewById(R.id.ivAttachment);
        }
    }
}
