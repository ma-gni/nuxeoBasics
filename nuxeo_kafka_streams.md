# Nuxeo + Kafka (Streams) Deep Dive

This document explains how **Nuxeo uses Kafka (via nuxeo-stream)** to process events, index documents, and handle asynchronous workloads.

---

## 1. What triggers a Kafka message?
- Document created / updated / deleted
- Lifecycle transition (e.g., draft → approved)
- Blob uploaded (thumbnails, renditions)
- Audit entry recording
- Bulk actions (reindex, enrichment, video processing)

➡️ A **core event** in Nuxeo → producer appends a payload to a **Kafka topic**.

**Example payload:**
```json
{
  "type": "DOCUMENT_UPDATED",
  "docId": "c5c6-...-a81f",
  "repo": "default",
  "modifiedProps": ["dc:title","contract:price"],
  "time": "2025-09-19T10:04:22Z",
  "user": "ilias"
}
```

---

## 2. Topics
Nuxeo defines topics for different async jobs:
- `nuxeo-elasticsearch-indexing` → index docs into Elasticsearch
- `nuxeo-audit` → log events for the audit trail
- `nuxeo-renditions` → create thumbnails / renditions
- `nuxeo-bulk` → handle large async jobs

---

## 3. Stream Processors (Consumers)
- Each processor subscribes to a **topic**
- Runs in a **consumer group**
- Moves an **offset** forward when successful

Examples:
- **ES Indexer** → consumes `DOCUMENT_*` events, updates Elasticsearch
- **Audit Replicator** → persists `LogEntry` into DB + ES
- **Rendition Worker** → generates thumbnails on blob upload

---

## 4. Delivery Model
- **At-least-once** delivery → events may be retried
- Make consumers **idempotent**
  - ES upserts by docId/version → safe
  - Thumbnails by blob digest → skip if exists
- **Ordering per partition** → use `docId` as key to keep doc events ordered

---

## 5. Why Kafka?
- Don’t block user actions with heavy tasks
- Scale out by adding more consumers
- Resilient: recover from crashes via offsets
- Replayable: reindex all docs or regenerate thumbnails

---

## 6. Config (common properties)
Enable Kafka + streams:
```
nuxeo.kafka.enabled=true
nuxeo.stream.kafka.enabled=true
nuxeo.stream.processing.enabled=true
```

Disable checks (dev-only):
```
nuxeo.backing.check=false
nuxeo.backing.check.kafka=false
```

---

## 7. Example Flow: Contract Edited
```
User edits “Contract #42” → Save
  ↓
Core Event: documentModified
  ↓
Producer → send message to `nuxeo-elasticsearch-indexing`
  ↓
Consumer A: ES Indexer → upserts into Elasticsearch
Consumer B: Audit Replicator → writes LogEntry
  ↓
Search results & audit logs reflect the update
```

---

## 8. Ops Cheat-Sheet
- **Disable Kafka** (dev mode): `nuxeo.stream.processing.enabled=false`
- **Lag** (backlog grows): add partitions/consumers, batch ES writes
- **Out-of-order issues**: partition by `docId`
- **Reindex repository**: bulk reindexer → produces events into Kafka

---

## 9. Mapping to Spring Kafka (for memory)
- `KafkaTemplate.send(topic, payload)` ↔ Nuxeo Producer
- `@KafkaListener(topics="...")` ↔ Nuxeo Stream Processor
- `groupId` ↔ Processor Group
- Offsets & retries ↔ Same semantics

---

✅ **Summary:**  
Nuxeo relies on **Kafka topics** to handle async workloads (indexing, audit, renditions, bulk jobs).  
This makes the system scalable, resilient, and near-real-time without blocking the user.
