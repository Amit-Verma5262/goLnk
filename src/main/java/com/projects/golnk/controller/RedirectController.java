package com.projects.golnk.controller;

import com.projects.golnk.service.UrlService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final UrlService service;

    public RedirectController(UrlService service) {
        this.service = service;
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return service.lookupActive(code)
                .map(url -> {
                    service.registerHit(url);
                    return ResponseEntity.status(302)
                            .header(HttpHeaders.LOCATION, url.getTargetUrl())
                            .<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
