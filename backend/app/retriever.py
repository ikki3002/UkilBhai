import re
import json
import logging
import unicodedata
import urllib.request
from typing import List, Dict, Any, Optional
from pathlib import Path
from rank_bm25 import BM25Okapi
from app.config import DATASET_URL, JSONL_FILE

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("LegalRetriever")

BN_DIGITS = str.maketrans("০১২৩৪৫৬৭৮৯", "0123456789")
BN_SUFFIXES = ['ের', 'দের', 'কে', 'তে', 'র', 'টি', 'গুলো', 'সমূহ', 'গণের', 'কারী', 'ওয়ালা', 'ওয়ালা']

BN_STOPWORDS = {
    'কি', 'কী', 'বা', 'করতে', 'পারে', 'বলা', 'আছে', 'হলে', 'যদি', 'এই', 'সেই',
    'কোন', 'কোনো', 'কেন', 'কিভাবে', 'থেকে', 'জন্য', 'ও', 'এবং', 'করা', 'হবে',
    'না', 'হতে', 'দিয়ে', 'নিয়ে', 'পর', 'হলে', 'করে', 'দিতে', 'বলা', 'যায়', 'যায়'
}

def normalize_bengali_orthography(text: str) -> str:
    """Normalize Unicode NFC, Bengali vowels (ী->ি, ূ->ু), and decomposed characters."""
    if not text:
        return ""
    nfc = unicodedata.normalize('NFC', text)
    # Normalize vowel markers for search invariance
    return nfc.replace('ী', 'ি').replace('ূ', 'ু')

def strip_bn_suffix(word: str) -> str:
    """Strip common Bengali inflectional suffixes (e.g. যৌতুকের -> যৌতুক)."""
    for s in sorted(BN_SUFFIXES, key=len, reverse=True):
        if word.endswith(s) and len(word) - len(s) >= 3:
            return word[:-len(s)]
    return word

# Legal bilingual synonym expansion to bridge Bengali queries with English statute books
LEGAL_SYNONYMS: Dict[str, List[str]] = {
    "খুন": ["murder", "penal code", "death"],
    "হত্যা": ["murder", "penal code", "death"],
    "মার্ডার": ["murder", "penal code"],
    "চুরি": ["theft", "dishonestly", "stolen"],
    "ডাকাতি": ["dacoity", "robbery"],
    "ছিনতাই": ["extortion", "robbery"],
    "ধর্ষণ": ["rape", "নারী ও শিশু নির্যাতন"],
    "মারধর": ["hurt", "grievous hurt", "assault"],
    "জখম": ["hurt", "grievous hurt"],
    "প্রতারণা": ["cheating", "fraud", "deceive"],
    "জালিয়াতি": ["forgery", "fraudulent"],
    "মানহানি": ["defamation", "reputation"],
    "হুমকি": ["criminal intimidation", "threat"],
    "দণ্ডবিধি": ["penal code"],
    "পেনাল": ["penal code"],
    "হেলমেট": ["helmet", "সড়ক পরিবহন", "motor vehicle"],
    "মোটরসাইকেল": ["motorcycle", "সড়ক পরিবহন", "যানবাহন"],
    "বাইক": ["motorcycle", "সড়ক পরিবহন"],
    "লাইসেন্স": ["driving licence", "licence", "সড়ক পরিবহন"],
    "ভাড়া": ["rent", "বাড়ী ভাড়া", "বাড়ি ভাড়া", "premises rent", "ভাড়া নিয়ন্ত্রণ"],
    "ভাড়াটিয়া": ["tenant", "বাড়ী ভাড়া", "বাড়ি ভাড়া", "rent"],
    "বাড়িওয়ালা": ["landlord", "বাড়ী ভাড়া", "বাড়ি ভাড়া", "rent"],
    "জামানত": ["বাড়ী ভাড়া", "security deposit", "salamee", "অগ্রিম"],
    "যৌতুক": ["dowry", "যৌতুক নিরোধ", "নারী ও শিশু"],
    "তালাক": ["divorce", "dissolution of marriage"],
    "ডিভোর্স": ["divorce", "dissolution of marriage"],
    "খোরপোশ": ["maintenance", "দাম্পত্য"],
    "চেক": ["cheque", "dishonour", "negotiable instruments"],
    "বাউন্স": ["dishonour of cheque", "negotiable instruments"]
}

def tokenize(text: str, expand_synonyms: bool = False, filter_stopwords: bool = False) -> List[str]:
    """Tokenize text with orthographic normalization, suffix stripping, and legal synonyms."""
    if not text:
        return []

    norm_text = normalize_bengali_orthography(text.lower())
    digits_normalized = norm_text.translate(BN_DIGITS)

    raw_tokens = re.findall(r'[\u0980-\u09ffa-zA-Z0-9]+', norm_text)
    norm_tokens = re.findall(r'[\u0980-\u09ffa-zA-Z0-9]+', digits_normalized)

    all_tokens = []
    for t in raw_tokens + norm_tokens:
        if filter_stopwords and t in BN_STOPWORDS:
            continue
        all_tokens.append(t)
        # Suffix-stripped stem for Bengali words
        stem = strip_bn_suffix(t)
        if stem != t and (not filter_stopwords or stem not in BN_STOPWORDS):
            all_tokens.append(stem)

    combined = list(dict.fromkeys(all_tokens))

    if expand_synonyms:
        expanded = list(combined)
        for t in combined:
            # Check direct or suffix-stripped word in synonyms
            stem = strip_bn_suffix(t)
            match_key = t if t in LEGAL_SYNONYMS else (stem if stem in LEGAL_SYNONYMS else None)
            if match_key:
                for syn in LEGAL_SYNONYMS[match_key]:
                    syn_norm = normalize_bengali_orthography(syn.lower())
                    syn_tokens = re.findall(r'[\u0980-\u09ffa-zA-Z0-9]+', syn_norm)
                    expanded.extend(syn_tokens)
        return list(dict.fromkeys(expanded))

    return combined

