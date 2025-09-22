/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connector

import base.UnitSpec
import config.FrontendAppConfig
import connectors.VapingDutyConnectorHttp
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import play.api.http.Status.*
import play.api.mvc.Results
import uk.gov.hmrc.http as StringContextOps
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class VapingDutyConnectorHttpSpec extends UnitSpec
  with Results
  with ScalaFutures {
  
  "ping" - {
    val mockUrl = s"http://vaping-duty/ping"

    "successfully retrieve obligation details" in new Setup {
      val httpResponse           = HttpResponse(OK)

      when(mockConfig.vdrPingUrl()).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(httpResponse)))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.ping()) { _ =>
        verify(connector.httpClient, times(1))
          .get(eqTo(url"$mockUrl"))(any())

        verify(requestBuilder, times(1))
          .execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "fail when an authorisation fails" in new Setup {
      val authorisationErrorResponse = Future.successful(
        Left[UpstreamErrorResponse, HttpResponse](UpstreamErrorResponse("", UNAUTHORIZED, UNAUTHORIZED, Map.empty))
      )

      when(mockConfig.vdrPingUrl()).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(authorisationErrorResponse)

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.ping().failed) { e =>
        e.getMessage must include("Not authorised")

        verify(connector.httpClient, times(1))
          .get(eqTo(url"$mockUrl"))(any())

        verify(requestBuilder, times(1))
          .execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }
    
    "fail when an unexpected response is returned" in new Setup {
      val upstreamErrorResponse = Future.successful(
        Left[UpstreamErrorResponse, HttpResponse](UpstreamErrorResponse("", BAD_GATEWAY, BAD_GATEWAY, Map.empty))
      )

      when(mockConfig.vdrPingUrl()).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(upstreamErrorResponse)

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.ping().failed) { e =>
        e.getMessage must include("Unexpected response")

        verify(connector.httpClient, times(1))
          .get(eqTo(url"$mockUrl"))(any())

        verify(requestBuilder, times(1))
          .execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "fail when unexpected status code returned" in new Setup {
      val invalidStatusCodeResponse = HttpResponse(CREATED, "")

      when(mockConfig.vdrPingUrl()).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(invalidStatusCodeResponse)))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.ping().failed) { e =>
        e.getMessage mustBe "Unexpected status code: 201"

        verify(connector.httpClient, times(1))
          .get(eqTo(url"$mockUrl"))(any())

        verify(requestBuilder, times(1))
          .execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }
  }

  class Setup {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val hc: HeaderCarrier    = HeaderCarrier()
    
    val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
    val connector                     = new VapingDutyConnectorHttp(config = mockConfig, httpClient = mock[HttpClientV2])

    val requestBuilder: RequestBuilder = mock[RequestBuilder]
  }
}
