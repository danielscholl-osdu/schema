/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.schema.exception;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AppExceptionHandler {

  @ExceptionHandler(AppException.class)
  public ResponseEntity<Object> handleAppExceptions(AppException e) {
    return this.getErrorResponse(e);
  }

  private ResponseEntity<Object> getErrorResponse(AppException e) {

    String exceptionMsg = Objects.nonNull(e.getOriginalException())
        ? e.getOriginalException().getMessage()
        : e.getError().getMessage();

    Integer errorCode = e.getError().getCode();

    if (errorCode > 499) {
      log.error(exceptionMsg, e.getOriginalException());
    } else {
      log.warn(exceptionMsg, e.getOriginalException());
    }

    return new ResponseEntity<>(e.getError(), HttpStatus.resolve(errorCode));
  }
}
