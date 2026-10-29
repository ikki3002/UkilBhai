import sys
from pathlib import Path

# Fix Windows console UTF-8 output
sys.stdout.reconfigure(encoding='utf-8')

# Add backend directory to path
sys.path.insert(0, str(Path(__file__).resolve().parent))

from app.retriever import LegalRetriever
from app.generator import generate_legal_advice

def main():
    print("Testing End-to-End RAG Advice Generation...")
    retriever = LegalRetriever.get_instance()

    query = "বাড়িওয়ালা কি অগ্রিম ৩ মাসের বেশি জামানত বা ভাড়া দাবি করতে পারে? আইনে কী বলা আছে?"
    print(f"\nUser Query: {query}")
    print("Retrieving relevant statutes...")

    sections = retriever.search(query, top_k=3)
    print(f"Retrieved {len(sections)} sections:")
    for s in sections:
        print(f" - {s['act_title']} ({s['act_year']}) | {s['section_title']} (Score: {s['score']})")

    print("\nGenerating AI Legal Advice with Groq/Gemini...")
    answer, provider, citations = generate_legal_advice(query, sections)

    print("\n" + "=" * 60)
    print(f"AI Provider Used: {provider}")
    print("=" * 60)
    print(answer)
    print("=" * 60)
    print(f"Citations attached: {len(citations)}")
    for c in citations:
        print(f" - [{c['act_title']} ({c['act_year']})] {c['section_title']}")

if __name__ == "__main__":
    main()
