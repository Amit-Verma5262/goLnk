package com.projects.golnk.controller;

import com.projects.golnk.dto.CreateShortUrlRequest;
import com.projects.golnk.model.ShortUrl;
import com.projects.golnk.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/urls")
public class UrlController {

    private final UrlService service;

    public UrlController(UrlService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ShortUrl> create(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrl result = service.create(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{code}")
    public ResponseEntity<ShortUrl> get(@PathVariable String code) {
        return service.lookupActive(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
