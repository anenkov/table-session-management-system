package com.nenkov.bar.testsupport;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
class TestProtectedController {

  @GetMapping("/__test/protected")
  Mono<String> protectedEndpoint() {
    return Mono.just("ok");
  }
}
