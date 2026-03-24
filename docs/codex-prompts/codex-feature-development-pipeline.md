# Codex Feature Development Pipeline (6-Step Prompt Guide)

This document provides a reusable prompt set for implementing a new backend feature
in the MI Platform repository using Codex.

Recommended flow:

1. feature-delivery-orchestrator
2. new-domain-model-design
3. backend-feature-spec
4. erd-change-review
5. design-consistency-review
6. spring-api-implementation

---

# Step 1 — Feature Investigation

Goal: understand existing implementation and reusable modules.

Prompt:

Use skill: feature-delivery-orchestrator

Investigate the existing implementation related to the requested feature.

Tasks:
1. Inspect related controllers, services, repositories and entities
2. Identify existing APIs and DTO patterns
3. Identify related database tables
4. Identify reusable modules

Constraints:
- Follow AGENTS.md
- Maintain BaseResponse<T>
- Respect layered architecture

Output format:
1. related controllers
2. related entities
3. related repositories
4. related tables
5. existing API patterns
6. implementation roadmap

---

# Step 2 — Domain Model Design

Goal: define domain model and lifecycle.

Prompt:

Use skill: new-domain-model-design

Based on the investigation result, design the domain model.

Requirements:
- Follow AGENTS.md
- Align with MI Platform architecture
- Identify aggregate root
- Define lifecycle
- Propose table design
- Define relationships

Output format:
1. feature summary
2. domain model
3. lifecycle
4. business rules
5. ERD draft
6. extension points

---

# Step 3 — Backend Feature Specification

Goal: define API contract and DTOs.

Prompt:

Use skill: backend-feature-spec

Using the domain model design, write the backend feature specification.

Include:
- API endpoints
- request DTO
- response DTO
- validation rules
- business rules
- error scenarios
- data access strategy

Constraints:
- Response must use BaseResponse<T>
- Follow newsletter API conventions

Output format:
1. feature goal
2. endpoints
3. request DTO
4. response DTO
5. error cases
6. data access strategy

---

# Step 4 — ERD Change Review

Goal: verify schema safety.

Prompt:

Use skill: erd-change-review

Review the proposed schema changes.

Check:
- existing table conflicts
- FK consistency
- index strategy
- data type compatibility
- migration safety

Environment constraints:
- ddl-auto=validate
- DBA-driven schema deployment

Output format:
1. schema change classification
2. proposed schema change
3. ERD impact
4. migration plan
5. DBA action items
6. risks

---

# Step 5 — Design Consistency Review

Goal: validate architecture alignment.

Prompt:

Use skill: design-consistency-review

Review the design for consistency with MI Platform architecture.

Check:
- controller/service/repository separation
- JPA / MyBatis / QueryDSL usage
- naming conventions
- API response patterns

Output format:
1. architecture compliance
2. naming review
3. data access separation
4. API consistency
5. risks
6. recommendations

---

# Step 6 — Spring API Implementation

Goal: generate implementation code.

Prompt:

Use skill: spring-api-implementation

Implement the feature based on the approved design.

Requirements:
- Spring Boot 3.4
- Java 17
- BaseResponse<T> response format
- Follow layered architecture

Generate:
- controller
- service
- DTO
- entity
- repository

Also include:
- SQL impact
- changed files
- potential risks
