import os
import json
import logging
import requests
from typing import List, Dict, Any, Optional, Tuple
from app.config import (
    GROQ_API_KEY,
    GEMINI_API_KEY,
    DEFAULT_GROQ_MODEL,
    DEFAULT_GEMINI_MODEL
)

logger = logging.getLogger("LegalGenerator")

SYSTEM_INSTRUCTION = (
    "আপনি 'উকিল ভাই' (Ukil Bhai), বাংলাদেশের সাধারণ নাগরিকদের জন্য একটি আন্তরিক, সহজবোধ্য ও নির্ভরযোগ্য ডিজিটাল আইনি পরামর্শক এআই সহকারী।\n"
    "আপনার কাছে বাংলাদেশ আইন ভান্ডার (Bangladesh Legal Acts Dataset) থেকে পরীক্ষিত এবং প্রাসঙ্গিক ধারাসমূহ সরবরাহ করা হয়েছে।\n"
    "আপনার দায়িত্ব হলো এই সরবরাহকৃত আইনের ধারার ওপর ভিত্তি করে নাগরিকের সমস্যা বা প্রশ্নের স্পষ্ট, সঠিক ও ব্যবহারিক পরামর্শ দেওয়া।\n\n"
    "উত্তর দেওয়ার নিয়মাবলী:\n"
    "১. উত্তরে প্রযোজ্য আইন ও ধারার নাম পরিষ্কারভাবে বোল্ড করে উল্লেখ করুন (যেমন: **দণ্ডবিধি ১৮৬০ এর ধারা ৩০২** বা **সড়ক পরিবহন আইন ২০১৮ এর ধারা ৯২**)।\n"
    "২. আইনি পরিভাষাগুলো সাধারণ মানুষের বোঝার মতো সহজ ও সাবলীল বাংলায় বুঝিয়ে বলুন।\n"
    "৩. পরিচ্ছন্ন অনুচ্ছেদ ও বুলেট পয়েন্ট আকারে উত্তর সাজান। কোনো ভাঙ্গা বা অসম্পূর্ণ টেবিল ব্যবহার করবেন না।\n"
    "৪. শুধুমাত্র সরবরাহকৃত বাস্তব আইনের আলোকেই পরামর্শ দিন, কোনো কাল্পনিক ধারা তৈরি করবেন না।\n"
    "৫. উপযুক্ত ক্ষেত্রে আইনি অধিকারের পাশাপাশি ব্যবহারিক করণীয় (থানায় জিডি/এফআইআর, আদালতে অভিযোগ, বা জাতীয় জরুরি সেবা ৯৯৯ / জাতীয় আইনি সেবা ১৬৬৯৯) উল্লেখ করুন।"
)

def format_context(sections: List[Dict[str, Any]]) -> str:
    """Format retrieved sections into readable prompt context."""
    if not sections:
        return "কোনো সরাসরি সংবিধিবদ্ধ ধারা পাওয়া যায়নি। সাধারণ বাংলাদেশ আইনের ভিত্তিতে উত্তর দিন।"
    
    context_lines = ["\n[বাংলাদেশ আইনের প্রাসঙ্গিক ধারাসমূহ (Retrieved Legal Statutes)]:\n"]
    for i, s in enumerate(sections, 1):
        context_lines.append(f"--- ধারা {i} ---")
        context_lines.append(f"আইনের নাম: {s.get('act_title', '')} (বছর: {s.get('act_year', '')})")
        context_lines.append(f"ধারার শিরোনাম: {s.get('section_title', '')}")
        context_lines.append(f"ধারার মূল পাঠ:\n{s.get('section_content', '')}\n")
    return "\n".join(context_lines)

