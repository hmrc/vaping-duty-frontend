# Form Rules

## Before Creating a Form

1. **Check for existing forms** - Can you reuse an existing one?
2. **Make it generic** - If creating new, make it reusable where possible
3. **Keep field names simple** - Use "text" for single text fields, avoid custom names when unnecessary

---

## Rule 1: Reuse Existing Forms

When building forms to use throughout the project, where it is reasonably easy to do so, reuse existing forms.

**Before creating a new form:**
- Search the `/forms` directory for similar forms
- Check if you can parameterize an existing form
- Only create new forms when truly necessary

---

## Rule 2: Make Forms Generic

If you need to make a new form, attempt to make it generic so it can be reused in other places.

### ✅ Good Generic Form:
```scala
// GenericTextForm.scala
case class TextInput(value: String)

object TextInput {
  def form(fieldName: String = "text"): Form[TextInput] = Form(
    mapping(
      fieldName -> nonEmptyText
    )(TextInput.apply)(TextInput.unapply)
  )
}
```

---

## Rule 3: Simple Field Names

Do your best to avoid requiring custom form field names. If a page only takes 1 field, and it is text, just use "text" as the field name.

### ✅ Simple Field Naming:
```scala
// For a single text field, just use "text"
val form = Form(mapping("text" -> nonEmptyText)(Identity.apply)(Identity.unapply))

// For multiple related fields, use descriptive names
val form = Form(mapping(
  "firstName" -> nonEmptyText,
  "lastName" -> nonEmptyText
)(Name.apply)(Name.unapply))
```

---

## Rule 4: Forms Need Case Classes

Forms should have an associated case class, so the apply and unapply can be done natively in Scala 2.13, rather than creating a manual object.

### ✅ Correct Pattern:
```scala
// UserForm.scala
case class UserForm(name: String, email: String)

object UserForm {
  implicit val format: OFormat[UserForm] = Json.format[UserForm]
  
  val form: Form[UserForm] = Form(
    mapping(
      "name" -> nonEmptyText,
      "email" -> email
    )(UserForm.apply)(UserForm.unapply)  // Uses case class's automatic apply/unapply
  )
}
```

### ❌ Don't Do Manual Object:
```scala
// DON'T create manual apply/unapply - use case class
object UserForm {
  def apply(name: String, email: String): UserData = ...
  def unapply(data: UserData): Option[(String, String)] = ...
}
```

---

## Rule 5: Store Form Data as Models

Form data stored in session or any other storage method should be stored and retrieved through a case class model, serialized to/from JSON using implicit `Reads` and `Writes` (or `Format`). Never store raw field strings directly.

### ✅ Correct Pattern:
```scala
// Store as case class with JSON serialization
case class ContactPreference(method: String, email: Option[String])

object ContactPreference {
  implicit val format: OFormat[ContactPreference] = Json.format[ContactPreference]
}

// In your controller/repository
def save(data: ContactPreference): Future[Unit] = {
  repository.set(Json.toJson(data))  // Serialized as JSON
}

def get(): Future[Option[ContactPreference]] = {
  repository.get().map(_.flatMap(_.asOpt[ContactPreference]))  // Deserialized from JSON
}
```

### ❌ Don't Store Raw Strings:
```scala
// DON'T do this - lose type safety and structure
def save(method: String, email: String): Future[Unit] = {
  repository.set(s"$method,$email")  // Raw string - BAD!
}
```

---

## Summary Checklist

When working with forms:
- [ ] Check if existing form can be reused
- [ ] Make new forms generic when reasonable
- [ ] Use simple field names ("text" for single fields)
- [ ] Always use case class with automatic apply/unapply
- [ ] Store form data as case classes with JSON serialization
- [ ] Never store raw strings directly
