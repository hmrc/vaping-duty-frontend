# Testing Rules

## CRITICAL: Do Not Run Tests

**Never run tests or execute any sbt commands.**
Write the test code only and stop.

The user will run tests manually in their own terminal.
If a test needs fixing, the user will paste the relevant
error back for you to address.

## CRITICAL: Before Writing Any Test

**ALWAYS perform these checks BEFORE writing a new test spec:**

1. **Read `SpecBase` first** - Check what it already provides. Your project's `SpecBase` includes:
   - `cc: ControllerComponents`
   - `fakeRequest: FakeRequest[AnyContentAsEmpty.type]`
   - `appConfig: AppConfig`
   - `bodyParsers: PlayBodyParsers`
   - `fakeAuthorisedAction: FakeAuthorisedAction`
   - `fakeCheckVpdIdAction: FakeCheckVpdIdAction`
   - `fakeCheckSignedInAction: FakeCheckSignedInAction`
   - `fakeUUIDGenerator: RandomUUIDGenerator`
   - `implicit val ec: ExecutionContext`
   - `implicit val hc: HeaderCarrier`

2. **NEVER redeclare these values in your spec** - Use them directly from SpecBase without redeclaring:
   ```scala
   // ❌ DON'T DO THIS:
   class MySpec extends SpecBase {
     implicit val hc: HeaderCarrier = HeaderCarrier()  // WRONG! Already in SpecBase
     val fakeRequest = FakeRequest()                    // WRONG! Already in SpecBase
   }
   
   // ✅ DO THIS:
   class MySpec extends SpecBase {
     // Just use hc and fakeRequest directly - they're inherited from SpecBase
   }
   ```

3. **Use pre-configured fake actions from SpecBase**:
   - ✅ DO: `fakeAuthorisedAction` (already configured with required dependencies)
   - ❌ DON'T: `new FakeAuthorisedAction()` (missing required `bodyParsers` parameter)
   - ✅ DO: `fakeCheckVpdIdAction`
   - ❌ DON'T: `new FakeCheckVpdIdAction()`

4. **Check existing similar tests** - Before creating a new test spec, find and examine a similar existing test in the same project to understand the established patterns and what SpecBase provides.

## General
1. When doing limit testing, only test upper and lower bounds.

2. Aim to reuse test code where possible. If you need to mock a certain function in multiple tests, make a generic helper function or `val` at the top of the file rather than repeating setup in each test.

## Base Class & Setup
3. All tests should extend a shared `SpecBase` trait. This base should provide the test framework mixins, Play result helpers, fake actions, and shared test data so individual specs don't need to redeclare them. A typical `SpecBase` looks like:
   ```scala
   trait SpecBase
       extends AnyFreeSpec
       with Matchers
       with MockitoSugar
       with ScalaFutures
       with GuiceOneAppPerSuite
       with TestData {

     override def fakeApplication(): Application =
       GuiceApplicationBuilder()
         .build()

     val cc: ControllerComponents                         = stubControllerComponents()
     val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
     val appConfig: AppConfig                             = app.injector.instanceOf[AppConfig]

     def fakeRequestWithJsonBody(json: JsValue): FakeRequest[JsValue] =
       FakeRequest("POST", "/", FakeHeaders(), json)

     implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
     implicit val hc: HeaderCarrier    = HeaderCarrier()
   }
   ```
   Do not redeclare things like `ec`, `hc`, or `fakeRequest` in individual specs if they are already provided by `SpecBase`.

4. Use the `AnyFreeSpec` nesting style with `- { }` for test structure. The top level names the class or method under test; nested blocks describe conditions:
   ```scala
   "getUser must" - {
     "return 200 OK when a record exists" in {
       // ...
     }
     "return 404 NOT_FOUND when no record exists" in {
       // ...
     }
   }
   ```

## Mocking
5. Use Mockito for mocking. Declare mocks at the top of the spec class with explicit types:
   ```scala
   val mockUserRepository: UserRepository = mock[UserRepository]
   val mockEmailConnector: EmailConnector = mock[EmailConnector]
   ```

6. Always explicitly type mock `val`s — do not rely on type inference.

