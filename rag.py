import pandas as pd
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.schema import Document
from sentence_transformers import SentenceTransformer
import faiss
import numpy as np
import os
import requests
import random

#ollama serve

print("Loading CSV...")
df = pd.read_csv("CNN_Articels_clean.csv")


text_field = "Article text"
meta_fields = ["Headline"]


print("Converting rows into LangChain Documents...")
documents = []
for _, row in df.iterrows():
    text = str(row[text_field])
    metadata = {field: str(row[field]) for field in meta_fields}
    documents.append(Document(page_content=text, metadata=metadata))


print("Chunking documents using RecursiveCharacterTextSplitter...")
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,      
    chunk_overlap=100,    
    separators=["\n\n", "\n", ".", "!", "?", ",", " "] 
)


chunked_documents = text_splitter.split_documents(documents)

print(f"Total documents: {len(documents)}")
print(f"Total chunks: {len(chunked_documents)}")


print("Initializing Sentence Transformer model...")
model = SentenceTransformer('all-MiniLM-L6-v2')

embedding_file = "embeddings.npy"
if os.path.exists(embedding_file):

    embeddings = np.load(embedding_file)
    print("Embeddings loaded from 'embeddings.npy'.")
else:
    print("Generating embeddings for each chunk...")
    embeddings = []
    for i, doc in enumerate(chunked_documents):
        embedding = model.encode(doc.page_content)  
        embeddings.append(embedding)
        if i % 100 == 0:  
            print(f"Processed {i+1}/{len(chunked_documents)} chunks...")

    embeddings = np.array(embeddings).astype('float32')

    # Save embeddings to a .npy file
    np.save(embedding_file, embeddings)
    print("Embeddings saved to 'embeddings.npy'.")


print("Initializing FAISS index...")
dimension = embeddings.shape[1]  # This is the size of each embedding vector
index = faiss.IndexFlatL2(dimension)  # Use L2 (Euclidean) distance
print("Adding embeddings to FAISS index...")
index.add(embeddings) 


while True:
    query = input("Enter your query: ")
    query_embedding = model.encode(query).astype('float32')


    D, I = index.search(np.array([query_embedding]), k=5)  # Top 5 nearest neighbors

    #Display the results
    print("\nTop 5 nearest neighbors:")
    for i in range(5):
        print(f"\n--- Result {i+1} ---")
        print("Document Metadata:", chunked_documents[I[0][i]].metadata)
        print("Content:", chunked_documents[I[0][i]].page_content[:200], "...")



    retrieved_chunks = [chunked_documents[i].page_content for i in I[0]]
    context = "\n\n".join(retrieved_chunks)


    prompt_templates = [
        """
        You are an AI assistant. Answer the following question: "{query}", 
        based only on the following context:\n\n{context}
        """,
        
        """
        Based on the following context, provide a response to the query: "{query}".
        Context:\n\n{context}
        """,
        
        """
        You have the following information. Answer the question: "{query}" using only the context below:\n\n{context}
        """,
        
        """
        Please respond to the following query: "{query}" by referring to the given context:\n\n{context}
        """,
        
        """
        Given the context below, provide the best possible response to the query: "{query}".\n\n{context}
        """,
        
        """
        Answer the question: "{query}", using only the context provided below:\n\n{context}
        """
    ]

    # Randomly select a prompt template
    selected_template = random.choice(prompt_templates)

    # Extract structure parts (before {query}, between {query} and {context})
    before_query, after_query = selected_template.split('{query}')
    between_query_context, _ = after_query.split('{context}')



    formatted_prompt = selected_template.format(query=query, context=context)

    # Function to send prompt to Ollama
    def query_ollama(prompt, model="llama3.2:1b"):
        response = requests.post(
            "http://localhost:11434/api/generate",
            json={
                "model": model,
                "prompt": prompt,
                "stream": False
            }
        )
        if response.status_code == 200:
            return response.json()["response"]
        else:
            print("Error:", response.status_code, response.text)
            return None

    print("\nQuerying Ollama...without context: ",query)
    response = query_ollama(query)
    print("\n💬 LLM Response:\n", response)
    # Query Ollama with formatted prompt
    print("\nQuerying Ollama...")
    print(before_query.strip(),"{query}" ,between_query_context.strip() , "{context}")
    response = query_ollama(formatted_prompt)
    print("\n💬 LLM Response:\n", response)


    #Why CNN is launching a new series on skin whitening?
    # why is there a shortage of truckers ?
    # why greece has built a wall on border with turkey ? 
    # which are the fastest growing economies ?
    # were there any data breach from big companies ?