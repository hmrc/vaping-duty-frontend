# System Patterns: Vaping Duty Frontend

## System Architecture

The Vaping Duty Frontend follows a classic Play Framework Model-View-Controller (MVC) architecture with additional layers for separation of concerns:

```
User Request → Routes → Controller → Service → Connector → External APIs
                 ↓
              Response ← View ← Controller
```

### Architectural Layers

1. **Routes**: Defined in `conf/app.routes`, mapping URLs to controller actions
2. **Controllers**: Handle HTTP requests, invoke services, and return views
3. **Services**: Contain business logic and orchestrate operations
4. **Connectors**: Interface with external services and APIs
5. **Models**: Define data structures and business objects
6. **Views**: Twirl templates that render HTML responses
7. **Forms**: Define and validate user input

## Key Technical Decisions

### Authentication and Authorization

The system employs a multi-layered authentication approach:

1. **Base Authentication**: Leverages HMRC's auth module (`uk.gov.hmrc.play.bootstrap.AuthModule`)
2. **Role-Based Actions**: Custom actions like `ApprovedVapingManufacturerAuthAction` for specific authorization
3. **Enrollment Checking**: Actions like `NoEnrolmentAction` and `HasEnrolmentAction` to verify enrollment status

### Data Flow and State Management

1. **UserAnswers**: Central model for storing user journey data
2. **MongoDB Integration**: For persisting user session data
3. **Data Actions**: `DataRetrievalAction` and `DataRequiredAction` for loading and ensuring data availability

### Navigation System

1. **Navigator Class**: Central manager for determining next pages in user journeys
2. **Mode-Based Routing**: Support for different navigation flows based on Normal/Check modes
3. **Page Hierarchy**: Page objects represent steps in user journeys

### Form Handling

1. **Form Providers**: Separate classes for form creation and validation
2. **Constraints**: Reusable validation constraints and formatters
3. **Error Handling**: Standardized error message format and display

## Component Relationships

### Controller Structure

Controllers follow a consistent pattern:
- Dependency injection for required services and actions
- `onPageLoad` method for displaying forms/pages
- `onSubmit` method for processing submissions
- Authentication action composition for security

Example:
```scala
def onPageLoad(): Action[AnyContent] = (ifApprovedVapingManufacturer andThen getData andThen requireData) {
  implicit request => ...
}
```

### Service Layer Pattern

Services encapsulate business logic and communicate with external systems through connectors:
- Clear separation from controllers
- Focused on orchestrating operations
- Return data to controllers for rendering

### Authentication Action Composition

The system uses action composition to build authentication chains:
```scala
(identify andThen getData andThen requireData).async { ... }
```

This pattern allows for flexible authorization requirements while maintaining code clarity.

## Critical Implementation Paths

### Contact Preference Journey

1. User chooses contact preference (email/post)
2. If email is selected:
   - User enters email
   - Email verification is triggered
   - User confirms verification
   - Preference is saved
3. If post is selected:
   - Address is confirmed
   - Preference is saved

### Enrollment Journey

1. User is authenticated
2. System checks if user has enrollment
3. If not enrolled, user is asked if they have a VPD approval ID
4. Based on response, appropriate enrollment path is followed

### Data Persistence Pattern

The application uses a consistent pattern for updating user answers:
```scala
for {
  updatedAnswers <- Future.fromTry(request.userAnswers.set(SomePage, value))
  _              <- sessionService.set(updatedAnswers)
} yield Redirect(navigator.nextPage(SomePage, mode, updatedAnswers))
```

## Routing Configuration

### Route File Structure (Updated 4 March 2026)

**Current Structure**:
- All application routes defined in `conf/app.routes`
- Returns journey routes included directly in `app.routes` with clear section headers
- `conf/prod.routes` includes only:
  - `-> /vaping-duty app.Routes`
  - `-> / health.Routes`

**Why Single Route File**:
- Previously had separate `conf/returns.routes` file
- This caused `type Routes is not a member of models.returns` compilation error
- Play Framework generates route objects based on controller packages, not arbitrary package names
- Merged all returns routes into `app.routes` for simplicity
- Follows HMRC standard pattern for frontend applications

**Route Organization in app.routes**:
- Clear section headers for each journey (e.g., `# Returns Journey`, `# Contact Preferences`)
- Sub-sections for related pages (e.g., `# Returns - Duty Declaration`)
- Maintains URL prefix structure (e.g., `/returns/`, `/contact-preferences/`)

## Integration Points

1. **Auth Service**: User authentication and authorization
2. **Email Verification**: Verification of user email addresses
3. **Backend Vaping Duty Service**: Core business functionality
4. **Account Service**: User account management
5. **Finance Service**: Financial aspects of vaping duty
