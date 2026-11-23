package com.library.loan_service;

import com.library.loan_service.client.BookClient;
import com.library.loan_service.client.UserClient;
import com.library.loan_service.dto.UserData;
import com.library.loan_service.model.Loan;
import com.library.loan_service.model.LoanStatus;
import com.library.loan_service.repository.FineRepository;
import com.library.loan_service.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Race conditions inside the loan-service. The user-service and catalog-service clients
 * are mocked; changing availability takes a few milliseconds, like a real network call.
 * Atomicity of availableCopies itself is tested in the catalog-service.
 */
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
class LoanConcurrencyTests {

    private static final int ROUNDS = 5;

    @Container
    @ServiceConnection
    static MariaDBContainer mariadb = new MariaDBContainer("mariadb:11");

    @LocalServerPort
    int port;

    @Autowired
    LoanRepository loanRepository;

    @Autowired
    FineRepository fineRepository;

    @MockitoBean
    UserClient userClient;

    @MockitoBean
    BookClient bookClient;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() {
        fineRepository.deleteAll();
        loanRepository.deleteAll();
        when(userClient.getUser(anyLong()))
                .thenAnswer(inv -> new UserData(inv.getArgument(0), "User", "user@test.com", "STUDENT", "MEMBER"));
        doAnswer(inv -> {
            Thread.sleep(20);
            return null;
        }).when(bookClient).changeAvailability(anyString(), anyInt());
    }

    // ---------- returns ----------

    @Test
    void concurrentReturnsOfSameLoanHaveOneWinner() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            clearInvocations(bookClient);
            Loan loan = saveLoan(1L, uniqueBookId(), LocalDate.now().minusDays(3));

            List<HttpResponse<String>> responses =
                    ConcurrentRunner.run(2, i -> post("/loans/" + loan.getId() + "/return", null));

            assertThat(responses).filteredOn(r -> r.statusCode() == 200).hasSize(1);
            assertThat(responses).filteredOn(r -> r.statusCode() == 400).hasSize(1)
                    .allSatisfy(r -> assertThat(r.body()).contains("Loan already returned"));
            // only the winner gives the copy back and only one fine is generated
            verify(bookClient, times(1)).changeAvailability(loan.getBookId(), 1);
            assertThat(fineRepository.findByUserId(1L).stream()
                    .filter(f -> f.getLoanId().equals(loan.getId()))).hasSize(1);
            assertThat(reload(loan).getStatus()).isEqualTo(LoanStatus.RETURNED);
        }
    }

    @Test
    void renewalRacingWithReturnDoesNotReactivateLoan() throws Exception {
        for (int round = 0; round < ROUNDS * 2; round++) {
            Loan loan = saveLoan(1L, uniqueBookId(), LocalDate.now().plusDays(7));

            List<HttpResponse<String>> responses = ConcurrentRunner.run(2, i -> i == 0
                    ? post("/loans/" + loan.getId() + "/return", null)
                    : post("/loans/" + loan.getId() + "/renew", null));

            assertThat(responses.get(0).statusCode()).isEqualTo(200);
            assertThat(reload(loan).getStatus())
                    .as("loan returned (copy given back) must stay RETURNED")
                    .isEqualTo(LoanStatus.RETURNED);
        }
    }

    @Test
    void concurrentRenewalsAreAllApplied() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            LocalDate dueDate = LocalDate.now().plusDays(7);
            Loan loan = saveLoan(1L, uniqueBookId(), dueDate);

            List<HttpResponse<String>> responses =
                    ConcurrentRunner.run(3, i -> post("/loans/" + loan.getId() + "/renew", null));

            assertThat(responses).allSatisfy(r -> assertThat(r.statusCode()).isEqualTo(200));
            Loan reloaded = reload(loan);
            assertThat(reloaded.getRenewalCount()).isEqualTo(3);
            assertThat(reloaded.getDueDate()).isEqualTo(dueDate.plusDays(3 * 7));
        }
    }

    // ---------- new loans from the same user ----------

    @Test
    void sameUserCannotExceedLoanLimit() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            long userId = 100L + round;

            List<HttpResponse<String>> responses =
                    ConcurrentRunner.run(6, i -> post("/loans", loanBody(userId, "book-" + i)));

            assertThat(responses).filteredOn(r -> r.statusCode() == 201).hasSize(3);
            assertThat(responses).filteredOn(r -> r.statusCode() == 400).hasSize(3)
                    .allSatisfy(r -> assertThat(r.body()).contains("Loan limit reached (3 books)"));
            assertThat(loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE)).isEqualTo(3);
        }
    }

    @Test
    void sameUserCannotTakeTwoCopiesOfSameBook() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            long userId = 200L + round;
            String bookId = "book-" + round;

            List<HttpResponse<String>> responses =
                    ConcurrentRunner.run(5, i -> post("/loans", loanBody(userId, bookId)));

            assertThat(responses).filteredOn(r -> r.statusCode() == 201).hasSize(1);
            assertThat(responses).filteredOn(r -> r.statusCode() == 400).hasSize(4)
                    .allSatisfy(r -> assertThat(r.body()).contains("User already has a copy of this book"));
            assertThat(loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE)).isEqualTo(1);
        }
    }

    // ---------- compensation ----------

    @Test
    void reservedCopyIsReleasedWhenSavingLoanFails() throws Exception {
        // bookId longer than the column (255): the INSERT fails after the copy was reserved
        String tooLongBookId = "x".repeat(300);

        HttpResponse<String> response = post("/loans", loanBody(1L, tooLongBookId));

        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(loanRepository.count()).isZero();
        verify(bookClient).changeAvailability(tooLongBookId, -1);
        verify(bookClient).changeAvailability(tooLongBookId, 1);
    }

    @Test
    void copyIsNotReservedWhenLoanIsRejected() throws Exception {
        saveLoan(1L, "book-a", LocalDate.now().plusDays(7));

        HttpResponse<String> response = post("/loans", loanBody(1L, "book-a"));

        assertThat(response.statusCode()).isEqualTo(400);
        verify(bookClient, never()).changeAvailability(eq("book-a"), anyInt());
    }

    // ---------- helpers ----------

    private Loan saveLoan(long userId, String bookId, LocalDate dueDate) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setBookId(bookId);
        loan.setLoanDate(dueDate.minusDays(7));
        loan.setDueDate(dueDate);
        loan.setRenewalCount(0);
        loan.setStatus(LoanStatus.ACTIVE);
        return loanRepository.save(loan);
    }

    private static String uniqueBookId() {
        return "book-" + UUID.randomUUID();
    }

    private Loan reload(Loan loan) {
        return loanRepository.findById(loan.getId()).orElseThrow();
    }

    private static String loanBody(long userId, String bookId) {
        return "{\"userId\":" + userId + ",\"bookId\":\"" + bookId + "\"}";
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        builder = json == null
                ? builder.POST(HttpRequest.BodyPublishers.noBody())
                : builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