7. Use `eqTo(...)` for specific argument matching and `any()` for generic arguments:
   ```scala
   import org.mockito.ArgumentMatchers.{any, eq as eqTo}
   import org.mockito.Mockito.when

   when(mockRepository.get(eqTo(specificId))(any()))
     .thenReturn(Future.successful(Some(record)))
   ```

8. Stub async methods with `Future.successful(...)`:
   ```scala
   when(mockConnector.submit(eqTo(payload), eqTo(id))(any()))
     .thenReturn(Future.successful(Right(response)))
   ```

## Controller Tests
9. Instantiate controllers directly by passing fakes and mocks into the constructor rather than using Guice injection. This keeps tests fast and explicit:
   ```scala
   val controller = new MyController(
     controllerComponents,
     mockRepository,
     mockConnector,
     fakeAuthorisedAction,
     clock
   )
   ```

10. Use `fakeRequest` for requests with no body, and `fakeRequestWithJsonBody(Json.toJson(...))` for requests with a JSON body (this helper should live in `SpecBase`):
    ```scala
    // GET with no body
    val result = controller.show(id)(fakeRequest)

    // POST with a JSON body
    val result = controller.submit()(fakeRequestWithJsonBody(Json.toJson(myPayload)))
    ```

11. Always assert both the status code AND the response body. Never test only the status code:
    ```scala
    status(result)        mustBe OK
    contentAsJson(result) mustBe Json.toJson(expectedResponse)
    ```

12. When testing multiple error scenarios that share the same call pattern, use a parameterised loop rather than duplicating tests:
    ```scala
    Seq(
      ("NotFound",            ErrorResponse(NOT_FOUND,            "Record not found")),
      ("BadRequest",          ErrorResponse(BAD_REQUEST,          "Bad request")),
      ("InternalServerError", ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
    ).foreach { case (errorName, errorResponse) =>
      s"return ${errorResponse.statusCode} when the connector returns $errorName" in {
        when(mockConnector.getData(eqTo(id))(any()))
          .thenReturn(Future.successful(Left(errorResponse)))

        val result = controller.show(id)(fakeRequest)

        status(result)        mustBe errorResponse.statusCode
        contentAsJson(result) mustBe Json.toJson(errorResponse)
      }
    }
    ```

## Connector Tests
13. Connector tests require a real HTTP client and WireMock to stub external calls. Provide this infrastructure via a shared `ConnectorTestHelpers` trait with an inner `ConnectorFixture` class. Specs mix in both:
    ```scala
    class MyConnectorSpec extends SpecBase with ConnectorTestHelpers {
      protected val endpointName = "my-service"  // matches the config key

      "MyConnector must" - {
        "return a result on success" in new SetUp {
          stubGet(url, OK, Json.toJson(expectedResponse).toString)
          whenReady(connector.getData(id)) { result =>
            result mustBe Right(expectedResponse)
            verifyGet(url)
          }
        }
      }

      class SetUp extends ConnectorFixture {
        val connector = appWithHttpClient.injector.instanceOf[MyConnector]
        lazy val url  = appConfig.myServiceUrl(id)  // lazy - resolved after app is built
      }
    }
    ```
    Always use `lazy val` for URLs inside `SetUp` to ensure `appConfig` is resolved after the application is built.

14. Use `whenReady(future) { result => ... }` for asserting async connector results. Do not use `await` directly in connector specs:
    ```scala
    // Do this
    whenReady(connector.getData(id)) { result =>
      result mustBe Right(expectedResponse)
    }

    // Not this
    val result = await(connector.getData(id))
    result mustBe Right(expectedResponse)
    ```

15. Always call a WireMock verify after asserting the result, to confirm the HTTP call was actually made:
    ```scala
    whenReady(connector.getData(id)) { result =>
      result mustBe Right(expectedResponse)
      verifyGet(url)   // confirms the outbound HTTP GET actually happened
    }
    ```
    Your `WireMockHelper` should expose `verifyGet`, `verifyPost`, `verifyPut` etc. wrapping WireMock's verify API:
    ```scala
    def verifyGet(url: String): Unit =
      wireMockServer.verify(getRequestedFor(urlEqualTo(url)))
    ```

