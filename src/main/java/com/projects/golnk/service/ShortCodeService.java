package com.projects.golnk.service;

import com.projects.golnk.repository.ShortUrlRepository;
import com.projects.golnk.util.Base62;
import org.springframework.stereotype.Service;

@Service
public class ShortCodeService {

    private static final int DEFAULT_LENGTH = 7;
    private final ShortUrlRepository repository;

    public ShortCodeService(ShortUrlRepository repository) {
        this.repository = repository;
    }

    public String generateUniqueCode() {
        for (int i = 0; i < 10; i++) {
            String candidate = Base62.randomCode(DEFAULT_LENGTH);
            if (!repository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate unique short code");
    }

    public String validateCustomAlias(String alias) {
        if (alias == null || alias.isBlank()) return null;
        if (!alias.matches("^[A-Za-z0-9_]{3,32}$")) {
            throw new IllegalArgumentException("Alias must be 3-32 characters long and contain only letters, digits, or underscores");
        }
        if (repository.existsByCode(alias)) {
            throw new IllegalArgumentException("Alias already exists");
        }
        return alias;
    }
}
