package com.projects.golnk.service;

import com.projects.golnk.dto.CreateShortUrlRequest;
import com.projects.golnk.model.ShortUrl;
import com.projects.golnk.repository.ShortUrlRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class UrlService {

    private final ShortUrlRepository repository;
    private final ShortCodeService codeService;

    public UrlService(ShortUrlRepository repository, ShortCodeService codeService) {
        this.repository = repository;
        this.codeService = codeService;
    }

    public ShortUrl create(CreateShortUrlRequest request) {
        String target = normalizeUrl(request.getUrl());
        String code = request.getAlias() != null
                ? codeService.validateCustomAlias(request.getAlias())
                : codeService.generateUniqueCode();

        Instant expiresAt = null;
        if (request.getExpiryDays() != null && request.getExpiryDays() > 0) {
            expiresAt = Instant.now().plus(request.getExpiryDays(), ChronoUnit.DAYS);
        }

        ShortUrl shortUrl = new ShortUrl(code, target, expiresAt);
        return repository.save(shortUrl);
    }

    public Optional<ShortUrl> lookupActive(String code) {
        return repository.findByCode(code)
                .filter(url -> url.getExpiresAt() == null || url.getExpiresAt().isAfter(Instant.now()));
    }

    public void registerHit(ShortUrl url) {
        url.setHits(url.getHits() + 1);
        url.setLastAccessedAt(Instant.now());
        repository.save(url);
    }

    private String normalizeUrl(String input) {
        try {
            URI uri = new URI(input.trim());
            if (uri.getScheme() == null) {
                uri = new URI("https://" + input.trim());
            }
            if (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https")) {
                throw new IllegalArgumentException("Only HTTP and HTTPS are supported");
            }
            return uri.normalize().toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL");
        }
    }
}
