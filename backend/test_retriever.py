import sys
from pathlib import Path

# Fix Windows console UTF-8 output
sys.stdout.reconfigure(encoding='utf-8')

# Add backend directory to path
sys.path.insert(0, str(Path(__file__).resolve().parent))

from app.retriever import LegalRetriever

def main():
    print("Testing LegalRetriever...")
    retriever = LegalRetriever.get_instance()
    print(f"Total indexed sections: {len(retriever.sections)}")

    test_queries = [
        "খুন করার শাস্তি কী ধারা ৩০২",
        "রাস্তায় হেলমেট ছাড়া মোটরসাইকেল চালালে কত টাকা জরিমানা",
        "যৌতুকের দাবি করলে শাস্তি",
        "বাড়িভাড়া অগ্রিম জামানত কত মাসের বেশি নেওয়া যাবে না"
    ]

    for q in test_queries:
        print("\n" + "=" * 60)
        print(f"Query: {q}")
        print("-" * 60)
        results = retriever.search(q, top_k=2)
        if not results:
            print("No results found.")
        for i, res in enumerate(results, 1):
            print(f"[{i}] Act: {res['act_title']} ({res['act_year']})")
            print(f"    Section: {res['section_title']}")
            print(f"    Score: {res['score']}")
            preview = res['section_content'].replace('\n', ' ')[:150]
            print(f"    Content: {preview}...")

if __name__ == "__main__":
    main()
