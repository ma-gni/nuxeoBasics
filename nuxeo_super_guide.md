# Nuxeo Super-Guide — From Concepts to Hands‑On (MD)

> **Scope:** Everything we covered so far: Tomcat/Servlets, Nuxeo runtime & OSGi, Content Model (schemas, facets, doc types, vocabularies), Layouts, NXQL, Lifecycles, Tabs, development workflow (Studio, dev mode, hot reload), architecture (blobs, metadata, audit, indexes, directories, stream), and common troubleshooting.

---

## 0) Quick Map (You Are Here)

- **Web tier:** Browser/API → **Tomcat (servlet container)** → Nuxeo servlets/JAX‑RS.  
- **App tier:** Nuxeo **Services** (Automation, Workflow, UserManager, etc.).  
- **Repo tier:** **CoreSession** → **Repository (VCS)** → DB (metadata) + **Binary Store** (files).  
- **Search tier:** **Elasticsearch** index (fast queries, aggregates).  
- **Infra async:** **Stream** (Kafka‑like) for background work (indexing, thumbnails…).  

```
Client → Tomcat (Servlet Container)
          └─> Nuxeo JAX-RS / Automation endpoints (Controller)
                └─> Services (Business logic)
                      └─> CoreSession (Repository API)
                            ├─> DB (Metadata)
                            ├─> Binary Store (Blobs)
                            └─> Stream (Async processing)
```

---

## 1) Tomcat, Servlets & Why Nuxeo Runs “on top of” Tomcat

**Servlet:** a Java class that receives HTTP requests and returns responses (`doGet`, `doPost`).  
**Servlet container (Tomcat):** the runtime that **loads**, **maps URLs**, **manages** servlet lifecycle and **sessions**.

- Tomcat listens on `:8080` and hosts the Nuxeo webapp at `/nuxeo`.
- Nuxeo uses **JAX‑RS** (REST resources) and Automation operations sitting on top of servlets.
- Think: **Servlet = waiter**; **Service/Repository = kitchen**; **Tomcat = restaurant**.

**Why “built on Tomcat”**: Tomcat provides the HTTP runtime; Nuxeo contributes the application (bundles, endpoints, UI).

---

## 2) OSGi Runtime & Bundles (How Nuxeo Stays Modular)

- **OSGi** = plugin system for Java.  
- **Bundle** = a JAR with metadata (MANIFEST) that declares what it **exports** and **requires**.
- Nuxeo is a **set of bundles** (repository, workflow, DAM, REST…). Your Studio project becomes **your bundle** deployed into `nxserver/bundles`.
- **Extension points** = places where bundles contribute XML config (schemas, types, directories, lifecycles, actions…).

Analogy: **LEGO**. Each feature is a brick (bundle). The runtime snaps them together dynamically.

---

## 3) Content Model — The Big Three: Document Types, Schemas, Facets

### 3.1 Document Types
A **document type** is a blueprint (like a class): defines what a document **is**, where it **lives**, what it can **contain**, and how it **behaves**.

- **Extends**: reuse an existing type (e.g., `Contract` **extends** `File`).
- **Container types**: allowed **parents** (where instances can be created).
- **Accepted children types**: allowed **children** (what it can contain).
- **Lifecycle**: the state machine attached to this type.
- **Facets**: mix‑ins to add behavior (versioning, comments, folderish…).

### 3.2 Schemas (Metadata Definitions)
- **Schema** = a group of fields (metadata). Stored as **XSD** behind the scenes.
- Types attach one or more schemas (built‑in like `dublincore`, and custom like `contract`).
- **Field kinds**: string, integer, float, boolean, date, **complex**, **directory** (links to a vocabulary).

### 3.3 Facets (Behavior + Metadata Mix‑ins)
- **Folderish** → can contain children.
- **Versionable** → check‑in/versions.
- **Commentable** → comments.
- **Publishable**, **Thumbnail**, **Picture**, etc.
- Add a facet to instantly inherit behavior and sometimes a schema.

