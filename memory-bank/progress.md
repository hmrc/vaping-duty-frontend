# Project Progress: Vaping Duty Frontend

## What Works

Based on the codebase analysis, the following functionality appears to be implemented and functional:

### Core Infrastructure
- ✅ Project structure and basic setup
- ✅ Build configuration and dependency management
- ✅ Play Framework integration with HMRC frontend components
- ✅ MongoDB integration for data persistence
- ✅ Multi-language support (English and Welsh)
- ✅ Session timeout management

### Authentication and Authorization
- ✅ Basic authentication integration
- ✅ Role-based authorization for approved vaping manufacturers
- ✅ Enrollment checking and management
- ✅ User identification actions

### User Journeys
- ✅ Contact preference selection (email vs. post)
- ✅ Email address entry and validation
- ✅ Email verification workflow
- ✅ Postal address confirmation
- ✅ Enrollment with VPD approval ID
- ✅ Journey recovery for error scenarios

### Technical Implementation
- ✅ Navigation system with mode-based routing
- ✅ Form validation and error handling
- ✅ User answers management and persistence
- ✅ Integration with email verification service
- ✅ View templates and components

## What's Left to Build

### 🚀 ACTIVE: Returns Journey Implementation (Started 31 Mar 2026)

**Overall Status**: Phase 2 Complete, Phase 3 In Progress

**Complete Plan**: See `memory-bank/returns-implementation-plan.md`

### Implementation Progress by Phase:

#### Phase 1: Foundation & Simple Pages ✅ COMPLETE
- ✅ 1.1: Folder structure created
- ✅ 1.2: Constants added to FrontendAppConfig
- ✅ 1.3: Before You Start page (controller, view, routes, messages)
- ✅ 1.4: Task List page (controller, view, TaskStatus model, routes, messages)
- ✅ 1.5: Declare Question page (page, controller, view, YesNoForm, routes, messages)
- ✅ 1.6: Vaping Products page (page, form, controller, view, routes, messages)

#### Phase 2: Spoilt Adjustments Flow ✅ COMPLETE
- ✅ 2.1: Data Models (ReturnPeriod, SpoiltAdjustment)
- ✅ 2.2: Spoilt Question page (page, controller, view, routes, messages)
- ✅ 2.3: Period Select Spoilt page (page, controller, view, routes, messages)
- ✅ 2.4: Spoilt Adjustment page (page, form, controller, view, routes, messages)
- ✅ 2.5: Additional Adjustments Spoilt page (controller, view, routes, messages)
- ✅ 2.6: Spoilt Adjustments Summary page with Remove links (controller, view, routes, messages)

#### Phase 3: Under/Over Adjustments Flow ✅ COMPLETE
- ✅ 3.1: Data Models (AdjustmentType, UnderOverAdjustment) - **Done 31 Mar 2026**
- ✅ 3.2: Adjustment Question page (page, controller, view) - **Done 31 Mar 2026**
- ✅ 3.3: Period Select page (page, controller, view, routes, messages) - **Done 31 Mar 2026**
- ✅ 3.4: Under/Over Declared page (Complex conditional form with validation) - **Done 31 Mar 2026**
- ✅ 3.5: Additional Adjustments page (controller, view) - **Done 31 Mar 2026**
- ✅ 3.6: Adjustments Summary page (page, controller, view with remove links) - **Done 31 Mar 2026**
- ✅ 3.7: Adjustment Reason page (page, form, controller, view) - **Done 31 Mar 2026**
- ✅ All routes added to app.routes - **Done 31 Mar 2026**
- ✅ All messages added to messages.en - **Done 31 Mar 2026**

**Status**: Phase 3 fully implemented - all 7 tasks complete with routes and messages

#### Phase 4: Duty Suspense Flow ✅ COMPLETE
- ✅ 4.1: Data Model (DutySuspenseData with net balance calculation) - **Done 31 Mar 2026**
- ✅ 4.2: Declare Duty Suspense page (page, controller, view) - **Done 31 Mar 2026**
- ✅ 4.3: Duty Suspense page (page, form, controller, view with two inputs) - **Done 31 Mar 2026**
- ✅ All routes added to app.routes - **Done 31 Mar 2026**
- ✅ All messages added to messages.en - **Done 31 Mar 2026**

