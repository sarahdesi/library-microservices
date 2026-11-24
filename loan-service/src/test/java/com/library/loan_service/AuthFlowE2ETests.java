package com.library.loan_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.JsonNode;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authentication and authorization end to end (CLAUDE.md 12.9), against a stack started with
 * the university test data. Needs a fresh university registration (2024001 / 11111111111 and
 * MASP 10203040 / 55555555555 not yet used): run it on a disposable stack.
 */
@EnabledIfSystemProperty(named = "e2e", matches = "true")
class AuthFlowE2ETests {

    private static final String PASSWORD = "senha-forte-123";

    private final E2ESupport e2e = new E2ESupport();

    @Test
    void fullFlowStudentAndTeacher() throws Exception {
        String librarian = e2e.librarianToken();

        // without token: 401; internal endpoint: blocked even for the librarian
        assertThat(e2e.send("GET", "/books", null, null).statusCode()).isEqualTo(401);
        assertThat(e2e.send("PATCH", "/books/x/availability", librarian, "{\"change\":-1}").statusCode()).isEqualTo(403);

        // university data that does not match: inactive student
        HttpResponse<String> inactive = register("student", "2019050", "44444444444", email());
        assertThat(inactive.statusCode()).isEqualTo(400);
        assertThat(inactive.body()).contains("University data does not match");

        // a student cannot register as a teacher with their matricula
        assertThat(register("teacher", "2024001", "11111111111", email()).statusCode()).isEqualTo(400);

        // student self-registration: name comes from the university
        String studentEmail = email();
        HttpResponse<String> student = register("student", "2024001", "11111111111", studentEmail);
        assertThat(student.statusCode()).as(student.body()).isEqualTo(201);
        JsonNode studentUser = e2e.json.readTree(student.body());
        assertThat(studentUser.get("name").asString()).isEqualTo("Ana Beatriz Souza");
        long studentId = studentUser.get("id").asLong();

        assertThat(register("student", "2024001", "11111111111", email()).statusCode()).isEqualTo(409);

        // teacher with MASP
        HttpResponse<String> teacher = register("teacher", "10203040", "55555555555", email());
        assertThat(teacher.statusCode()).as(teacher.body()).isEqualTo(201);
        assertThat(e2e.json.readTree(teacher.body()).get("type").asString()).isEqualTo("TEACHER");

        String member = e2e.login(studentEmail, PASSWORD);

        // the member area
        assertThat(e2e.get("/users/me", member).get("id").asLong()).isEqualTo(studentId);
        assertThat(e2e.send("GET", "/books", member, null).statusCode()).isEqualTo(200);
        assertThat(e2e.send("GET", "/loans/me", member, null).body()).isEqualTo("[]");
        assertThat(e2e.send("GET", "/fines/me", member, null).statusCode()).isEqualTo(200);

        // counter operations are not for members
        assertThat(e2e.send("GET", "/users", member, null).statusCode()).isEqualTo(403);
        assertThat(e2e.send("POST", "/loans", member, "{\"userId\":" + studentId + ",\"bookId\":\"x\"}").statusCode())
                .isEqualTo(403);

        // a forged identity header is replaced by the token's
        HttpResponse<String> forged = e2e.send("GET", "/users/me", member, null,
                "X-User-Id", "1", "X-User-Role", "LIBRARIAN");
        assertThat(e2e.json.readTree(forged.body()).get("id").asLong()).isEqualTo(studentId);

        // someone else's data
        long otherMember = e2e.createMember(librarian, "STUDENT");
        assertThat(e2e.send("GET", "/users/" + otherMember, member, null).statusCode()).isEqualTo(403);
        assertThat(e2e.send("GET", "/loans/user/" + otherMember, member, null).statusCode()).isEqualTo(403);

        // the librarian lends, the member renews their own loan but cannot return it
        String bookId = e2e.createBook(librarian, 2);
        HttpResponse<String> loan = e2e.send("POST", "/loans", librarian, "{\"userId\":" + studentId + ",\"bookId\":\"" + bookId + "\"}");
        assertThat(loan.statusCode()).as(loan.body()).isEqualTo(201);
        long loanId = e2e.json.readTree(loan.body()).get("id").asLong();
        HttpResponse<String> otherLoan = e2e.send("POST", "/loans", librarian, "{\"userId\":" + otherMember + ",\"bookId\":\"" + bookId + "\"}");
        long otherLoanId = e2e.json.readTree(otherLoan.body()).get("id").asLong();

        HttpResponse<String> renewed = e2e.send("POST", "/loans/" + loanId + "/renew", member, null);
        assertThat(renewed.statusCode()).as(renewed.body()).isEqualTo(200);
        assertThat(e2e.json.readTree(renewed.body()).get("renewalCount").asInt()).isEqualTo(1);
        assertThat(e2e.send("POST", "/loans/" + otherLoanId + "/renew", member, null).statusCode()).isEqualTo(403);
        assertThat(e2e.send("POST", "/loans/" + loanId + "/return", member, null).statusCode()).isEqualTo(403);
        assertThat(e2e.get("/loans/me", member).size()).isEqualTo(1);

        // the librarian is not a borrower
        long librarianId = e2e.get("/users/me", librarian).get("id").asLong();
        HttpResponse<String> librarianLoan = e2e.send("POST", "/loans", librarian,
                "{\"userId\":" + librarianId + ",\"bookId\":\"" + bookId + "\"}");
        assertThat(librarianLoan.statusCode()).isEqualTo(400);
        assertThat(librarianLoan.body()).contains("Only members can borrow books");

        // clean up the book
        e2e.send("POST", "/loans/" + loanId + "/return", librarian, null);
        e2e.send("POST", "/loans/" + otherLoanId + "/return", librarian, null);
        assertThat(e2e.send("DELETE", "/books/" + bookId, librarian, null).statusCode()).isEqualTo(204);
    }

    @Test
    void servicesAreNotReachableFromOutsideDocker() {
        for (int port : new int[]{8081, 8082, 8083, 8084}) {
            assertThat(isOpen(port)).as("port %d must not be published", port).isFalse();
        }
    }

    private HttpResponse<String> register(String kind, String number, String cpf, String email) throws Exception {
        return e2e.send("POST", "/auth/register/" + kind, null, """
                {"registrationNumber":"%s","cpf":"%s","email":"%s","password":"%s"}
                """.formatted(number, cpf, email, PASSWORD));
    }

    private static String email() {
        return "e2e-" + UUID.randomUUID() + "@test.local";
    }

    private static boolean isOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 300);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
