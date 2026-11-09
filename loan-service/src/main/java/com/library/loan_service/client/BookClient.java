package com.library.loan_service.client;


import com.library.loan_service.dto.AvailabilityChange;
import com.library.loan_service.dto.BookData;
import com.library.loan_service.exception.BusinessException;
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

    public BookData getBook(String bookId){
        try{
            return restClient.get()
                    .uri("http://CATALOG-SERVICE/books/{id}", bookId)
                    .retrieve()
                    .body(BookData.class);
        } catch(HttpClientErrorException.NotFound e){
            throw new ResourceNotFoundException("Book not found");
        } catch(RestClientException e){
            throw new ServiceUnavailableException("Catalog service unavailable");
        }
    }
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
            throw new BusinessException("No copies available");
        } catch (RestClientException e){
            throw new ServiceUnavailableException("Catalog service unavailable");
        }
    }
}
