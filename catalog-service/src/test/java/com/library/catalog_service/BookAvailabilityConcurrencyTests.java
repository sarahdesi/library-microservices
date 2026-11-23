package com.library.catalog_service;

import com.library.catalog_service.model.Book;
import com.library.catalog_service.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces race conditions on availableCopies. Each scenario runs several rounds
 * because a race only shows up when requests actually interleave.
 */
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
class BookAvailabilityConcurrencyTests {

    private static final int ROUNDS = 5;
    private static final int THREADS = 10;

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @LocalServerPort
    int port;

    @Autowired
    BookRepository bookRepository;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void clean() {
        bookRepository.deleteAll();
    }

    @Test
    void concurrentDecrementsOnLastCopyLetOnlyOneThrough() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            Book book = saveBook(1, 1);

            List<HttpResponse<String>> responses = runConcurrently(THREADS, () -> patch(book.getId(), -1));

            assertThat(responses).filteredOn(r -> r.statusCode() == 200).hasSize(1);
            assertThat(responses).filteredOn(r -> r.statusCode() == 400).hasSize(THREADS - 1)
                    .allSatisfy(r -> assertThat(r.body()).contains("No copies available"));
            assertThat(reload(book).getAvailableCopies()).isZero();
        }
    }

    @Test
    void concurrentIncrementsNeverExceedTotalCopies() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            // 3 copies, 2 on loan: only two returns can be accepted
            Book book = saveBook(3, 1);

            List<HttpResponse<String>> responses = runConcurrently(THREADS, () -> patch(book.getId(), 1));

            assertThat(responses).filteredOn(r -> r.statusCode() == 200).hasSize(2);
            assertThat(responses).filteredOn(r -> r.statusCode() == 400).hasSize(THREADS - 2);
            assertThat(reload(book).getAvailableCopies()).isEqualTo(3);
        }
    }

    @Test
    void concurrentEditDoesNotOverwriteAvailability() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            Book book = saveBook(5, 5);
            String body = """
                    {"title":"Edited","author":"Author","category":"Category","isbn":"%s","totalCopies":5}
                    """.formatted(book.getIsbn());

            // 4 loans and 4 edits (same total) at the same time
            List<HttpResponse<String>> responses = runConcurrently(8, i -> i % 2 == 0
                    ? patch(book.getId(), -1)
                    : send(HttpRequest.newBuilder(uri("/books/" + book.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body)).build()));

            assertThat(responses).allSatisfy(r -> assertThat(r.statusCode()).isEqualTo(200));
            Book reloaded = reload(book);
            assertThat(reloaded.getAvailableCopies()).isEqualTo(1);
            assertThat(reloaded.getTotalCopies()).isEqualTo(5);
        }
    }

    @Test
    void bookIsNeverDeletedWhileACopyIsBeingLoaned() throws Exception {
        for (int round = 0; round < ROUNDS * 4; round++) {
            Book book = saveBook(1, 1);

            List<HttpResponse<String>> responses = runConcurrently(2, i -> i == 0
                    ? patch(book.getId(), -1)
                    : send(HttpRequest.newBuilder(uri("/books/" + book.getId())).DELETE().build()));

            boolean loaned = responses.get(0).statusCode() == 200;
            boolean deleted = responses.get(1).statusCode() == 204;
            assertThat(loaned && deleted)
                    .as("a copy was loaned and the book was deleted in the same round")
                    .isFalse();
            if (loaned) {
                assertThat(bookRepository.findById(book.getId())).isPresent();
            }
        }
    }

    // ---------- helpers ----------

    private Book saveBook(int total, int available) {
        Book book = new Book();
        book.setTitle("Book");
        book.setAuthor("Author");
        book.setCategory("Category");
        book.setIsbn(randomIsbn13());
        book.setTotalCopies(total);
        book.setAvailableCopies(available);
        return bookRepository.save(book);
    }

    /** Valid ISBN-13 (978 prefix + 9 random digits + check digit). */
    private static String randomIsbn13() {
        StringBuilder digits = new StringBuilder("978");
        for (int i = 0; i < 9; i++) {
            digits.append(ThreadLocalRandom.current().nextInt(10));
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (digits.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return digits.append((10 - sum % 10) % 10).toString();
    }

    private Book reload(Book book) {
        return bookRepository.findById(book.getId()).orElseThrow();
    }

    private HttpResponse<String> patch(String id, int change) throws Exception {
        return send(HttpRequest.newBuilder(uri("/books/" + id + "/availability"))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"change\":" + change + "}"))
                .build());
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private interface IndexedTask<T> {
        T call(int index) throws Exception;
    }

    private static <T> List<T> runConcurrently(int threads, Callable<T> task) throws Exception {
        return runConcurrently(threads, i -> task.call());
    }

    /** Starts all tasks at the same instant and returns results in submission order. */
    private static <T> List<T> runConcurrently(int threads, IndexedTask<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call(index);
                }));
            }
            ready.await();
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}
