# ERD Review Check Template

## 1. Scope

- feature/change title:
- affected domains:
- new/changed tables:
- expected rollout date:

## 2. Naming Rules

- table names follow `{t}{subject2}_{business}{entity_type}` pattern
- entity type code (`m/d/l/s/h/t/r/c`) matches data nature
- object names follow resource code rules (`vw/tr/pr/fu/ix/ux/fx/fk/ptr/ptl/pth/ptk`)
- sequence suffix is 2-digit (`01+`) when needed
- terms/abbreviations follow data dictionary

## 3. Model Integrity

- PK defined for every table
- PK column order reflects main query pattern
- PK column count is justified (prefer <= 4 unless explicit reason)
- FK is defined where referential integrity is required
- interface-receiving tables are intentionally excluded from FK if applicable
- bidirectional FK is avoided (or priority rationale documented)

## 4. Column Design

- column order follows `PK -> system columns -> business columns`
- system columns included: `reg_user_id`, `reg_dtm`, `mod_user_id`, `mod_dtm`, `del_yn`
- data types are justified (`char/varchar/text`, `decimal/float/double`, `datetime/timestamp`)
- nullability and default values are explicit
- logical delete policy (`del_yn`) is consistent

## 5. Index / Partition

- index names follow `{table_name}_{ix|ux|fx|fk}{nn}`
- secondary indexes are tied to concrete query patterns
- index count/width risk for DML is reviewed
- partition key belongs to PK/UK
- partition name follows `{ptr|ptl|pth|ptk}_{scope}`
- partition type/data type compatibility checked

## 6. Compatibility / Rollout

- backward compatibility impact documented (read/write/query path)
- migration order and fallback plan documented
- DBA manual deployment steps are explicit
- data backfill/rebuild needs are defined
- monitoring/verification points after deployment are defined

## 7. Open Risks

- unresolved decisions:
- assumptions:
- owner / due date:
