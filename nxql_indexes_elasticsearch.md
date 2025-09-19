# NXQL, Indexes, and Elasticsearch in Nuxeo

---

## 🔹 1. Blobs vs Metadata
- **Blob**: the actual binary file (PDF, image, video). Stored in a binary store (e.g., file system, S3).  
- **Metadata**: structured data *about* the file (title, author, contract price, validation state). Stored in the relational database (PostgreSQL, etc.).  

👉 Queries work on **metadata**, not directly on blobs.

---

## 🔹 2. What is NXQL?
- **NXQL (Nuxeo Query Language)** is similar to SQL but specialized for Nuxeo documents.  
- It allows filtering, sorting, and full-text searching over document metadata.  
- Examples:
  ```sql
  SELECT * FROM Document
  WHERE ecm:primaryType = 'Contract'
  ```

  ```sql
  SELECT * FROM Document
  WHERE validation:validator = 'user:ilias'
  ```

  ```sql
  SELECT * FROM Document
  WHERE ecm:fulltext = 'invoice'
  ```

---

## 🔹 3. What are Indexes?
- **Indexes** are search-optimized data structures.  
- Without indexes: the database would scan millions of rows → very slow.  
- With indexes: queries run in milliseconds.  

### Types of indexed fields in Nuxeo:
- Standard properties: `dc:title`, `dc:created`  
- Custom schema fields: `contract:price`, `contract:validator`  
- Full-text index: `ecm:fulltext` (includes text extracted from blobs)

---

## 🔹 4. Role of Elasticsearch
- Nuxeo integrates **Elasticsearch** as its indexing engine.  
- Every time a document changes:
  1. Metadata saved in DB.  
  2. Metadata pushed to Elasticsearch for indexing.  
- Queries hit Elasticsearch first for performance.  
- Elasticsearch stores:
  - Document metadata.  
  - Extracted text from blobs.  
  - Audit logs (who did what, when).  

👉 Elasticsearch = “Google Search” for your Nuxeo repository.

---

## 🔹 5. How They Work Together

### Step-by-step flow:
1. **User uploads a file** (e.g., Contract.pdf).  
2. Nuxeo stores the **blob** in the binary store.  
3. Nuxeo stores the **metadata** in the database.  
4. Nuxeo sends metadata (and extracted text) to **Elasticsearch** for indexing.  
5. When the user searches:
   - NXQL query → translated into Elasticsearch query.  
   - Elasticsearch returns document IDs.  
   - Database fetches final metadata + links to blobs.  
   - Response sent back as JSON.  

---

## 🔹 6. Why This Matters
- **Scalability**: Elasticsearch can handle millions of documents quickly.  
- **Flexibility**: NXQL allows SQL-like queries + full-text.  
- **Separation of concerns**:
  - Blob = storage.  
  - Metadata = database.  
  - Index = Elasticsearch.  

---

## ✅ Summary Table

| Component      | Role |
|----------------|------|
| Blob Store     | Stores the raw binary files (PDFs, images, etc.) |
| Database (SQL) | Stores structured metadata (title, author, price, etc.) |
| Elasticsearch  | Indexes metadata & text for fast queries |
| NXQL           | Query language to search documents via indexes |

---

👉 This is why Nuxeo is powerful: it separates storage concerns but gives you one unified query language (NXQL) backed by Elasticsearch for performance.