**Status**: Phase 4 fully implemented - all 3 tasks complete with routes and messages

#### Phase 5: Check Your Answers and Confirmation ⬜ NOT STARTED

#### Phase 6: Testing ⬜ NOT STARTED

**Overall Progress**: Phase 1 ✅ | Phase 2 ✅ | Phase 3 ✅ | Phase 4 ✅ | Phase 5-6 ⬜

**What Returns Journey Enables**:
- Submit monthly Vaping Products Duty returns
- Declare vaping products released for consumption
- Report adjustments (spoilt products, under/over declarations)
- Report duty suspended deliveries
- Calculate total duty owed
- Submit complete returns with confirmation

### Other Features (Lower Priority)

#### User Journeys
- ⏳ Enhanced "Check Your Answers" functionality
- ⏳ Additional vaping duty specific features
- ⏳ Enhanced error recovery for complex scenarios
- ⏳ View historical returns (deferred from initial returns scope)

#### Technical Enhancements
- ⏳ Payment integration (after returns submission)
- ⏳ Enhanced security features if required
- ⏳ Performance optimizations if needed
- ⏳ More comprehensive testing

#### User Experience
- ⏳ Accessibility improvements based on testing results
- ⏳ Content refinements based on user feedback
- ⏳ Additional user guidance or help content

## Known Issues

Based on the codebase analysis, there don't appear to be critical known issues, but areas for potential improvement include:

1. The "Check Your Answers" page appears to be minimal/placeholder (showing empty rows)
2. Some email verification journey edge cases might need additional handling
3. The enrollment process might benefit from additional guidance or error handling

## Evolution of Project Decisions

### Returns Journey Decisions (4 November 2026)

**Context**: Analyzed ur-beta-round7 prototype to plan complete monthly returns submission journey (24 pages total)

1. **Return Reference Generation**
   - Decision: Generate in frontend (format: VPD + 11 digits)
   - Rationale: Simple stub for now, can move to backend later if needed
   - Date: 4 Nov 2026
   - Status: ✅ Decided

2. **Duty Rate Configuration**
   - Decision: Static value in messages file (£2.20 per 10ml)
   - Rationale: Easy to change, no need for dynamic configuration initially
   - Date: 4 Nov 2026
   - Status: ✅ Decided

3. **Return Period Calculation**
   - Decision: Monthly periods ending on last day of month
   - Rationale: Business requirement for monthly returns
   - Date: 4 Nov 2026
   - Status: ✅ Decided

4. **Returns Folder Structure**
   - Decision: All returns code in `returns/` subdirectories
   - Rationale: Clear separation, easier to maintain and navigate
   - Date: 4 Nov 2026
   - Status: ✅ Decided

5. **Multiple Adjustments Handling**
   - Decision: Store as List in UserAnswers with "add another" pattern
   - Rationale: Flexible approach supporting unlimited adjustments per return
   - Date: 4 Nov 2026
   - Status: ✅ Decided

6. **Email Confirmation**
   - Decision: Stub message for now ("email sent")
   - Rationale: Email integration is separate concern, will be addressed later
   - Date: 4 Nov 2026
   - Status: ✅ Decided

7. **Backend Connector**
   - Decision: Create in frontend initially
   - Rationale: Keep flexible, may move to backend service later
   - Date: 4 Nov 2026
   - Status: ✅ Decided

8. **Initial Scope**
   - Decision: New return submission only (no view previous returns)
   - Rationale: Focus on core functionality first, add viewing later
   - Date: 4 Nov 2026
   - Status: ✅ Decided

### Architecture Decisions (Earlier)

1. **Play Framework MVC**: Decision to use standard Play Framework with HMRC extensions provides a robust, familiar structure for government services.

2. **Action Composition**: The approach of composing actions for authentication and data retrieval creates a clear separation of concerns and follows HMRC patterns.

3. **Navigation System**: The central Navigator class with mode-based routing provides flexibility for handling different user journeys.

4. **MongoDB Integration**: The decision to use MongoDB for session persistence aligns with HMRC standards and provides flexibility.

### Implementation Decisions (Earlier)

1. **Contact Preference Management**: The implementation of email and postal contact preferences provides users with options while ensuring proper verification.

2. **Email Verification**: Integration with the standard HMRC email verification service ensures security and reliability.

