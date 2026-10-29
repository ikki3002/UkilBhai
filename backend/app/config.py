import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "data"
DATA_DIR.mkdir(exist_ok=True)

DATASET_URL = os.getenv(
    "DATASET_URL",
    "https://huggingface.co/datasets/sakhadib/Bangladesh-Legal-Acts-Dataset/resolve/main/bangladesh_legal_acts.jsonl"
)
JSONL_FILE = DATA_DIR / "bangladesh_legal_acts.jsonl"
CACHE_FILE = DATA_DIR / "sections_cache.json"

def _load_local_properties() -> dict:
    """Safely load keys from local.properties if running locally (local.properties is git-ignored)."""
    props = {}
    local_props_file = BASE_DIR.parent / "local.properties"
    if local_props_file.exists():
        with open(local_props_file, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    props[k.strip()] = v.strip()
    return props

_local_props = _load_local_properties()

# API Keys (read from environment, with fallback to git-ignored local.properties)
GROQ_API_KEY = os.getenv("GROQ_API_KEY", _local_props.get("GROQ_API_KEY", ""))
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", _local_props.get("GEMINI_API_KEY", ""))
HUGGINGFACE_API_KEY = os.getenv("HUGGINGFACE_API_KEY", _local_props.get("HUGGINGFACE_API_KEY", ""))

# Default Model Selection
DEFAULT_GROQ_MODEL = os.getenv("GROQ_MODEL", "qwen/qwen3.8-27b")
DEFAULT_GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")

PORT = int(os.getenv("PORT", 7860))
HOST = os.getenv("HOST", "0.0.0.0")
