/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.api.jsonrpc.health;

import static java.util.Collections.singletonMap;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public final class HealthService {

  public static final HealthService ALWAYS_HEALTHY =
      new HealthService(params -> HealthCheckResult.HEALTHY);

  public static final String LIVENESS_PATH = "/liveness";
  public static final String READINESS_PATH = "/readiness";

  private static final int HEALTHY_STATUS_CODE = HttpResponseStatus.OK.code();
  private static final int UNHEALTHY_STATUS_CODE = HttpResponseStatus.SERVICE_UNAVAILABLE.code();
  private static final int BAD_REQUEST_STATUS_CODE = HttpResponseStatus.BAD_REQUEST.code();
  private static final String HEALTHY_STATUS_TEXT = "UP";
  private static final String UNHEALTHY_STATUS_TEXT = "DOWN";
  private static final String BAD_REQUEST_STATUS_TEXT = "BAD_REQUEST";

  private final HealthCheck healthCheck;

  public HealthService(final HealthCheck healthCheck) {
    this.healthCheck = healthCheck;
  }

  public void handleRequest(final RoutingContext routingContext) {
    final int statusCode;
    final String statusText;
    final HealthCheckResult result =
        healthCheck.check(name -> routingContext.queryParams().get(name));
    switch (result) {
      case HEALTHY:
        statusCode = HEALTHY_STATUS_CODE;
        statusText = HEALTHY_STATUS_TEXT;
        break;
      case BAD_REQUEST:
        statusCode = BAD_REQUEST_STATUS_CODE;
        statusText = BAD_REQUEST_STATUS_TEXT;
        break;
      default:
        statusCode = UNHEALTHY_STATUS_CODE;
        statusText = UNHEALTHY_STATUS_TEXT;
        break;
    }
    final HttpServerResponse response = routingContext.response();
    if (!response.closed()) {
      response
          .setStatusCode(statusCode)
          .end(new JsonObject(singletonMap("status", statusText)).encodePrettily());
    }
  }

  @FunctionalInterface
  public interface HealthCheck {
    HealthCheckResult check(ParamSource paramSource);
  }

  @FunctionalInterface
  public interface ParamSource {
    String getParam(String name);
  }
}