**Contract/Portfolio example recap**
- `Contract` extends `File`, facets: Versionable/Publishable/Commentable, schema `contract` (fields: `product`, `amount`, `price`, `deliveryDate`, etc.).
- `Portfolio` extends `Folder` (gets **Folderish**), container types: `Workspace`, accepted children: `Contract`; schema adds `customer` (complex) + `totalPrice`.

---

## 4) Vocabularies & Directories (Controlled Lists)

- A **Vocabulary** (Studio term) is stored as a **Directory** (runtime term) → usually a **SQL table**.
- Each entry has **id** (stored value), **label** (display), plus flags (obsolete) and order.
- Field of **type = directory** points to a directory/vocabulary by **directory name**.

**Kinds:** Simple, Child, Hierarchical (2 levels).  
**Creation policy:** how the backing table is created on startup (dev vs prod).

**Examples**
- `voc_season`: spring/summer/fall/winter.  
- `voc_state`: pending_approval/rejected/validated.  
- Hierarchical `products`: PARENT **COMPUTER** → CHILD **Laptop**/**Desktop**/…

**UI widget:** `nuxeo-directory-suggestion` displays labels, supports search & single/multi‑select.

---

## 5) Layouts (How Metadata Appears in the UI)

**Schemas define fields**; **layouts decide how & where** they show to users:
- **Create**: form shown at creation.
- **Edit**: edit form.
- **View**: read‑only main screen.
- **Metadata**: additional grouped fields.
- **Import**: used by bulk/drag‑drop.

Widgets adapt to field type (text/number/date/directory picker/complex editor).  
If a field isn’t in any layout, users won’t see or edit it (but it still exists).

**Tip:** Keep Create minimal, show more in View/Metadata. Use directory widgets for vocab fields.

---

## 6) NXQL, Page Providers & Content Views (Search)

**NXQL** = SELECT‑only query language over documents.  
Common predicates: `ecm:primaryType`, `ecm:currentLifeCycleState`, `ecm:parentId`, `dc:created`, any schema field (`contract:price`).

Examples:
```sql
SELECT * FROM Contract
SELECT * FROM Document WHERE contract:amount > 1000
SELECT * FROM Document WHERE ecm:parentId = 'UUID'
SELECT * FROM Document WHERE ecm:currentLifeCycleState = 'approved'
SELECT * FROM Document WHERE dc:created BETWEEN DATE '2025-09-01' AND DATE '2025-09-30'
```

**Page provider** = reusable search definition (query + params + sort + page size).  
**Content view** = UI layer around a page provider (form layout + results layout).  
Often backed by **Elasticsearch** for speed & aggregates.

---

## 7) Lifecycles (State Machines for Docs)

- **States** (e.g., `draft`, `negotiating`, `approved`) and **Transitions** (`to-negotiating`, `to-approved`…).
- Attach a lifecycle policy to a type; the **initial state** applies at creation.
- UI shows available transitions as **actions**; core enforces valid moves.
- Query with `ecm:currentLifeCycleState` in NXQL.

**Example policy: `LC_Contracts`**
```
States: draft (initial) → negotiating → approved
Transitions:
  draft → negotiating   (to-negotiating)
  negotiating → draft   (to-draft)
  negotiating → approved (to-approved)
```
**Bonus:** Add context actions that call **Follow Life Cycle Transition** via Automation chains.

---

## 8) Tabs (Custom Screens per Type/Context)

- Create **tabs** that appear only on certain types/states.  
- Inside a tab you can place:
  - **Content views** (lists with filters/results),
  - **Toggleable layout** (view⇄edit in place),
  - **Custom actions** (buttons running automation).
- Use **Activation** rules (e.g., only on `BookLibrary` or `Book`). Set **Order** to sort among built-ins.

---

## 9) Architecture Deep Dive

### 9.1 Blobs (Files)
- Actual file content (PDF/JPG/MP4) stored in **binary store** (filesystem, S3…).
- DB holds pointers (digest/hash) + blob metadata (size, mime type).

### 9.2 Metadata (Schemas → SQL)
- Your fields (string/int/date/directory/complex) are persisted in relational tables by VCS mapping.
- Complex fields often stored in auxiliary tables.

### 9.3 Audit Trails
- Events (create/update/transition/delete) stored in **audit** tables.
- Drive history UI, compliance, triggers for automation.

