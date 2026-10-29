package com.example.ukilbhai.lawyer;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.R;
import com.example.ukilbhai.model.ConsultationRequest;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ConsultationRequestAdapter extends RecyclerView.Adapter<ConsultationRequestAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(ConsultationRequest request);
        void onDecline(ConsultationRequest request);
    }

    private final Context context;
    private final List<ConsultationRequest> requestList = new ArrayList<>();
    private final OnRequestActionListener listener;

    public ConsultationRequestAdapter(Context context, OnRequestActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setRequests(List<ConsultationRequest> list) {
        requestList.clear();
        if (list != null) {
            requestList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_consultation_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConsultationRequest req = requestList.get(position);

        holder.tvCitizenName.setText(req.getCitizenName() != null ? req.getCitizenName() : "নাগরিক");
        holder.tvCitizenDistrict.setText(!TextUtils.isEmpty(req.getCitizenDistrict())
                ? "📍 জেলা: " + req.getCitizenDistrict() : "সাধারণ নাগরিক");

        holder.tvCategoryBadge.setText(req.getCaseCategory() != null ? req.getCaseCategory() : "আইনি পরামর্শ");
        holder.tvCaseSummary.setText(req.getCaseSummary() != null ? req.getCaseSummary() : "");

        if (!TextUtils.isEmpty(req.getCitizenPhone())) {
            holder.tvCitizenPhone.setVisibility(View.VISIBLE);
            holder.tvCitizenPhone.setText("📞 " + req.getCitizenPhone());
        } else {
            holder.tvCitizenPhone.setVisibility(View.GONE);
        }

        if (req.getRequestedAt() > 0) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    req.getRequestedAt(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            holder.tvRequestTime.setText("আবেদনের সময়: " + timeAgo);
        } else {
            holder.tvRequestTime.setText("");
        }

        boolean isPending = ConsultationRequest.STATUS_PENDING.equalsIgnoreCase(req.getStatus());
        if (isPending) {
            holder.layoutActionButtons.setVisibility(View.VISIBLE);
            holder.tvRequestStatus.setVisibility(View.GONE);

            holder.btnAccept.setOnClickListener(v -> {
                if (listener != null) listener.onAccept(req);
            });
            holder.btnDecline.setOnClickListener(v -> {
                if (listener != null) listener.onDecline(req);
            });
        } else {
            holder.layoutActionButtons.setVisibility(View.GONE);
            holder.tvRequestStatus.setVisibility(View.VISIBLE);
            if (ConsultationRequest.STATUS_ACCEPTED.equalsIgnoreCase(req.getStatus())) {
                holder.tvRequestStatus.setText("✓ গৃহীত হয়েছে (চ্যাট চলছে)");
                holder.tvRequestStatus.setTextColor(context.getResources().getColor(R.color.mint_accent));
            } else if (ConsultationRequest.STATUS_DECLINED.equalsIgnoreCase(req.getStatus())) {
                holder.tvRequestStatus.setText("✕ প্রত্যাখ্যাত");
                holder.tvRequestStatus.setTextColor(context.getResources().getColor(R.color.text_secondary));
            } else {
                holder.tvRequestStatus.setText("✓ পরামর্শ সমাপ্ত");
                holder.tvRequestStatus.setTextColor(context.getResources().getColor(R.color.mint_accent));
            }
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCitizenName;
        final TextView tvCitizenDistrict;
        final TextView tvCategoryBadge;
        final TextView tvCaseSummary;
        final TextView tvCitizenPhone;
        final TextView tvRequestTime;
        final LinearLayout layoutActionButtons;
        final MaterialButton btnDecline;
        final MaterialButton btnAccept;
        final TextView tvRequestStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCitizenName = itemView.findViewById(R.id.tvCitizenName);
            tvCitizenDistrict = itemView.findViewById(R.id.tvCitizenDistrict);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvCaseSummary = itemView.findViewById(R.id.tvCaseSummary);
            tvCitizenPhone = itemView.findViewById(R.id.tvCitizenPhone);
            tvRequestTime = itemView.findViewById(R.id.tvRequestTime);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            tvRequestStatus = itemView.findViewById(R.id.tvRequestStatus);
        }
    }
}
