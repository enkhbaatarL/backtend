# Codex Master Feature Prompt

Use this prompt when you want Codex to analyze, design, and implement
a backend feature in one run.

This prompt follows the recommended engineering workflow:
investigation → design → API → ERD → implementation.

---

# Master Prompt

You are working on the MI Platform backend.

Follow AGENTS.md and repository conventions.

Feature request:

{describe the feature}

Background:

{business context}

---

# Tasks

Perform the following steps sequentially.

## 1. Investigate existing implementation

- Inspect related controllers
- Inspect related entities
- Inspect related repositories
- Identify existing APIs
- Identify related database tables

Output:

- related code structure
- reusable modules
- potential impact

---

## 2. Design the domain model

Define:

- aggregate root
- lifecycle
- business rules
- relationships
- table design

Output:

- domain model
- lifecycle states
- ERD draft

---

## 3. Define API contract

Generate:

- endpoints
- request DTO
- response DTO
- validation rules
- error cases

Constraints:

- BaseResponse<T>
- consistent naming

---

## 4. Review ERD impact

Check:

- schema conflicts
- FK consistency
- index strategy
- migration safety

Environment:

- ddl-auto=validate
- DBA deployment required

---

## 5. Verify architecture consistency

Check:

- controller/service/repository separation
- JPA / MyBatis / QueryDSL usage
- naming conventions
- transaction boundaries

---

## 6. Generate implementation plan

Provide:

- controller code
- service code
- entity
- repository
- DTO

Also include:

- changed files
- SQL impact
- risks