### 9.4 Indexes (Elasticsearch)
- Fast search/full‑text/facets/aggregations.
- Near‑real‑time (async refresh), not a source of record (DB is).

### 9.5 Directories (Lookup Tables)
- Vocabularies = directory tables; managed in Admin Center; referenced by **directory fields**.

### 9.6 Stream (Async, Kafka‑like)
- Log‑based stream used for background tasks (indexing, renditions, listeners).

### 9.7 Additional Persistent Data
- Users/groups (if not external), workflow models, automation chains, package state, configuration.

### 9.8 Non‑Persistent Data
- Sessions, caches, in‑memory queues; transient runtime info.

---

## 10) Development Workflow: Studio, Dev Mode, Hot Reload

- **Studio Modeler**: content model (types, schemas, directories, lifecycles, page providers, actions…).  
- **Studio Designer**: layouts, tabs, widgets, theming.

**Commit** in Studio saves to a Git project on Nuxeo Connect.  
**Hot reload** pulls it into your running server (no full restart) — **requires dev mode**.

### Enable Dev Mode
Add to your active config (often `bin/nuxeo.conf` in your distro):
```properties
org.nuxeo.dev=true
```
Then restart:
```bash
./bin/nuxeoctl restart
```

**Where is `nuxeo.conf`?**  
- ZIP/Tomcat distro: often **`<NUXEO_HOME>/bin/nuxeo.conf`**.  
- If missing: create it there; Nuxeo merges it over defaults.  
- `showconf` prints the path actually used.

**Common commands**
```bash
./bin/nuxeoctl showconf      # print effective config
./bin/nuxeoctl configure     # (re)generate from templates
./bin/nuxeoctl console       # start with live logs (Ctrl+C stops)
./bin/nuxeoctl start|stop|restart
```

**Hotfixes**  
- `mp-hotfix` requires a **Nuxeo Online Services** registration (`nuxeoctl register`).  
- For local dev, you can often proceed without hotfixes at first.

---

## 11) Troubleshooting Cookbook (Real Issues You Hit)

### 11.1 Port conflict (:8080)
Error: `Address already in use: 127.0.0.1:8080`  
Fix: find & kill the process or change port.
```bash
# find user of 8080
ss -ltnp | grep :8080
# change port in config
nuxeo.server.http.port=8081
```

### 11.2 “Starting process is taking too long – giving up”
- Use `console` to see logs; increase wait or move install out of `/mnt/c` if using WSL2.

### 11.3 CORS null pointer in Dev panel
`Cannot invoke ... "this.corsFilters" is null`  
- Either define a CORS filter or disable CORS for dev:
```properties
nuxeo.cors.enabled=false
```

### 11.4 KafkaChecker ClassNotFound (before OSGi)
Launcher tries to load `org.nuxeo.runtime.kafka.KafkaChecker`.
Disable backing-service checks for dev:
```properties
nuxeo.backing.check=false
nuxeo.backing.check.kafka=false
nuxeo.kafka.enabled=false
nuxeo.stream.kafka.enabled=false
nuxeo.stream.processing.enabled=false
```
If stubborn, also append as JVM options (in the same config):
```properties
nuxeo.server.jvm.options=${nuxeo.server.jvm.options} -Dnuxeo.backing.check=false -Dnuxeo.backing.check.kafka=false -Dnuxeo.kafka.enabled=false -Dnuxeo.stream.kafka.enabled=false -Dnuxeo.stream.processing.enabled=false
```

### 11.5 Moved install; now “Failed to restore conf/server.xml from .bak”
- Delete stale template cache and regenerate:
```bash
rm -f templates/files.list conf/server.xml.bak
./bin/nuxeoctl configure
```

### 11.6 Media helpers missing (WARN, not fatal)
`exiftool`, `ffmpeg`, `pdftotext` not found → only affects media processing; install and add to PATH later.

### 11.7 Slow startup on WSL2
- Prefer placing install under Linux home (`~/...`), not `/mnt/c/...`.
- Exclude folder from Windows Defender.
- Use **Hot Reload** for Studio changes (avoid full restarts).

---

## 12) Security & API Notes (Quick Orientation)

