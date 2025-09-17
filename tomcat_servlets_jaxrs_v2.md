# Tomcat Dispatching V2 — Classic Servlet vs JAX‑RS (Step‑by‑Step, With Nuxeo Context)

> **Why this V2?** You already nailed the big picture. This version goes deeper and **shows both approaches side‑by‑side** — how Tomcat routes, how each stack handles the request, what code you write, when to choose which, and how this maps to **Nuxeo**.

---

## 0) Glossary (super short)

- **URL**: full address, e.g. `http://localhost:8080/api/contracts/123?foo=bar`  
- **Request URI**: only the path (+ optional query), e.g. `/api/contracts/123?foo=bar`  
- **Path**: the part without query, e.g. `/api/contracts/123`  
- **Servlet**: Java class that handles HTTP via `service()` → `doGet/doPost/...`  
- **JAX‑RS**: annotation‑based REST API on top of servlets (`@Path`, `@GET`, …)  
- **Dispatcher**: the router; picks the handler for a request

---

## 1) Two Dispatch Layers (the key mental model)

```
Browser
  ↓ HTTP request (URL)
Tomcat (Servlet Container)
  ├─ Layer 1: Servlet Dispatcher  →  Which servlet gets this URL?
  │   (based on web.xml/annotations patterns: /api/*, /contracts/*, *.jsp, /)
  │
  └─ Calls chosen servlet's service(req, res)
          ↓
      Layer 2A: Classic Servlet path (manual)
          └─ Your servlet decides what to do, often by parsing URI or using extra libs
              OR
      Layer 2B: JAX‑RS path (annotation‑based)
          └─ JAX‑RS runtime matches @Path + HTTP verb and calls your resource method
```

- **Layer 1** (Tomcat) routes **by URL pattern** to **one servlet**.
- **Layer 2A**: In a **Classic Servlet**, you handle method/paths yourself.  
- **Layer 2B**: In **JAX‑RS**, a single “JAX‑RS servlet” delegates to annotated resource classes.

> In Nuxeo, **Layer 2B (JAX‑RS)** is what you usually extend.

---

## 2) Classic Servlet — Step by Step

### 2.1 Map the servlet (Layer 1)

`web.xml`:
```xml
<web-app>
  <servlet>
    <servlet-name>ContractServlet</servlet-name>
    <servlet-class>com.example.ContractServlet</servlet-class>
  </servlet>

  <servlet-mapping>
    <servlet-name>ContractServlet</servlet-name>
    <url-pattern>/contracts/*</url-pattern>
  </servlet-mapping>
</web-app>
```
**Meaning**: any URL under `/contracts/...` goes to `ContractServlet`.

**Pattern priority in Tomcat** (highest → lowest):  
1) exact (`/foo/bar`) → 2) longest path (`/foo/*`) → 3) extension (`*.jsp`) → 4) default (`/`).

---

### 2.2 Implement the servlet (Layer 2A)

```java
package com.example;

import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.util.stream.Collectors;

@WebServlet(name="ContractServlet", urlPatterns={"/contracts/*"})
public class ContractServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String uri = req.getRequestURI();      // e.g. "/contracts/123"
    String id  = uri.substring(uri.lastIndexOf('/') + 1); // "123"
    resp.setContentType("application/json");
    resp.getWriter().println("{\"id\":\"" + id + "\",\"status\":\"OK\"}");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String body = req.getReader().lines().collect(Collectors.joining());
    // parse JSON, run logic...
    resp.setStatus(HttpServletResponse.SC_CREATED);
  }
}
```

**What you handle yourself**:  
- Parse path segments (`/contracts/123`).  
- Parse JSON body.  
- Map verbs to `doGet/doPost/...` (the servlet API does the last bit).

**Pros**: maximum control, minimal dependencies.  
**Cons**: you reinvent routing, parameter binding, content negotiation.

---

### 2.3 Request example (Classic Servlet)

- `GET /contracts/123` → Tomcat matches `/contracts/*` → calls `doGet()` → you parse `123`.  
- `POST /contracts` → same servlet → `doPost()` handles body.

---

## 3) JAX‑RS — Step by Step

### 3.1 Register the JAX‑RS servlet (Layer 1)

`web.xml` (or equivalent auto-setup):
```xml
<servlet>
  <servlet-name>JAXRS</servlet-name>
  <servlet-class>org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet</servlet-class>
</servlet>

<servlet-mapping>
  <servlet-name>JAXRS</servlet-name>
  <url-pattern>/api/*</url-pattern>
</servlet-mapping>
```
**Meaning**: everything under `/api/*` goes to the **JAX‑RS engine** (still a servlet!).

---

### 3.2 Implement resource classes (Layer 2B)

```java
package com.example.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;

@Path("/contracts")
@Produces(MediaType.APPLICATION_JSON)
public class ContractResource {

  // GET /api/contracts/123
  @GET
  @Path("/{id}")
  public Response getById(@PathParam("id") String id) {
    Map<String,Object> dto = new HashMap<>();
    dto.put("id", id);
    dto.put("status", "OK");
    return Response.ok(dto).build();
  }

  // POST /api/contracts/approve
  @POST
  @Path("/approve")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response approve(String body) {
    // parse JSON if needed, or bind to a POJO param
    return Response.ok("{\"result\":\"approved\"}").build();
  }
}
```

