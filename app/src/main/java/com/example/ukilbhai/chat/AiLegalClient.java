package com.example.ukilbhai.chat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.content.Context;
import com.example.ukilbhai.BuildConfig;
import com.example.ukilbhai.UkilBhaiApp;
import com.example.ukilbhai.rag.LegalDatabaseHelper;
import com.example.ukilbhai.rag.LegalSection;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiLegalClient {

    private static final String TAG = "AiLegalClient";

    // 1. Groq Cloud (Free high-speed models)
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    // Active free models on Groq
    private static final String[] GROQ_MODELS = {
            "qwen/qwen3.8-27b",
            "openai/gpt-oss-120b",
            "openai/gpt-oss-20b"
    };

    // 2. Google Gemini Models (Supported on Google AI Studio)
    private static final String[] GEMINI_MODELS = {
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-flash-latest"
    };
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    // 3. Hugging Face Router
    private static final String HF_ROUTER_URL = "https://router.huggingface.co/v1/chat/completions";
    private static final String HF_MODEL = "meta-llama/Llama-3.2-3B-Instruct";

    private static final String SYSTEM_PROMPT =
            "আপনি 'উকিল ভাই' (Ukil Bhai), বাংলাদেশের সাধারণ নাগরিকদের জন্য একটি আন্তরিক, সহজবোধ্য ও দক্ষ আইনি পরামর্শক এআই সহকারী।\n" +
            "আপনার দায়িত্ব হলো ব্যবহারকারীর আইনি সমস্যার কথা শুনে বাংলাদেশের প্রযোজ্য আইনের (যেমন: দণ্ডবিধি ১৮৬০, পারিবারিক আইন, সড়ক পরিবহন আইন ২০১৮, যৌতুক নিরোধ আইন ২০১৮, ইত্যাদি) " +
            "ভিত্তিতে সহজ, সাবলীল বাংলায় স্পষ্ট আইনি অধিকার ও ব্যবহারিক পদক্ষেপ ব্যাখ্যা করা।\n" +
            "ফরম্যাটিং সংক্রান্ত নির্দেশাবলী:\n" +
            "১. উত্তর পরিষ্কার ও পাঠযোগ্যভাবে সুন্দর পরিচ্ছন্ন প্যারাগ্রাফ এবং বুলেট পয়েন্ট আকারে উপস্থাপন করুন।\n" +
            "২. অপ্রয়োজনীয় বা ভাঙ্গা টেবিল পরিহার করুন।\n" +
            "৩. গুরুত্বপূর্ণ আইন বা ধারাগুলো বোল্ড (**আইন**) করে লিখুন।\n" +
            "৪. প্রয়োজন হলে শান্ত ও সহানুভূতিশীলভাবে ব্যবহারকারীকে জরুরি হেল্পলাইন (জরুরি সেবা ৯৯৯ এবং জাতীয় আইনি সহায়তা ১৬৬৯৯) এর কথা মনে করিয়ে দিন।";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ResponseCallback {
        void onSuccess(String reply, String providerUsed);
        void onError(String errorMessage);
    }

    public void sendMessage(List<ChatMessage> history, String userMessage, ResponseCallback callback) {
        executorService.execute(() -> {
            String groqKey = BuildConfig.GROQ_API_KEY;
            String geminiKey = BuildConfig.GEMINI_API_KEY;
            String hfKey = BuildConfig.HUGGINGFACE_API_KEY;


            List<String> attemptedErrors = new ArrayList<>();

            // 0. On-Device RAG: Search local SQLite FTS5 database (instant & offline)
            List<LegalSection> retrievedSections = new ArrayList<>();
            Context appContext = UkilBhaiApp.getAppContext();
            if (appContext != null) {
                try {
                    retrievedSections = LegalDatabaseHelper.getInstance(appContext).searchStatutes(userMessage, 3);
                    Log.i(TAG, "On-Device RAG retrieved " + retrievedSections.size() + " sections for: " + userMessage);
                } catch (Exception e) {
                    Log.w(TAG, "On-device statute retrieval error: " + e.getMessage());
                }
            }

            // Build augmented prompt with retrieved statutes
            String augmentedMessage = userMessage;
            if (!retrievedSections.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("[বাংলাদেশ আইন ভাণ্ডার থেকে প্রাসঙ্গিক ধারাসমূহ (Statute Context)]:\n");
                for (int i = 0; i < retrievedSections.size(); i++) {
                    LegalSection s = retrievedSections.get(i);
                    sb.append("--- ধারা ").append(i + 1).append(" ---\n");
                    sb.append("আইন: ").append(s.getCitationHeader()).append("\n");
                    if (!s.getSectionTitle().isEmpty()) {
                        sb.append("শিরোনাম: ").append(s.getSectionTitle()).append("\n");
                    }
                    String preview = s.getSectionContent();
                    if (preview.length() > 600) {
                        preview = preview.substring(0, 600) + "...";
                    }
                    sb.append("মূল পাঠ:\n").append(preview).append("\n\n");
                }
                sb.append("[নাগরিকের আইনি প্রশ্ন]:\n").append(userMessage);
                augmentedMessage = sb.toString();
            }

            final List<LegalSection> finalRetrieved = retrievedSections;

            // 1. Try Groq Cloud first (Known healthy & working on your account)
            if (groqKey != null && !groqKey.trim().isEmpty()) {
                for (String model : GROQ_MODELS) {
                    try {
                        Log.d(TAG, "Attempting Groq Cloud model: " + model);
                        String reply = callOpenAiCompatible(GROQ_API_URL, groqKey.trim(), model, history, augmentedMessage);
                        if (reply != null && !reply.trim().isEmpty()) {
                            String formatted = appendCitations(reply, finalRetrieved);
                            String provider = finalRetrieved.isEmpty() ? "Groq (" + model + ")" : "Groq RAG (" + model + ")";
                            mainHandler.post(() -> callback.onSuccess(formatted, provider));
                            return;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Groq model " + model + " failed: " + e.getMessage());
                        attemptedErrors.add("Groq(" + model + "): " + e.getMessage());
                    }
                }
            }

            // 2. Try Google Gemini models
            if (geminiKey != null && !geminiKey.trim().isEmpty()) {
                for (String model : GEMINI_MODELS) {
                    try {
                        Log.d(TAG, "Attempting Gemini model: " + model);
                        String reply = callGeminiApi(geminiKey.trim(), model, history, augmentedMessage);
                        if (reply != null && !reply.trim().isEmpty()) {
                            String formatted = appendCitations(reply, finalRetrieved);
                            String provider = finalRetrieved.isEmpty() ? "Google " + model : "Google RAG (" + model + ")";
                            mainHandler.post(() -> callback.onSuccess(formatted, provider));
                            return;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Gemini " + model + " failed: " + e.getMessage());
                        attemptedErrors.add("Gemini(" + model + "): " + e.getMessage());
                    }
                }
            }

            // 3. Try Hugging Face Router
            if (hfKey != null && !hfKey.trim().isEmpty()) {
                try {
                    Log.d(TAG, "Attempting Hugging Face router...");
                    String reply = callOpenAiCompatible(HF_ROUTER_URL, hfKey.trim(), HF_MODEL, history, augmentedMessage);
                    if (reply != null && !reply.trim().isEmpty()) {
                        String formatted = appendCitations(reply, finalRetrieved);
                        String provider = finalRetrieved.isEmpty() ? "Hugging Face" : "Hugging Face RAG";
                        mainHandler.post(() -> callback.onSuccess(formatted, provider));
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Hugging Face failed: " + e.getMessage());
                    attemptedErrors.add("HuggingFace: " + e.getMessage());
                }
            }

            // All failed
            mainHandler.post(() -> {
                boolean hasAnyKey = (groqKey != null && !groqKey.isEmpty()) ||
                                   (geminiKey != null && !geminiKey.isEmpty()) ||
                                   (hfKey != null && !hfKey.isEmpty());

                if (!hasAnyKey) {
                    callback.onError("কোন API Key পাওয়া যায়নি। local.properties ফাইলে GROQ_API_KEY অথবা GEMINI_API_KEY যুক্ত করুন।");
                } else {
                    String detail = attemptedErrors.isEmpty() ? "" : " (" + attemptedErrors.get(0) + ")";
                    callback.onError("দুঃখিত, এআই সার্ভারের সাথে সংযোগ স্থাপন করা সম্ভব হয়নি" + detail + "। অনুগ্রহ করে পুনরায় চেষ্টা করুন।");
                }
            });
        });
    }

    // Google Gemini REST Call
    private String callGeminiApi(String apiKey, String modelName, List<ChatMessage> history, String userMessage) throws Exception {
        String endpoint = GEMINI_BASE_URL + modelName + ":generateContent?key=" + apiKey;
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(40000);
        conn.setDoOutput(true);

        JSONObject root = new JSONObject();

        // System instructions
        JSONObject sysInstruction = new JSONObject();
        JSONArray sysParts = new JSONArray();
        JSONObject sysText = new JSONObject();
        sysText.put("text", SYSTEM_PROMPT);
        sysParts.put(sysText);
        sysInstruction.put("parts", sysParts);
        root.put("system_instruction", sysInstruction);

        // Contents
        JSONArray contents = new JSONArray();
        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            JSONObject turn = new JSONObject();
            turn.put("role", msg.getSender() == ChatMessage.SENDER_USER ? "user" : "model");
            JSONArray parts = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("text", msg.getText());
            parts.put(textPart);
            turn.put("parts", parts);
            contents.put(turn);
        }

        JSONObject currentTurn = new JSONObject();
        currentTurn.put("role", "user");
        JSONArray currentParts = new JSONArray();
        JSONObject currentTextPart = new JSONObject();
        currentTextPart.put("text", userMessage);
        currentParts.put(currentTextPart);
        currentTurn.put("parts", currentParts);
        contents.put(currentTurn);

        root.put("contents", contents);

        byte[] input = root.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(input, 0, input.length);
        }

        int statusCode = conn.getResponseCode();
        InputStream stream = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder responseStr = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseStr.append(line);
                }
            }
        }

        if (statusCode >= 200 && statusCode < 300) {
            JSONObject resJson = new JSONObject(responseStr.toString());
            JSONArray candidates = resJson.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject contentObj = firstCandidate.getJSONObject("content");
                JSONArray parts = contentObj.getJSONArray("parts");
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text");
                }
            }
        }
        throw new RuntimeException("Gemini HTTP " + statusCode + ": " + responseStr);
    }

    // OpenAI Compatible Caller (Groq & Hugging Face)
    private String callOpenAiCompatible(String endpointUrl, String apiKey, String modelName, List<ChatMessage> history, String userMessage) throws Exception {
        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setConnectTimeout(25000);
        conn.setReadTimeout(50000);
        conn.setDoOutput(true);

        JSONObject root = new JSONObject();
        root.put("model", modelName);

        JSONArray messagesArray = new JSONArray();

        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", SYSTEM_PROMPT);
        messagesArray.put(sysMsg);

        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            JSONObject chatObj = new JSONObject();
            chatObj.put("role", msg.getSender() == ChatMessage.SENDER_USER ? "user" : "assistant");
            chatObj.put("content", msg.getText());
            messagesArray.put(chatObj);
        }

        JSONObject currentMsg = new JSONObject();
        currentMsg.put("role", "user");
        currentMsg.put("content", userMessage);
        messagesArray.put(currentMsg);

        root.put("messages", messagesArray);

        byte[] input = root.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(input, 0, input.length);
        }

        int statusCode = conn.getResponseCode();
        InputStream stream = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder responseStr = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseStr.append(line);
                }
            }
        }

        if (statusCode >= 200 && statusCode < 300) {
            JSONObject resJson = new JSONObject(responseStr.toString());
            JSONArray choices = resJson.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject messageObj = firstChoice.getJSONObject("message");
                return messageObj.getString("content");
            }
        }
        throw new RuntimeException("HTTP " + statusCode + ": " + responseStr);
    }

    private String appendCitations(String reply, List<LegalSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return reply;
        }
        StringBuilder sb = new StringBuilder(reply);
        sb.append("\n\n---\n**উদ্ধৃত আইন ও ধারাসমূহ (BLAD):**\n");
        for (LegalSection s : sections) {
            sb.append("• 📜 **").append(s.getCitationHeader()).append("**");
            if (!s.getSectionTitle().isEmpty()) {
                sb.append(" — *").append(s.getSectionTitle()).append("*");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