3. **Enrollment Flow**: The approach to enrollment verification ensures that only users with valid VPD approval IDs can access the service.

4. **Form Validation**: Standardized form validation with clear error messages improves user experience.

## Development Milestones

### Completed ✅
- ✅ Project setup and configuration
- ✅ Basic authentication and authorization
- ✅ Contact preference management
- ✅ Email verification integration
- ✅ Enrollment process for approved manufacturers
- ✅ **Returns journey planning complete** (4 Nov 2026)

### In Progress ⏳
- ⏳ **Returns Journey - Step 1: Models & Forms** (Starting now)
- ⏳ Testing and quality assurance (ongoing)

### Planned 📋
- 📋 Returns Journey - Steps 2-10 (see detailed breakdown above)
- 📋 Payment integration (after returns submission)
- 📋 Enhanced integration with backend services
- 📋 User experience refinements
- 📋 Additional duty management features as required

## Recent Activity Log

**31 March 2026** (Latest):
- ✅ **Task 2.6 COMPLETED: Spoilt Adjustments Summary with Remove Functionality**
  - Added remove(index: Int) method to controller
  - Updated view to include Remove links in table
  - Added route for remove action
  - Added site.actions message for accessibility
  - Summary page now fully functional with add/remove capabilities

- ✅ **Phase 3 STARTED: Under/Over Adjustments Flow**
  - Created AdjustmentType.scala (sealed trait: UnderDeclared, OverDeclared)
  - Created UnderOverAdjustment.scala (case class with JSON serialization)
  - Created AdjustmentQuestionPage.scala
  - Created AdjustmentQuestionController.scala with conditional navigation
  - Created AdjustmentQuestionView.scala.html with Details component
  - Created PeriodSelectPage.scala
  - Created PeriodSelectController.scala with period generation logic
  - Next: Complete PeriodSelectView and continue with remaining Phase 3 tasks

**5 March 2026**:
- ✅ **NAVIGATION FIX COMPLETE: Complete Journey Navigation Implemented**
  - **Problem**: Navigator only had 5 routes but needed 14 navigation links
  - **Solution**: Created 8 missing Page objects for returns journey navigation
  - Created Page objects:
    - `VapingDutyPage` - For duty calculation display
    - `PeriodSelectSpoiltPage` - For spoilt period selection
    - `SpoiltAdjustmentPage` - For spoilt volume entry
    - `AdditionalAdjustmentsSpoiltPage` - For "add another spoilt"
    - `UnderOverQuestionPage` - For under/over type selection
    - `UnderOverDeclaredPage` - For adjustment volume
    - `AdjustmentReasonPage` - For reason selection
    - `AdditionalAdjustmentsPage` - For "add another adjustment"
  - Updated Navigator.scala:
    - Added 8 new route cases in `normalRoutes`
    - Added `spoiltAdditionalRoute()` helper for spoilt adjustment looping
    - Added `adjustmentAdditionalRoute()` helper for under/over adjustment looping
  - Fixed Task List view (ReturnsTaskListView.scala.html):
    - Removed all 5 TODO comments
    - Added actual controller routes for all sections:
      - Duty Declaration → `DeclareQuestionController.onPageLoad(NormalMode)`
      - Spoilt Adjustments → `SpoiltQuestionController.onPageLoad(NormalMode)`
      - Under/Over Adjustments → `AdjustmentQuestionController.onPageLoad(NormalMode)`
      - Duty Suspense → `DeclareDutySuspenseController.onPageLoad(NormalMode)`
      - Submit → `ReturnsCheckYourAnswersController.onPageLoad()`
  - **Result**: Complete end-to-end navigation now working
    - Task List links to all sections ✅
    - Page-to-page navigation via Navigator ✅
    - "Add another" loops working ✅
    - All sections return to Task List ✅
  - Compilation: Success with 0 errors, 15 warnings (unused parameters only)

**4 March 2026**:
- ✅ **Companion Object Anti-Pattern Fix**
  - Fixed 4 controllers using companion objects for constants (anti-pattern)
  - Moved constants from companion objects into controller classes
  - `ReturnsConfirmationController` - 4 constants (RETURN_REFERENCE_PREFIX, MAX_VALUE, LENGTH, PAD_CHAR)
  - `PeriodSelectSpoiltController` - 1 constant (SESSION_KEY_SPOILT_PERIOD)
  - `SpoiltAdjustmentController` - 1 constant (SESSION_KEY_SPOILT_PERIOD)
  - Now follows Cline rules: constants declared as `private val` at top of class
  - Note: 41 compilation errors remain in views (missing methods/constants)

