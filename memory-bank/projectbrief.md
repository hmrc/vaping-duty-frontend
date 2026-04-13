# Project Brief: Vaping Duty Frontend

## Project Purpose

The Vaping Duty Frontend is a microservice that provides the user interface for the Vaping Products Duty service. It serves as the primary interaction point for approved vaping manufacturers to manage their vaping duty obligations.

## Core Requirements

1. **User Authentication**: Secure login and authentication for approved vaping manufacturers.
2. **Enrollment Management**: Support for users to enroll in the service using their Vaping Products Duty (VPD) approval ID.
3. **Contact Preference Management**: Allow users to set and manage their preferred contact methods (email or post).
4. **User Journey Flows**: Well-defined user journeys for all key processes.
5. **Integration with Backend Services**: Seamless connection with other vaping duty services.
6. **Compliance with HMRC Standards**: Adherence to HMRC frontend design patterns and accessibility requirements.

## Project Scope

### In Scope

- User authentication and authorization
- Enrollment process for approved manufacturers
- Contact preference settings and management
- Email verification workflows
- User journey recovery mechanisms
- Multi-language support (English and Welsh)

### Out of Scope

- Actual duty calculations and assessments (handled by backend services)
- Payment processing (likely handled by other HMRC services)
- Approval processes for becoming a vaping manufacturer (separate service)

## Service Goals

- Provide a user-friendly interface for vaping manufacturers to interact with the Vaping Products Duty service
- Ensure secure and reliable access to service features
- Support HMRC's digital-first approach to tax collection and management
- Maintain compliance with government accessibility standards
- Enable efficient communication between HMRC and vaping manufacturers

## Related Services

- **Backend**: vaping-duty - Core business logic and data processing
- **Account**: vaping-duty-account - Account management functionality
- **Finance**: vaping-duty-finance - Financial aspects of vaping duty
- **Stub**: vaping-duty-stubs - Test stubs for development