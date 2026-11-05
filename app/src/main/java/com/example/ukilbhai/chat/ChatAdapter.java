package com.example.ukilbhai.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.R;

import java.util.List;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnChatActionListener {
        void onReadAloud(int position, String text);
        void onCopy(int position, String text);
        void onRegenerate(int position, ChatMessage message);
        default void onConsultLawyer(int position, ChatMessage message) {}
    }

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private final List<ChatMessage> messageList;
    private final Markwon markwon;
    private OnChatActionListener actionListener;
    private int currentlySpeakingPosition = -1;

    public ChatAdapter(Context context, List<ChatMessage> messageList) {
        this.messageList = messageList;
        this.markwon = Markwon.builder(context)
                .usePlugin(TablePlugin.create(context))
                .usePlugin(StrikethroughPlugin.create())
                .build();
    }

    public void setOnChatActionListener(OnChatActionListener listener) {
        this.actionListener = listener;
    }

    public void setCurrentlySpeakingPosition(int position) {
        int previous = currentlySpeakingPosition;
        currentlySpeakingPosition = position;
        if (previous != -1 && previous < messageList.size()) {
            notifyItemChanged(previous);
        }
        if (currentlySpeakingPosition != -1 && currentlySpeakingPosition < messageList.size()) {
            notifyItemChanged(currentlySpeakingPosition);
        }
    }

    public int getCurrentlySpeakingPosition() {
        return currentlySpeakingPosition;
    }

    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).getSender() == ChatMessage.SENDER_USER) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_AI;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_ai, parent, false);
            return new AiViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        int primaryTextColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary);

        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.tvMessage.setTextColor(primaryTextColor);
            userHolder.tvMessage.setText(message.getText());
        } else if (holder instanceof AiViewHolder) {
            AiViewHolder aiHolder = (AiViewHolder) holder;
            if (message.isError()) {
                aiHolder.tvMessage.setTextColor(0xFFEF4444); // Error red
                aiHolder.tvMessage.setText(message.getText());
                if (aiHolder.layoutAiActions != null) {
                    aiHolder.layoutAiActions.setVisibility(View.VISIBLE);
                }
                if (aiHolder.btnCopyAi != null) {
                    aiHolder.btnCopyAi.setVisibility(View.GONE);
                }
                if (aiHolder.btnReadAloud != null) {
                    aiHolder.btnReadAloud.setVisibility(View.GONE);
                }
                if (aiHolder.btnConsultLawyerAi != null) {
                    aiHolder.btnConsultLawyerAi.setVisibility(View.GONE);
                }
                if (aiHolder.btnTryAgain != null) {
                    aiHolder.btnTryAgain.setVisibility(View.VISIBLE);
                    aiHolder.btnTryAgain.setColorFilter(0xFFEF4444);
                    aiHolder.btnTryAgain.setOnClickListener(v -> {
                        if (actionListener != null) {
                            actionListener.onRegenerate(holder.getAdapterPosition(), message);
                        }
                    });
                }
            } else {
                aiHolder.tvMessage.setTextColor(primaryTextColor);
                if (aiHolder.layoutAiActions != null) {
                    aiHolder.layoutAiActions.setVisibility(View.VISIBLE);
                }
                if (aiHolder.btnCopyAi != null) {
                    aiHolder.btnCopyAi.setVisibility(View.VISIBLE);
                    aiHolder.btnCopyAi.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
                    aiHolder.btnCopyAi.setOnClickListener(v -> {
                        if (actionListener != null) {
                            actionListener.onCopy(holder.getAdapterPosition(), message.getText());
                        }
                    });
                }
                if (aiHolder.btnTryAgain != null) {
                    aiHolder.btnTryAgain.setVisibility(View.VISIBLE);
                    aiHolder.btnTryAgain.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
                    aiHolder.btnTryAgain.setOnClickListener(v -> {
                        if (actionListener != null) {
                            actionListener.onRegenerate(holder.getAdapterPosition(), message);
                        }
                    });
                }
                if (aiHolder.btnConsultLawyerAi != null) {
                    aiHolder.btnConsultLawyerAi.setVisibility(View.VISIBLE);
                    aiHolder.btnConsultLawyerAi.setOnClickListener(v -> {
                        if (actionListener != null) {
                            actionListener.onConsultLawyer(holder.getAdapterPosition(), message);
                        }
                    });
                }

                // Render rich markdown (bold, lists, headings, and tables)
                markwon.setMarkdown(aiHolder.tvMessage, message.getText());

                boolean isSpeaking = (position == currentlySpeakingPosition);
                if (aiHolder.btnReadAloud != null) {
                    aiHolder.btnReadAloud.setVisibility(View.VISIBLE);
                    if (isSpeaking) {
                        aiHolder.btnReadAloud.setImageResource(R.drawable.ic_stop);
                        aiHolder.btnReadAloud.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.mint_accent));
                        aiHolder.btnReadAloud.setContentDescription("Stop Reading");
                    } else {
                        aiHolder.btnReadAloud.setImageResource(R.drawable.ic_speaker);
                        aiHolder.btnReadAloud.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
                        aiHolder.btnReadAloud.setContentDescription("Read Aloud");
                    }

                    aiHolder.btnReadAloud.setOnClickListener(v -> {
                        if (actionListener != null) {
                            actionListener.onReadAloud(holder.getAdapterPosition(), message.getText());
                        }
                    });
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvUserMessage);
        }
    }

    static class AiViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        View layoutAiActions;
        ImageButton btnCopyAi;
        ImageButton btnTryAgain;
        TextView btnConsultLawyerAi;
        ImageButton btnReadAloud;

        AiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvAiMessage);
            layoutAiActions = itemView.findViewById(R.id.layoutAiActions);
            btnCopyAi = itemView.findViewById(R.id.btnCopyAi);
            btnTryAgain = itemView.findViewById(R.id.btnTryAgain);
            btnConsultLawyerAi = itemView.findViewById(R.id.btnConsultLawyerAi);
            btnReadAloud = itemView.findViewById(R.id.btnReadAloud);
        }
    }
}
