#!/usr/bin/env python
import sys
import json
from sentence_transformers import SentenceTransformer

def main():
    
    model = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')

    if len(sys.argv) < 2:
        print(json.dumps({"error": "Missing query"}))
        sys.exit(1)
    query = sys.argv[1]

   
    emb = model.encode(query, normalize_embeddings=True).tolist()
    
    print(json.dumps({"embedding": emb}))

if __name__ == "__main__":
    main()
