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
import com.example.ukilbhai.model.LawyerProfile;
import com.example.ukilbhai.util.ImageStorageHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class LawyerAdapter extends RecyclerView.Adapter<LawyerAdapter.ViewHolder> {

    public interface OnApplyClickListener {
        void onApply(LawyerProfile lawyer);
    }

    private final Context context;
    private final List<LawyerProfile> lawyerList = new ArrayList<>();
    private final OnApplyClickListener listener;

    public LawyerAdapter(Context context, OnApplyClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setLawyers(List<LawyerProfile> list) {
        lawyerList.clear();
        if (list != null) {
            lawyerList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lawyer_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LawyerProfile lawyer = lawyerList.get(position);

        holder.tvLawyerName.setText(lawyer.getName() != null && !lawyer.getName().isEmpty()
                ? lawyer.getName() : "অ্যাডভোকেট");

        // Rating
        holder.tvRating.setText(String.format("⭐ %.1f (%d+)",
                lawyer.getRating() > 0 ? lawyer.getRating() : 5.0,
                lawyer.getCompletedConsultations() > 0 ? lawyer.getCompletedConsultations() : 12));

        // Bar Association / Sanad
        StringBuilder barInfo = new StringBuilder();
        if (!TextUtils.isEmpty(lawyer.getDesignation())) {
            barInfo.append(lawyer.getDesignation());
        } else {
            barInfo.append("অ্যাডভোকেট");
        }
        if (!TextUtils.isEmpty(lawyer.getBarAssociation())) {
            barInfo.append(" • ").append(lawyer.getBarAssociation());
        }
        holder.tvLawyerBar.setText(barInfo.toString());

        // District & Experience
        StringBuilder locExp = new StringBuilder();
        if (!TextUtils.isEmpty(lawyer.getDistrict())) {
            locExp.append("📍 ").append(lawyer.getDistrict());
        }
        if (lawyer.getExperienceYears() > 0) {
            locExp.append(" • ").append(lawyer.getExperienceYears()).append(" বছরের অভিজ্ঞতা");
        }
        holder.tvLawyerDistrict.setText(locExp.toString());

        // Online Dot Indicator
        holder.viewOnlineDot.setVisibility(lawyer.isOnline() ? View.VISIBLE : View.GONE);

        // Tags / Practice Areas
        List<String> areas = lawyer.getPracticeAreas();
        if (areas != null && !areas.isEmpty()) {
            StringBuilder tags = new StringBuilder("🏷️ ");
            for (int i = 0; i < areas.size(); i++) {
                tags.append("#").append(areas.get(i));
                if (i < areas.size() - 1) tags.append("  ");
            }
            holder.tvPracticeAreas.setVisibility(View.VISIBLE);
            holder.tvPracticeAreas.setText(tags.toString());
        } else {
            holder.tvPracticeAreas.setVisibility(View.GONE);
        }

        // Bio
        if (!TextUtils.isEmpty(lawyer.getBio())) {
            holder.tvLawyerBio.setVisibility(View.VISIBLE);
            holder.tvLawyerBio.setText(lawyer.getBio());
        } else {
            holder.tvLawyerBio.setVisibility(View.GONE);
        }

        // Fee
        if (lawyer.getConsultationFee() > 0) {
            holder.tvConsultationFee.setText("৳ " + (int) lawyer.getConsultationFee() + " / সেশন");
        } else {
            holder.tvConsultationFee.setText("ফ্রি পরামর্শ (Pro Bono)");
        }

        // Avatar
        ImageStorageHelper.loadAvatar(context, holder.ivLawyerAvatar, lawyer.getPhotoUrl());

        // Apply Click
        holder.btnApplyConsult.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApply(lawyer);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lawyerList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivLawyerAvatar;
        final View viewOnlineDot;
        final TextView tvLawyerName;
        final TextView tvRating;
        final TextView tvLawyerBar;
        final TextView tvLawyerDistrict;
        final TextView tvPracticeAreas;
        final TextView tvLawyerBio;
        final TextView tvConsultationFee;
        final MaterialButton btnApplyConsult;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivLawyerAvatar = itemView.findViewById(R.id.ivLawyerAvatar);
            viewOnlineDot = itemView.findViewById(R.id.viewOnlineDot);
            tvLawyerName = itemView.findViewById(R.id.tvLawyerName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvLawyerBar = itemView.findViewById(R.id.tvLawyerBar);
            tvLawyerDistrict = itemView.findViewById(R.id.tvLawyerDistrict);
            tvPracticeAreas = itemView.findViewById(R.id.tvPracticeAreas);
            tvLawyerBio = itemView.findViewById(R.id.tvLawyerBio);
            tvConsultationFee = itemView.findViewById(R.id.tvConsultationFee);
            btnApplyConsult = itemView.findViewById(R.id.btnApplyConsult);
        }
    }
}
