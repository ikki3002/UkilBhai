package com.example.ukilbhai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupEmergencyButtons();
        setupAiChatButton();
    }

    private void setupEmergencyButtons() {
        // 999 Police & Emergency Hotline
        View btnEmergency999 = findViewById(R.id.btnEmergency999);
        if (btnEmergency999 != null) {
            btnEmergency999.setOnClickListener(v -> dialPhoneNumber("999"));
        }

        // 16699 National Legal Aid Services Hotline
        View btnLegalAid16699 = findViewById(R.id.btnLegalAid16699);
        if (btnLegalAid16699 != null) {
            btnLegalAid16699.setOnClickListener(v -> dialPhoneNumber("16699"));
        }
    }

    private void setupAiChatButton() {
        // Floating Button: "এআই পরামর্শ নিন (Chat)"
        View fabAiChat = findViewById(R.id.fabAiChat);
        if (fabAiChat != null) {
            fabAiChat.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                startActivity(intent);
            });
        }
    }

    private void dialPhoneNumber(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "কল করার অপশন পাওয়া যায়নি: " + phoneNumber, Toast.LENGTH_SHORT).show();
        }
    }
}
