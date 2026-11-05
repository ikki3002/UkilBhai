package com.example.ukilbhai.rag;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.R;

import java.util.ArrayList;
import java.util.List;

public class LegalSectionAdapter extends RecyclerView.Adapter<LegalSectionAdapter.ViewHolder> {

    public interface OnSectionClickListener {
        void onSectionClick(LegalSection section);
    }

    private final List<LegalSection> sectionList = new ArrayList<>();
    private final OnSectionClickListener listener;

    public LegalSectionAdapter(List<LegalSection> initialList, OnSectionClickListener listener) {
        if (initialList != null) {
            this.sectionList.addAll(initialList);
        }
        this.listener = listener;
    }

    public void updateData(List<LegalSection> newList) {
        this.sectionList.clear();
        if (newList != null) {
            this.sectionList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_legal_section_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LegalSection section = sectionList.get(position);
        holder.bind(section, listener);
    }

    @Override
    public int getItemCount() {
        return sectionList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvActBadge;
        private final TextView tvActYear;
        private final TextView tvSectionTitle;
        private final TextView tvSectionSnippet;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvActBadge = itemView.findViewById(R.id.tvActBadge);
            tvActYear = itemView.findViewById(R.id.tvActYear);
            tvSectionTitle = itemView.findViewById(R.id.tvSectionTitle);
            tvSectionSnippet = itemView.findViewById(R.id.tvSectionSnippet);
        }

        public void bind(LegalSection section, OnSectionClickListener listener) {
            tvActBadge.setText(section.getCitationHeader());
            String year = section.getActYear();
            if (year != null && !year.trim().isEmpty()) {
                tvActYear.setVisibility(View.VISIBLE);
                tvActYear.setText(year.trim());
            } else {
                tvActYear.setVisibility(View.GONE);
            }

            tvSectionTitle.setText(section.getDisplayTitle());
            tvSectionSnippet.setText(section.getSnippet());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSectionClick(section);
                }
            });
        }
    }
}