- Auth methods: Basic, OAuth2, **Nuxeo API tokens** (recommended for scripts/clients).
- Permissions (ACLs) can be tied to lifecycle state (e.g., approved = read‑only except Managers).
- REST samples:
```bash
# GET a document by id
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/nuxeo/api/v1/id/<UUID>

# Update metadata (partial)
curl -X PUT -H "Content-Type: application/json+nxentity" \
     -H "Authorization: Bearer <TOKEN>" \
     -d '{"entity-type":"document","uid":"<UUID>","properties":{"contract:price":1200}}' \
     http://localhost:8080/nuxeo/api/v1/id/<UUID>
```

---

## 13) Exercises Recap (What You Built)

- **Contract** (extends File): custom schema (`product`, `amount`, `price`, `deliveryDate`), facets (Versionable/Publishable/Commentable), lifecycle (`LC_Contracts`).  
- **Portfolio** (extends Folder): container `Workspace/Folder`, accepted child `Contract`, schema (`customer.company`, `customer.location`, `totalPrice`).  
- **Vocabularies**: `voc_season`, `voc_state`, hierarchical `products`; wired via **directory** fields + `nuxeo-directory-suggestion`.  
- **Layouts**: create/edit/view/metadata for Contract & Portfolio.  
- **Search**: NXQL basics; page providers + content views; ES-backed.  
- **Tabs**: custom tabs with content views filtered by lifecycle/category.  
- **Dev flow**: Studio commit → Hot reload (with `org.nuxeo.dev=true`).  
- **Troubleshooting**: CORS NPE, Kafka checker, port conflicts, template cache cleanup, WSL tips.

---

## 14) Cheat Sheets

### 14.1 NXQL
```sql
SELECT * FROM Document WHERE ecm:primaryType = 'Contract'
SELECT * FROM Document WHERE ecm:currentLifeCycleState = 'approved'
SELECT * FROM Document WHERE contract:price >= 1000
SELECT * FROM Document WHERE ecm:parentId = 'UUID'
SELECT * FROM Document WHERE dc:created BETWEEN DATE '2025-09-01' AND DATE '2025-09-30'
```

### 14.2 Lifecycle attach
- Modeler → Lifecycles → New (`LC_Contracts`)
- States: draft* → negotiating → approved
- Transitions: to-negotiating, to-draft, to-approved
- Attach to **Contract** in General tab.

### 14.3 Directory field
- Field type: **Directory**
- Directory name: your vocab ID (e.g., `voc_state`, `products`)
- Widget: `nuxeo-directory-suggestion`

### 14.4 Dev toggles
```properties
# dev mode
org.nuxeo.dev=true

# change HTTP port
nuxeo.server.http.port=8081

# disable CORS quickly for dev
nuxeo.cors.enabled=false
```

---

## 15) Visual Memory (Text Diagrams)

### Request Lifecycle
```
Browser/API → Tomcat (servlet container)
  → Nuxeo JAX-RS / Automation (Controller)
    → Services (business logic)
      → CoreSession (repo API)
        → DB (metadata) + Blobs + Stream
  ← HTTP Response (JSON/HTML)
```

### Content Model
```
Document Type (extends File/Folder/...)
  ├─ Schemas (your metadata fields)
  ├─ Facets (behaviors)
  ├─ Lifecycle (states/transitions)
  ├─ Container types / Accepted children
  └─ Layouts (Create/Edit/View/Metadata/Import) → UI
```

### Storage
```
- DB: metadata tables (VCS mapping), directories (vocab tables), audit
- Binary store: file blobs
- Elasticsearch: indexes for search
- Stream: async pipelines
```

---

## 16) Next Steps (Pick Your Path)

- **Automation:** add listeners/chains (e.g., block Approve unless `validation:validator` set).  
- **Search UX:** a dashboard content view with filters (state, validator, date range).  
- **Permissions:** read‑only when `approved`, editable in earlier states.  
- **REST Client:** build a small script to create/list Contracts via token.  
- **Layouts polish:** badges, tabs, toggleable layouts, icons.

> Tell me which track you want next; I’ll give you copy‑paste configs or starter code.
