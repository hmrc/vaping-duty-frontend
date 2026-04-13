# Model Rules

## CRITICAL: Before Creating a Model

1. **One model per file** - Never put multiple case classes in one file
2. **Check Scala version** - Serialization differs between 2.13 and 3+
3. **Always include companion object** - For JSON serialization

---

## Rule 1: File Structure

Each file should contain **at most** 1 model and 1 associated companion object.

### ❌ DON'T DO THIS:
```scala
// Bad: Multiple models in one file
package uk.gov.hmrc.myservice.models

case class User(id: String, name: String)
case class Address(line1: String, postcode: String)
case class Phone(number: String, type: String)
```

### ✅ DO THIS:
```scala
// User.scala
package uk.gov.hmrc.myservice.models

import play.api.libs.json.{Json, OFormat}

final case class User(
  id: String,
  name: String
)

object User {
  implicit val format: OFormat[User] = Json.format[User]
}
```

```scala
// Address.scala (separate file)
package uk.gov.hmrc.myservice.models

import play.api.libs.json.{Json, OFormat}

final case class Address(
  line1: String,
  postcode: String
)

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]
}
```

---

## Rule 2: Do NOT Put Multiple Case Classes in One File

This is critical for maintainability and follows HMRC standards.

**Why?** 
- Easier to find models
- Clear file organization
- Prevents merge conflicts
- Follows single responsibility principle

---

## Rule 3: JSON Serialization (Scala 2.13)

In Scala 2.13, companion objects should use `Json.format[MyModel]` for serialisation rather than manually written apply/unapply.

### ✅ Correct Pattern:
```scala
final case class VpdSummaryResponse(
  service: Service,
  identifiers: Identifiers,
  contactPreference: ContactPreference,
  links: Links
)

object VpdSummaryResponse {
  implicit val format: OFormat[VpdSummaryResponse] = Json.format[VpdSummaryResponse]
}
```

### ❌ Don't Manually Write Apply/Unapply:
```scala
// Don't do this - Json.format handles it automatically
object VpdSummaryResponse {
  def apply(service: Service, identifiers: Identifiers, ...): VpdSummaryResponse = ...
  def unapply(response: VpdSummaryResponse): Option[(Service, Identifiers, ...)] = ...
}
```

---

## Rule 4: Scala 3 and Above

In Scala 3+, add an explicit `unapply` method to the case class companion object if needed for pattern matching beyond what the case class provides automatically.

```scala
final case class MyModel(id: String, value: Int)

object MyModel {
  implicit val format: OFormat[MyModel] = Json.format[MyModel]
  
  // Only if you need custom unapply logic
  def unapply(model: MyModel): Option[(String, Int)] = 
    Some((model.id, model.value))
}
```

---

## Summary Checklist

When creating a model:
- [ ] One case class per file
- [ ] Companion object with implicit format
- [ ] Use `Json.format[MyModel]` (don't write apply/unapply manually)
- [ ] Use `final` keyword for case classes
- [ ] Import only what you need: `{Json, OFormat}`