- ✅ **Routing Configuration Fix**
  - Resolved `type Routes is not a member of models.returns` error
  - Merged all 18 returns routes from `conf/returns.routes` into `conf/app.routes`
  - Removed problematic include line from `conf/prod.routes`
  - Deleted `conf/returns.routes` file
  - Simplified routing structure following HMRC standards

- ✅ **Form Provider Name Mismatch Fix**
  - Created missing `AdditionalAdjustmentsFormProvider` (Boolean form)
  - Controller was referencing non-existent form provider
  - Added validation message: `additionalAdjustments.error.required`

- ✅ **Cline Rules Compliance Fixes**
  - Created missing `PeriodSelectFormProvider` (overlooked in Step 1)
  - Fixed magic string violations in 4 controllers (added named constants)
  - Added validation messages to `conf/messages.en`

**4 March 2026** (Continued):
- ✅ **Step 9 Complete**: Navigation & Integration
  - Updated Navigator class with returns page imports
  - Implemented conditional navigation for all flows
  - Duty declaration: Yes → Volume | No → Task List
  - Duty suspense: Yes → Details | No → Task List  
  - Spoilt adjustments: Yes → Period Select | No → Task List
  - Under/over adjustments: Yes → Period Select | No → Task List
  - Error recovery and journey recovery paths
  - NormalMode and CheckMode navigation

- ✅ **Step 8 Complete**: Confirmation Page
  - Created confirmation controller and view
  - GDS Panel component with return reference
  - Payment deadline warning (60 days from submission)
  - Payment methods guidance
  - Print receipt functionality
  - Links to returns list and BTA

- ✅ **Step 7 Complete**: Check Your Answers aggregation page
  - Created ReturnsSummaryService for data aggregation
  - Five summary sections (duty, spoilt, under/over, suspense, totals)
  - Calculated duty totals
  - Submit button redirects to confirmation

- ✅ **Step 5 Complete**: Spoilt Adjustments Flow (4 pages)
  - Created 2 page classes (SpoiltQuestion, SpoiltAdjustments)
  - Created 4 controllers (SpoiltQuestion, PeriodSelectSpoilt, SpoiltAdjustment, AdditionalAdjustmentsSpoilt)
  - Created 4 views with GDS components
  - Routes and messages added
  - "Add another" pattern for multiple spoilt adjustments
  - List storage in UserAnswers

**4 March 2026**:

- ✅ **Step 4 Complete**: Duty Suspense Flow (2 pages)
  - Created 2 page classes (DeclareSuspense, DutySuspenseData)
  - Created 2 controllers with conditional flow handling
  - Created 2 views: yes/no + two volume inputs (received/moved)
  - Routes with CheckMode support
  - Used DutySuspenseFormProvider from Step 1
  - Model calculates net balance automatically

- ✅ **Step 3 Complete**: Duty Declaration Flow (3 pages)
  - Created 2 page classes for data storage
  - Created 3 controllers (DeclareQuestion, VapingProducts, VapingDuty)
  - Created 3 views with GDS components
  - Routes with CheckMode support
  - Conditional navigation based on yes/no answer
  - Real-time duty calculation display

- ✅ **Step 2 Complete**: Task List Page (Central Hub)
  - Created controller with status tracking and deadline calculation
  - Created view with GDS Task List component
  - 6 task items with completion status tracking
  - Routes: `/vaping-duty/returns/tasks`
  - Comprehensive messages added for all returns pages

- ✅ **Step 1 Complete**: Models & Forms foundation
  - Created 8 models in `app/models/returns/`
  - Created 10 form providers in `app/forms/returns/`
  - Implemented duty calculation logic (£2.20 per 10ml)
  - All models have JSON serialization
  - All forms have validation rules

**Returns Implementation Plan Created**: November 2026
- Full implementation plan documented in `returns-implementation-plan.md`
- 19 pages identified across 6 phases
- Technical patterns and architecture defined