16. When testing retry behaviour, provide two app instances in `ConnectorFixture` — one with default config and one with retry config:
    ```scala
    // In ConnectorFixture
    val appWithHttpClient: Application =
      GuiceApplicationBuilder()
        .configure(getWireMockAppConfig(Seq(endpointName)))
        .overrides(bind[HttpClientV2].toInstance(httpClientV2))
        .build()

    val appWithHttpClientAndRetry: Application =
      GuiceApplicationBuilder()
        .configure(getWireMockAppConfigWithRetry(Seq(endpointName)))
        .overrides(bind[HttpClientV2].toInstance(httpClientV2))
        .build()
    ```
    Then expose both from `SetUp`:
    ```scala
    class SetUp extends ConnectorFixture {
      val connector          = appWithHttpClient.injector.instanceOf[MyConnector]
      val connectorWithRetry = appWithHttpClientAndRetry.injector.instanceOf[MyConnector]
      lazy val url           = appConfig.myServiceUrl(id)
    }
    ```
    Assert retry behaviour by verifying the call count. Your `WireMockHelper` should expose count-aware verify helpers:
    ```scala
    // Called exactly once — no retry occurred
    def verifyGetWithoutRetry(url: String): Unit =
      wireMockServer.verify(1, getRequestedFor(urlEqualTo(url)))

    // Called twice — one original + one retry
    def verifyGetWithRetry(url: String): Unit =
      wireMockServer.verify(2, getRequestedFor(urlEqualTo(url)))
    ```
    Use `connectorWithRetry` for all retry-related tests. Use the retry verify helper for 5xx (should retry), and the no-retry verify helper for 4xx (retries should be suppressed):
    ```scala
    "retry on 500" in new SetUp {
      stubGet(url, INTERNAL_SERVER_ERROR, "")
      whenReady(connectorWithRetry.getData(id)) { result =>
        result mustBe Left(unexpectedResponse)
        verifyGetWithRetry(url)     // called twice
      }
    }

    "not retry on 400" in new SetUp {
      stubGet(url, BAD_REQUEST, "")
      whenReady(connectorWithRetry.getData(id)) { result =>
        result mustBe Left(badRequest)
        verifyGetWithoutRetry(url)  // called only once
      }
    }
    ```

17. Test the full set of HTTP scenarios for every connector method. At minimum cover: success (2xx), parse failure (2xx with unparseable body), 400, 404, 422, 500, and network fault. A network fault is simulated with WireMock's `Fault` API. Your `WireMockHelper` should expose a stub helper for it:
    ```scala
    def stubGetFault(url: String): Unit =
      wireMockServer.stubFor(
        WireMock.get(urlEqualTo(url))
          .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
      )
    ```
    Then test it:
    ```scala
    "return an error when a network fault occurs" in new SetUp {
      stubGetFault(url)
      whenReady(connector.getData(id)) { result =>
        result mustBe Left(unexpectedResponse)
        verifyGet(url)
      }
    }
    ```

## Test Data
18. All shared test values should live in a `TestData` trait that is mixed into `SpecBase`. Do not declare test values inline in specs if they are reused across multiple tests or files:
    ```scala
    trait TestData {
      val userId       = "user-123"
      val emailAddress = "test@example.com"
      val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1718118467838L), ZoneId.of("UTC"))

      val sampleRecord = MyRecord(
        id        = userId,
        email     = emailAddress,
        createdAt = Instant.now(clock)
      )
    }
    ```

19. Use a fixed `Clock` from `TestData` for all time-sensitive values. Never use `Instant.now()` or `LocalDate.now()` directly in tests as these produce non-deterministic results:
    ```scala
    // Don't do this
    val record = MyRecord(createdAt = Instant.now())

    // Do this — clock is fixed to a known millisecond in TestData
    val record = MyRecord(createdAt = Instant.now(clock))
    ```

20. When a deterministic UUID is needed, define a fake generator in `SpecBase` that always returns a fixed value, and pass it into any class under test that takes a UUID generator:
    ```scala
    // In SpecBase
    val fakeUUIDGenerator = new UUIDGenerator {
      override def uuid: String = "01234567-89ab-cdef-0123-456789abcdef"
    }

    // In a spec
    val connector = new MyConnector(config, httpClient, fakeUUIDGenerator)
    ```