**What JAX‑RS gives you** (so you don’t hand‑roll it):  
- URL routing with `@Path` (including params like `/{id}`).  
- HTTP verb routing with `@GET`, `@POST`, etc.  
- Parameter injection: `@PathParam`, `@QueryParam`, `@HeaderParam`, `@Context`, …  
- Content types via `@Consumes/@Produces` (JSON/XML).

**Pros**: clean annotations, less glue code, standard across Java.  
**Cons**: adds a framework layer (but it’s the standard one).

---

### 3.3 Request example (JAX‑RS)

- `GET /api/contracts/123` → Tomcat → JAX‑RS servlet → finds `@GET @Path("/{id}")` → calls `getById("123")`.  
- `POST /api/contracts/approve` → finds `@POST @Path("/approve")` → calls `approve(...)`.

---

## 4) Side‑by‑Side Comparison

| Topic                     | Classic Servlet                           | JAX‑RS (CXF/Jersey/RESTEasy)                    |
|--------------------------|--------------------------------------------|--------------------------------------------------|
| URL routing              | `web.xml` + code parsing                   | `@Path` on classes/methods                       |
| Verb routing             | `doGet/doPost/...`                         | `@GET/@POST/@PUT/@DELETE`                        |
| Params                   | `req.getParameter()`, parse URI segments   | `@PathParam`, `@QueryParam`, `@HeaderParam`      |
| Content types            | Set headers manually                       | `@Produces/@Consumes`                            |
| JSON binding             | Manual parsing                             | MessageBodyReaders/Writers (auto)                |
| Testability              | More boilerplate                           | Cleaner, finer‑grained methods                    |
| Nuxeo alignment          | Rarely used for custom code                | **Preferred** (Nuxeo’s REST API is JAX‑RS)       |

---

## 5) How This Maps to **Nuxeo**

- Nuxeo runs on Tomcat, and exposes **REST endpoints** under `/nuxeo/api/v1/...` using **JAX‑RS**.  
- To extend Nuxeo, you typically:  
  1) Write a **JAX‑RS resource class** (`@Path` + methods).  
  2) Register it with Nuxeo (via an OSGi contribution / module so the JAX‑RS runtime discovers it).  
  3) Call **Nuxeo services** (CoreSession, Query, Automation) inside your resource method.

> You normally **do not** write raw servlets in Nuxeo projects; JAX‑RS is the clean extension point.

---

## 6) Dispatcher Matching Details (for your notes)

**Tomcat’s servlet mapping order**:  
1. **Exact match** (`/foo/bar`)  
2. **Longest path match** (`/foo/*`)  
3. **Extension match** (`*.jsp`)  
4. **Default servlet** (`/`)  

**Servlet API flow**:  
- Dispatcher calls `service(req, res)` on servlet → `service()` routes to `doGet/doPost/...` based on HTTP method.  
- For JAX‑RS, the JAX‑RS servlet delegates to a registry that matches `@Path` + verb and invokes the method.

---

## 7) Concrete End‑to‑End Example (Both Paths)

### Classic Servlet
- **URL**: `GET /contracts/123`  
- **Layer 1**: `/contracts/*` → `ContractServlet`  
- **Layer 2A**: `doGet()` parses `123` from `req.getRequestURI()`, returns JSON.

### JAX‑RS
- **URL**: `GET /api/contracts/123`  
- **Layer 1**: `/api/*` → JAX‑RS servlet  
- **Layer 2B**: finds `@GET @Path("/{id}")`, injects `id=123`, returns JSON.

---

## 8) Choosing Which One

- **Use Classic Servlet** only when you need a very low‑level hook or you’re inside a legacy app that already uses servlets heavily.  
- **Use JAX‑RS** for anything RESTful (CRUD APIs, integration endpoints) — it’s cleaner, standard, and what **Nuxeo** is built on.

---

## 9) Quick Testing Snippets

```bash
# JAX-RS resource GET
curl -i http://localhost:8080/api/contracts/123

# JAX-RS resource POST
curl -i -X POST http://localhost:8080/api/contracts/approve \
     -H 'Content-Type: application/json' \
     -d '{"id":"123"}'
```

For a Classic Servlet mapped on `/contracts/*`:
```bash
curl -i http://localhost:8080/contracts/123
```

---

## 10) Troubleshooting Tips

- **404** on `/api/...`? Check the **servlet mapping** for the JAX‑RS servlet.  
- **Method not allowed (405)**? You hit a method without a matching JAX‑RS verb (e.g., POST to a `@GET`).  
- **Bad JSON**? Ensure `@Consumes(MediaType.APPLICATION_JSON)` and that a proper JSON provider is on the classpath.  
- **Ambiguous path**? Two resource methods match the same `@Path` + verb — make paths unambiguous.

---

## 11) Mini Cheat Sheet

- Tomcat routes **URL → servlet** (Layer 1).  
- Servlet API routes **HTTP verb → method** (Layer 2A).  
- JAX‑RS routes **@Path + verb → resource method** (Layer 2B).  
- In Nuxeo: extend via **JAX‑RS resource classes**, not raw servlets.