def generate_legal_advice(
    query: str,
    sections: List[Dict[str, Any]],
    history: Optional[List[Dict[str, str]]] = None
) -> Tuple[str, str, List[Dict[str, Any]]]:
    """
    Synthesize legal advice using retrieved sections.
    Returns: (answer_text, provider_name, citations_list)
    """
    context_str = format_context(sections)
    user_prompt = f"{context_str}\n\n[নাগরিকের আইনি প্রশ্ন]:\n{query}\n\n[উকিল ভাই এর পরামর্শ]:"

    # Build citations list for client badges
    citations = []
    for s in sections:
        citations.append({
            "act_title": s.get("act_title", ""),
            "act_year": s.get("act_year", ""),
            "section_title": s.get("section_title", ""),
            "score": s.get("score", 0.0),
            "preview": (s.get("section_content", "")[:200] + "...") if len(s.get("section_content", "")) > 200 else s.get("section_content", "")
        })

    # 1. Try Groq Cloud first (High speed & reliable)
    if GROQ_API_KEY and GROQ_API_KEY.strip():
        try:
            answer = _call_groq(user_prompt, history)
            if answer:
                return answer, f"Groq ({DEFAULT_GROQ_MODEL})", citations
        except Exception as e:
            logger.warning(f"Groq API call failed: {e}")

    # 2. Try Google Gemini API
    if GEMINI_API_KEY and GEMINI_API_KEY.strip():
        try:
            answer = _call_gemini(user_prompt, history)
            if answer:
                return answer, f"Google ({DEFAULT_GEMINI_MODEL})", citations
        except Exception as e:
            logger.warning(f"Gemini API call failed: {e}")

    # 3. Fallback: Return structured section summary directly
    fallback_text = (
        "### ⚖️ বাংলাদেশ আইন ভাণ্ডার থেকে প্রাপ্ত প্রাসঙ্গিক ধারাসমূহ:\n\n" +
        "\n\n".join([f"**{c['act_title']} ({c['act_year']})** - *{c['section_title']}*\n{c['preview']}" for c in citations]) +
        "\n\n> *পরামর্শ:* তাৎক্ষণিক পরামর্শের জন্য সরকারি আইনি সহায়তা টোল-ফ্রি **১৬৬৯৯** অথবা জরুরি সেবা **৯৯৯** এ যোগাযোগ করুন।"
    )
    return fallback_text, "Direct Statute Engine", citations

def _call_groq(user_prompt: str, history: Optional[List[Dict[str, str]]] = None) -> Optional[str]:
    url = "https://api.groq.com/openai/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {GROQ_API_KEY.strip()}",
        "Content-Type": "application/json"
    }

    messages = [{"role": "system", "content": SYSTEM_INSTRUCTION}]
    if history:
        for msg in history[-4:]:
            role = "user" if msg.get("role") in ["user", "USER"] else "assistant"
            messages.append({"role": role, "content": msg.get("content", "")})
    messages.append({"role": "user", "content": user_prompt})

    payload = {
        "model": DEFAULT_GROQ_MODEL,
        "messages": messages,
        "temperature": 0.3,
        "max_tokens": 1500
    }

    resp = requests.post(url, headers=headers, json=payload, timeout=25)
    resp.raise_for_status()
    data = resp.json()
    return data["choices"][0]["message"]["content"]

def _call_gemini(user_prompt: str, history: Optional[List[Dict[str, str]]] = None) -> Optional[str]:
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{DEFAULT_GEMINI_MODEL}:generateContent?key={GEMINI_API_KEY.strip()}"
    headers = {"Content-Type": "application/json"}

    contents = []
    if history:
        for msg in history[-4:]:
            role = "user" if msg.get("role") in ["user", "USER"] else "model"
            contents.append({
                "role": role,
                "parts": [{"text": msg.get("content", "")}]
            })

    contents.append({
        "role": "user",
        "parts": [{"text": user_prompt}]
    })

    payload = {
        "systemInstruction": {
            "parts": [{"text": SYSTEM_INSTRUCTION}]
        },
        "contents": contents,
        "generationConfig": {
            "temperature": 0.3,
            "maxOutputTokens": 1500
        }
    }

    resp = requests.post(url, headers=headers, json=payload, timeout=25)
    resp.raise_for_status()
    data = resp.json()
    candidates = data.get("candidates", [])
    if candidates and "content" in candidates[0]:
        parts = candidates[0]["content"].get("parts", [])
        if parts and "text" in parts[0]:
            return parts[0]["text"]
    return None
