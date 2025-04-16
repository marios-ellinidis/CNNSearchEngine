import pandas as pd
import numpy as np
from sentence_transformers import SentenceTransformer
from tqdm import tqdm
import os




base_dir = os.path.dirname(os.path.abspath(__file__))

csv_path = os.path.join(base_dir, 'CNN_Articels_clean.csv')
output_path = os.path.join(base_dir, 'article_embeddings.json')

model = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')

#csv_path = 'CNN_Articels_clean.csv'
df = pd.read_csv(csv_path)


df = df.dropna(subset=['Index', 'Headline', 'Description', 'Second headline', 'Article text'])


def embed_field(text):
    return model.encode(text if pd.notna(text) and isinstance(text, str) else "", normalize_embeddings=True)


embeddings_data = []


for idx, row in tqdm(df.iterrows(), total=len(df), desc="Embedding articles"):
    article_index = row['Index']
    author = str(row.get('Author', ''))
    section = str(row.get('Section', ''))
    category = str(row.get('Category', ''))
    headline = str(row.get('Headline', ''))
    description = str(row.get('Description', ''))
    keywords = str(row.get('Keywords', ''))
    second_headline = str(row.get('Second headline', ''))
    article_text = str(row.get('Article text', ''))

    headline_emb = embed_field(headline)
    description_emb = embed_field(description)
    second_headline_emb = embed_field(second_headline)
    article_text_emb = embed_field(article_text)

    author_emb = embed_field(author)
    keywords_emb = embed_field(keywords)
    section_emb = embed_field(section)
    category_emb = embed_field(category)

    embeddings_data.append({
        "Index": article_index,
        "HeadlineEmbedding": headline_emb.tolist(),
        "DescriptionEmbedding": description_emb.tolist(),
        "SecondHeadlineEmbedding": second_headline_emb.tolist(),
        "ArticleTextEmbedding": article_text_emb.tolist(),
        "AuthorEmbedding": author_emb.tolist(),
        "KeywordsEmbedding": keywords_emb.tolist(),
        "SectionEmbedding": section_emb.tolist(),
        "CategoryEmbedding": category_emb.tolist()
    })


output_df = pd.DataFrame(embeddings_data)
output_df.to_json(output_path, orient="records", lines=True)

print(f"Embeddings saved to {output_path}")