class LegalRetriever:
    _instance: Optional["LegalRetriever"] = None

    def __init__(self):
        self.sections: List[Dict[str, Any]] = []
        self.bm25: Optional[BM25Okapi] = None
        self._ensure_dataset()
        self._build_index()

    @classmethod
    def get_instance(cls) -> "LegalRetriever":
        if cls._instance is None:
            cls._instance = LegalRetriever()
        return cls._instance

    def _ensure_dataset(self):
        """Download bangladesh_legal_acts.jsonl if not locally cached."""
        if not JSONL_FILE.exists() or JSONL_FILE.stat().st_size < 1000000:
            logger.info(f"Downloading dataset from {DATASET_URL}...")
            JSONL_FILE.parent.mkdir(parents=True, exist_ok=True)
            req = urllib.request.Request(DATASET_URL, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req) as resp, open(JSONL_FILE, "wb") as out_f:
                total_bytes = 0
                while chunk := resp.read(1024 * 512):
                    out_f.write(chunk)
                    total_bytes += len(chunk)
                    if total_bytes % (5 * 1024 * 1024) == 0:
                        logger.info(f"Downloaded {total_bytes // (1024 * 1024)} MB...")
            logger.info(f"Dataset downloaded: {JSONL_FILE.stat().st_size / (1024 * 1024):.2f} MB")
        else:
            logger.info(f"Using cached dataset at {JSONL_FILE}")

    def _build_index(self):
        """Parse non-repealed acts into sections and construct the BM25 index."""
        logger.info("Loading acts and building section corpus...")
        tokenized_corpus: List[List[str]] = []
        loaded_acts = 0
        skipped_repealed = 0

        with open(JSONL_FILE, "r", encoding="utf-8") as f:
            for line in f:
                if not line.strip():
                    continue
                try:
                    act = json.loads(line)
                except Exception:
                    continue

                if act.get("is_repealed", False):
                    skipped_repealed += 1
                    continue

                act_title = act.get("act_title", "").strip()
                act_year = str(act.get("act_year", "")).strip()
                act_no = str(act.get("act_no", "")).strip()
                lang = act.get("language", "")

                for sec in act.get("sections", []):
                    sec_title = sec.get("section_title", "").strip()
                    sec_content = sec.get("section_content", "").strip()
                    if not sec_content and not sec_title:
                        continue

                    # Emphasize Act Title (4x weight) and Section Title (2x weight)
                    index_text = f"{act_title} {act_title} {act_title} {act_title} {act_year} {sec_title} {sec_title} {sec_content[:1500]}"
                    tokens = tokenize(index_text, expand_synonyms=False, filter_stopwords=False)
                    if tokens:
                        tokenized_corpus.append(tokens)
                        self.sections.append({
                            "act_title": act_title,
                            "act_year": act_year,
                            "act_no": act_no,
                            "language": lang,
                            "section_title": sec_title,
                            "section_content": sec_content,
                            "footnotes": sec.get("footnotes", [])
                        })

                loaded_acts += 1

        logger.info(f"Indexed {len(self.sections)} sections across {loaded_acts} active acts (Skipped {skipped_repealed} repealed acts).")
        logger.info("Building BM25 index...")
        self.bm25 = BM25Okapi(tokenized_corpus)
        logger.info("BM25 index built successfully.")

    def search(self, query: str, top_k: int = 5) -> List[Dict[str, Any]]:
        """Search top_k relevant sections for the given query."""
        if not self.bm25 or not self.sections:
            return []

        # Tokenize query with legal synonyms and filter conversational stop words
        query_tokens = tokenize(query, expand_synonyms=True, filter_stopwords=True)
        if not query_tokens:
            query_tokens = tokenize(query, expand_synonyms=True, filter_stopwords=False)
        if not query_tokens:
            return []

        scores = self.bm25.get_scores(query_tokens)
        top_indices = sorted(range(len(scores)), key=lambda i: scores[i], reverse=True)[:top_k]

        results = []
        for idx in top_indices:
            score = float(scores[idx])
            if score <= 0.0:
                continue
            sec = self.sections[idx]
            results.append({
                "act_title": sec["act_title"],
                "act_year": sec["act_year"],
                "act_no": sec["act_no"],
                "section_title": sec["section_title"],
                "section_content": sec["section_content"],
                "footnotes": sec["footnotes"],
                "score": round(score, 4)
            })

        return results
