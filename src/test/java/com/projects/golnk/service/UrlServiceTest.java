package com.projects.golnk.service;

import com.projects.golnk.model.ShortUrl;
import com.projects.golnk.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import com.projects.golnk.dto.CreateShortUrlRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private ShortCodeService codeService;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(repository, codeService);
    }

    @Test
    void create_shouldCreateShortUrlWithGeneratedCode() {
        // Arrange
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("https://example.com/test");

        when(codeService.generateUniqueCode())
                .thenReturn("aB3xZ12");

        ShortUrl savedUrl =
                new ShortUrl("aB3xZ12", "https://example.com/test", null);

        when(repository.save(any(ShortUrl.class)))
                .thenReturn(savedUrl);

        // Act
        ShortUrl result = urlService.create(request);

        // Assert
        assertNotNull(result);
        assertEquals("aB3xZ12", result.getCode());
        assertEquals("https://example.com/test", result.getTargetUrl());
        assertNull(result.getExpiresAt());

        verify(codeService).generateUniqueCode();
        verify(repository).save(any(ShortUrl.class));
    }

    @Test
    void create_shouldNormalizeUrl_whenSchemeIsMissing() {
        // Arrange
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("example.com/test");

        when(codeService.generateUniqueCode())
                .thenReturn("abc1234");

        when(repository.save(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShortUrl result = urlService.create(request);

        // Assert
        assertEquals(
                "https://example.com/test",
                result.getTargetUrl()
        );

        assertEquals("abc1234", result.getCode());

        verify(repository).save(any(ShortUrl.class));
    }

    @Test
    void create_shouldUseCustomAlias_whenAliasIsProvided() {
        // Arrange
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("https://example.com");
        request.setAlias("my_link");

        when(codeService.validateCustomAlias("my_link"))
                .thenReturn("my_link");

        when(repository.save(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShortUrl result = urlService.create(request);

        // Assert
        assertEquals("my_link", result.getCode());
        assertEquals("https://example.com", result.getTargetUrl());

        verify(codeService).validateCustomAlias("my_link");
        verify(codeService, never()).generateUniqueCode();
        verify(repository).save(any(ShortUrl.class));
    }

    @Test
    void create_shouldSetExpiryDate_whenExpiryDaysProvided() {
        // Arrange
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("https://example.com");
        request.setExpiryDays(7);

        when(codeService.generateUniqueCode())
                .thenReturn("abc1234");

        when(repository.save(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();

        // Act
        ShortUrl result = urlService.create(request);

        Instant after = Instant.now();

        // Assert
        assertNotNull(result.getExpiresAt());

        assertTrue(
                result.getExpiresAt().isAfter(
                        before.plusSeconds(6 * 24 * 60 * 60)
                )
        );

        assertTrue(
                result.getExpiresAt().isBefore(
                        after.plusSeconds(8 * 24 * 60 * 60)
                )
        );
    }

    @Test
    void create_shouldNotSetExpiryDate_whenExpiryDaysIsZero() {
        // Arrange
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("https://example.com");
        request.setExpiryDays(0);

        when(codeService.generateUniqueCode())
                .thenReturn("abc1234");

        when(repository.save(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShortUrl result = urlService.create(request);

        // Assert
        assertNull(result.getExpiresAt());
    }

    @Test
    void create_shouldThrowException_whenUrlUsesUnsupportedProtocol() {
        // Arrange
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("ftp://example.com/file");

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> urlService.create(request)
                );

        assertEquals(
                "Only HTTP and HTTPS are supported",
                exception.getMessage()
        );

        verifyNoInteractions(repository);
        verifyNoInteractions(codeService);
    }

    @Test
    void create_shouldThrowException_whenUrlIsInvalid() {
        // Arrange
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("http://");

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> urlService.create(request)
        );

        verifyNoInteractions(repository);
        verifyNoInteractions(codeService);
    }

    @Test
    void lookupActive_shouldReturnUrl_whenUrlExistsAndIsNotExpired() {
        // Arrange
        ShortUrl shortUrl =
                new ShortUrl(
                        "abc1234",
                        "https://example.com",
                        Instant.now().plusSeconds(3600)
                );

        when(repository.findByCode("abc1234"))
                .thenReturn(Optional.of(shortUrl));

        // Act
        Optional<ShortUrl> result =
                urlService.lookupActive("abc1234");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("abc1234", result.get().getCode());

        verify(repository).findByCode("abc1234");
    }

    @Test
    void lookupActive_shouldReturnEmpty_whenUrlDoesNotExist() {
        // Arrange
        when(repository.findByCode("unknown"))
                .thenReturn(Optional.empty());

        // Act
        Optional<ShortUrl> result =
                urlService.lookupActive("unknown");

        // Assert
        assertTrue(result.isEmpty());

        verify(repository).findByCode("unknown");
    }

    @Test
    void lookupActive_shouldReturnEmpty_whenUrlIsExpired() {
        // Arrange
        ShortUrl shortUrl =
                new ShortUrl(
                        "abc1234",
                        "https://example.com",
                        Instant.now().minusSeconds(3600)
                );

        when(repository.findByCode("abc1234"))
                .thenReturn(Optional.of(shortUrl));

        // Act
        Optional<ShortUrl> result =
                urlService.lookupActive("abc1234");

        // Assert
        assertTrue(result.isEmpty());

        verify(repository).findByCode("abc1234");
    }

    @Test
    void lookupActive_shouldReturnUrl_whenItNeverExpires() {
        // Arrange
        ShortUrl shortUrl =
                new ShortUrl(
                        "abc1234",
                        "https://example.com",
                        null
                );

        when(repository.findByCode("abc1234"))
                .thenReturn(Optional.of(shortUrl));

        // Act
        Optional<ShortUrl> result =
                urlService.lookupActive("abc1234");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("https://example.com", result.get().getTargetUrl());
    }

    @Test
    void registerHit_shouldIncrementHitsAndUpdateLastAccessedAt() {
        // Arrange
        ShortUrl shortUrl =
                new ShortUrl(
                        "abc1234",
                        "https://example.com",
                        null
                );

        shortUrl.setHits(5);

        Instant before = Instant.now();

        // Act
        urlService.registerHit(shortUrl);

        Instant after = Instant.now();

        // Assert
        assertEquals(6, shortUrl.getHits());
        assertNotNull(shortUrl.getLastAccessedAt());

        assertTrue(
                !shortUrl.getLastAccessedAt().isBefore(before)
        );

        assertTrue(
                !shortUrl.getLastAccessedAt().isAfter(after)
        );

        verify(repository).save(shortUrl);
    }
}