package com.library.loan_service.client;


import com.library.loan_service.dto.AvailabilityChange;
import com.library.loan_service.exception.BusinessException;
import com.library.loan_service.exception.ErrorResponse;
import com.library.loan_service.exception.ResourceNotFoundException;
import com.library.loan_service.exception.ServiceUnavailableException;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class BookClient {

    private final RestClient restClient;

    /**
     * Atomic in the catalog: -1 fails with "No copies available" when there is no copy left,
     * +1 fails when it would exceed the total copies.
     */
    public void changeAvailability(String bookId, int change){
        try{
            restClient.patch()
                    .uri("http://CATALOG-SERVICE/books/{id}/availability", bookId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AvailabilityChange(change))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e){
            throw new ResourceNotFoundException("Book not found");
        } catch (HttpClientErrorException.BadRequest e){
            throw new BusinessException(catalogMessage(e));
        } catch (RestClientException | IllegalStateException e){
            // IllegalStateException: the load balancer has no instance registered in Eureka
            throw new ServiceUnavailableException("Catalog service unavailable");
        }
    }

    // Keeps the catalog's own message (e.g. "No copies available") instead of assuming one
    private static String catalogMessage(HttpClientErrorException e) {
        try {
            ErrorResponse body = e.getResponseBodyAs(ErrorResponse.class);
            if (body != null && body.message() != null) {
                return body.message();
            }
        } catch (RuntimeException ignored) {
            // body is not in the expected format
        }
        return "No copies available";
    }
}
