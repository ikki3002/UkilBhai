package com.example.ukilbhai.rag;

public class LegalSection {
    private final String actTitle;
    private final String actYear;
    private final String sectionTitle;
    private final String sectionContent;

    public LegalSection(String actTitle, String actYear, String sectionTitle, String sectionContent) {
        this.actTitle = actTitle != null ? actTitle : "";
        this.actYear = actYear != null ? actYear : "";
        this.sectionTitle = sectionTitle != null ? sectionTitle : "";
        this.sectionContent = sectionContent != null ? sectionContent : "";
    }

    public String getActTitle() {
        return actTitle;
    }

    public String getActYear() {
        return actYear;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public String getSectionContent() {
        return sectionContent;
    }

    public String getCitationHeader() {
        if (actYear != null && !actYear.isEmpty() && !actTitle.contains(actYear)) {
            return actTitle + " (" + actYear + ")";
        }
        return actTitle;
    }
}
