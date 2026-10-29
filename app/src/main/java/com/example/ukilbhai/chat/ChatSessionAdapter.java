package com.example.ukilbhai.chat;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.R;
import com.example.ukilbhai.util.LanguageHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.SessionViewHolder> {

    public interface OnSessionActionListener {
        void onSessionSelected(ChatSession session);
        void onSessionPinToggle(ChatSession session);
        void onSessionDelete(ChatSession session);
    }

    private final List<ChatSession> sessionList = new ArrayList<>();
    private OnSessionActionListener listener;
    private String activeSessionId = null;

    public void setOnSessionActionListener(OnSessionActionListener listener) {
        this.listener = listener;
    }

    public void setActiveSessionId(String activeSessionId) {
        this.activeSessionId = activeSessionId;
        notifyDataSetChanged();
    }

    public void updateSessions(List<ChatSession> newSessions) {
        sessionList.clear();
        if (newSessions != null) {
            sessionList.addAll(newSessions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ChatSession session = sessionList.get(position);
        Context context = holder.itemView.getContext();
        boolean isBn = LanguageHelper.isBengali(context);

        holder.tvSessionTitle.setText(session.getTitle());

        // Date and message count summary
        int msgCount = session.getMessages().size();
        String dateStr = formatDate(context, session.getUpdatedAt(), isBn);
        String countStr = isBn ? (msgCount + "টি বার্তা") : (msgCount + " msgs");
        holder.tvSessionDate.setText(dateStr + " • " + countStr);

        // Highlight active session
        boolean isActive = session.getId().equals(activeSessionId);
        if (isActive) {
            holder.itemView.setBackgroundResource(R.drawable.bg_search_pill);
            holder.ivSessionIcon.setColorFilter(ContextCompat.getColor(context, R.color.mint_accent));
            holder.tvSessionTitle.setTextColor(ContextCompat.getColor(context, R.color.mint_accent));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_search_pill);
            holder.ivSessionIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
            holder.tvSessionTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }

        // Pin icon state
        if (session.isPinned()) {
            holder.btnPinSession.setImageResource(R.drawable.ic_pin_filled);
            holder.btnPinSession.setColorFilter(ContextCompat.getColor(context, R.color.mint_accent));
            holder.btnPinSession.setContentDescription(isBn ? "পিন খুলুন" : "Unpin");
        } else {
            holder.btnPinSession.setImageResource(R.drawable.ic_pin);
            holder.btnPinSession.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
            holder.btnPinSession.setContentDescription(isBn ? "পিন করুন" : "Pin");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSessionSelected(session);
            }
        });

        holder.btnPinSession.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSessionPinToggle(session);
            }
        });

        holder.btnDeleteSession.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSessionDelete(session);
            }
        });
    }

    private String formatDate(Context context, long timeMillis, boolean isBn) {
        if (DateUtils.isToday(timeMillis)) {
            return isBn ? "আজ" : "Today";
        }
        if (DateUtils.isToday(timeMillis + DateUtils.DAY_IN_MILLIS)) {
            return isBn ? "গতকাল" : "Yesterday";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", isBn ? new Locale("bn") : Locale.getDefault());
        return sdf.format(new Date(timeMillis));
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSessionIcon;
        TextView tvSessionTitle;
        TextView tvSessionDate;
        ImageButton btnPinSession;
        ImageButton btnDeleteSession;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSessionIcon = itemView.findViewById(R.id.ivSessionIcon);
            tvSessionTitle = itemView.findViewById(R.id.tvSessionTitle);
            tvSessionDate = itemView.findViewById(R.id.tvSessionDate);
            btnPinSession = itemView.findViewById(R.id.btnPinSession);
            btnDeleteSession = itemView.findViewById(R.id.btnDeleteSession);
        }
    }
}
