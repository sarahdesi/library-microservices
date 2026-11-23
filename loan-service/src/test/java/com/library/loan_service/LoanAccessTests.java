package com.library.loan_service;

import com.library.loan_service.client.BookClient;
import com.library.loan_service.client.UserClient;
import com.library.loan_service.dto.UserData;
import com.library.loan_service.model.Fine;
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

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ownership rules. Requests carry the X-User-Id / X-User-Role headers the gateway would set;
 * the role matrix itself is tested in the api-gateway.
 */
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
class LoanAccessTests {

    private static final long ANA = 10L;
    private static final long BRUNO = 20L;
    private static final long LIBRARIAN = 1L;

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
    private Loan anaLoan;
    private Loan brunoLoan;

    @BeforeEach
    void setUp() {
        fineRepository.deleteAll();
        loanRepository.deleteAll();
        when(userClient.getUser(anyLong()))
                .thenAnswer(inv -> new UserData(inv.getArgument(0), "User", "user@test.com", "STUDENT", "MEMBER"));
        anaLoan = saveLoan(ANA);
        brunoLoan = saveLoan(BRUNO);
        Fine fine = new Fine();
        fine.setLoanId(brunoLoan.getId());
        fine.setUserId(BRUNO);
        fine.setDaysLate(2);
        fine.setAmount(new BigDecimal("4.00"));
        fine.setPaid(false);
        fineRepository.save(fine);
    }

    // ---------- renewal ----------

    @Test
    void memberRenewsOwnLoan() throws Exception {
        HttpResponse<String> response = send("POST", "/loans/" + anaLoan.getId() + "/renew", member(ANA));

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(reload(anaLoan).getRenewalCount()).isEqualTo(1);
    }

    @Test
    void memberCannotRenewSomeoneElsesLoan() throws Exception {
        HttpResponse<String> response = send("POST", "/loans/" + brunoLoan.getId() + "/renew", member(ANA));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("Access denied");
        assertThat(reload(brunoLoan).getRenewalCount()).isZero();
    }

    @Test
    void librarianRenewsAnyLoan() throws Exception {
        assertThat(send("POST", "/loans/" + brunoLoan.getId() + "/renew", librarian()).statusCode()).isEqualTo(200);
    }

    // ---------- reading ----------

    @Test
    void meRoutesReturnOnlyTheCallersData() throws Exception {
        HttpResponse<String> loans = send("GET", "/loans/me", member(ANA));
        HttpResponse<String> pending = send("GET", "/loans/me/pending", member(BRUNO));
        HttpResponse<String> fines = send("GET", "/fines/me", member(BRUNO));

        assertThat(loans.statusCode()).isEqualTo(200);
        assertThat(loans.body()).contains("\"id\":" + anaLoan.getId()).doesNotContain("\"id\":" + brunoLoan.getId());
        assertThat(pending.body()).contains("\"hasPendingFines\":true");
        assertThat(fines.body()).contains("\"userId\":" + BRUNO);
        assertThat(send("GET", "/fines/me", member(ANA)).body()).isEqualTo("[]");
    }

    @Test
    void meRoutesRequireALoggedInUser() throws Exception {
        assertThat(send("GET", "/loans/me", new String[0]).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/fines/me", new String[0]).statusCode()).isEqualTo(401);
    }

    @Test
    void memberCannotReadSomeoneElsesData() throws Exception {
        String[] ana = member(ANA);
        assertThat(send("GET", "/loans/user/" + BRUNO, ana).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/loans/user/" + BRUNO + "/pending", ana).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/fines/user/" + BRUNO, ana).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/loans/" + brunoLoan.getId(), ana).statusCode()).isEqualTo(403);

        assertThat(send("GET", "/loans/user/" + ANA, ana).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/loans/" + anaLoan.getId(), ana).statusCode()).isEqualTo(200);
    }

    @Test
    void librarianReadsAnyonesData() throws Exception {
        assertThat(send("GET", "/loans/user/" + BRUNO, librarian()).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/fines/user/" + BRUNO, librarian()).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/loans/" + anaLoan.getId(), librarian()).statusCode()).isEqualTo(200);
    }

    // ---------- borrowing ----------

    @Test
    void librarianCannotBorrowBooks() throws Exception {
        when(userClient.getUser(LIBRARIAN))
                .thenReturn(new UserData(LIBRARIAN, "Bibliotecária", "lib@test.com", null, "LIBRARIAN"));

        HttpResponse<String> response = http.send(HttpRequest.newBuilder(uri("/loans"))
                        .header("Content-Type", "application/json")
                        .headers(librarian())
                        .POST(HttpRequest.BodyPublishers.ofString("{\"userId\":" + LIBRARIAN + ",\"bookId\":\"b1\"}")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("Only members can borrow books");
        verify(bookClient, never()).changeAvailability(anyString(), anyInt());
    }

    // ---------- helpers ----------

    private static String[] member(long id) {
        return new String[]{"X-User-Id", String.valueOf(id), "X-User-Role", "MEMBER"};
    }

    private static String[] librarian() {
        return new String[]{"X-User-Id", String.valueOf(LIBRARIAN), "X-User-Role", "LIBRARIAN"};
    }

    private Loan saveLoan(long userId) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setBookId("book-" + userId);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(7));
        loan.setRenewalCount(0);
        loan.setStatus(LoanStatus.ACTIVE);
        return loanRepository.save(loan);
    }

    private Loan reload(Loan loan) {
        return loanRepository.findById(loan.getId()).orElseThrow();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> send(String method, String path, String[] headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).method(method, HttpRequest.BodyPublishers.noBody());
        if (headers.length > 0) {
            builder.headers(headers);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
