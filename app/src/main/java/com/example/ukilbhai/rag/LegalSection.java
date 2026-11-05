package com.example.ukilbhai.rag;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LegalSection implements Serializable {
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
        String cleanAct = getCleanActTitle();
        if (actYear != null && !actYear.isEmpty() && !cleanAct.contains(actYear)) {
            return cleanAct + " (" + actYear + ")";
        }
        return cleanAct;
    }

    public String getCleanActTitle() {
        if (actTitle == null || actTitle.trim().isEmpty()) return "বাংলাদেশ আইন";
        String t = actTitle.trim();
        // Remove leading numeric artifact like "1The Penal Code" -> "The Penal Code"
        if (t.matches("^\\d+[A-Za-z].*")) {
            t = t.replaceFirst("^\\d+", "");
        }
        return t.trim();
    }

    public String getDisplayTitle() {
        if (sectionTitle != null && sectionTitle.trim().length() > 1) {
            return sectionTitle.trim();
        }

        if (sectionContent == null || sectionContent.trim().isEmpty()) {
            return "আইনি ধারা";
        }

        String trimmed = sectionContent.trim();
        Pattern pattern = Pattern.compile("^([0-9]+|[০-৯]+|[IVXLCDM]+)[.\\u0964)]\\s*(.*)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(trimmed);
        if (matcher.find()) {
            String num = matcher.group(1);
            String rest = matcher.group(2).trim();
            String[] parts = rest.split("[.;:।\\n]");
            String firstClause = parts.length > 0 ? parts[0].trim() : "";
            if (firstClause.length() > 50) {
                firstClause = firstClause.substring(0, 48) + "...";
            }
            if (!firstClause.isEmpty()) {
                return "ধারা " + num + ": " + firstClause;
            }
            return "ধারা " + num;
        }

        String firstLine = trimmed.split("\\n")[0].trim();
        if (firstLine.length() > 50) {
            firstLine = firstLine.substring(0, 48) + "...";
        }
        return !firstLine.isEmpty() ? firstLine : "আইনি ধারা";
    }

    public String getSnippet() {
        if (sectionContent == null) return "";
        String clean = sectionContent.replaceAll("\\s+", " ").trim();
        if (clean.length() > 160) {
            return clean.substring(0, 155) + "...";
        }
        return clean;
    }
}
