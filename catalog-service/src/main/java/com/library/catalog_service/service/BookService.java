package com.library.catalog_service.service;


import com.library.catalog_service.dto.AvailabilityRequest;
import com.library.catalog_service.dto.BookResponse;
import com.library.catalog_service.exception.BusinessException;
import com.library.catalog_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import com.library.catalog_service.model.Book;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import com.library.catalog_service.repository.BookRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final MongoTemplate mongoTemplate;

    public BookResponse create(com.library.catalog_service.dto.BookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new BusinessException("ISBN already registered");
        }
        Book book = new Book();
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setCategory(request.category());
        book.setIsbn(request.isbn());
        book.setTotalCopies(request.totalCopies());
        book.setAvailableCopies(request.totalCopies());
        return BookResponse.from(bookRepository.save(book));
    }

    public List<BookResponse> findAll() {
        return bookRepository.findAll().stream()
                .map(BookResponse::from)
                .toList();
    }

    public BookResponse findById(String id) {
        return BookResponse.from(getBook(id));
    }

    public List<BookResponse> search(String title, String author, String category) {
        List<Book> books;
        if (title != null) {
            books = bookRepository.findByTitleContainingIgnoreCase(title);
        } else if (author != null) {
            books = bookRepository.findByAuthorContainingIgnoreCase(author);
        } else if (category != null) {
            books = bookRepository.findByCategoryIgnoreCase(category);
        } else {
            throw new BusinessException("Provide title, author or category to search");
        }
        return books.stream()
                .map(BookResponse::from)
                .toList();
    }

    public BookResponse update(String id, com.library.catalog_service.dto.BookRequest request) {
        Book book = getBook(id);

        boolean isbnChanged = !book.getIsbn().equals(request.isbn());
        if (isbnChanged && bookRepository.existsByIsbn(request.isbn())) {
            throw new BusinessException("ISBN already registered");
        }

        // Atomic: only matches while copies on loan (total - available) fit in the new total,
        // and recomputes availableCopies from the values stored at the moment of the update,
        // so a loan or return happening at the same time is not overwritten.
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("id").is(id),
                Criteria.expr(ComparisonOperators.Lte
                        .valueOf(ArithmeticOperators.Subtract.valueOf("totalCopies").subtract("availableCopies"))
                        .lessThanEqualToValue(request.totalCopies()))));
        // availableCopies must be set before totalCopies (each set is a pipeline stage)
        AggregationUpdate update = AggregationUpdate.update()
                .set("availableCopies").toValue(ArithmeticOperators.Subtract
                        .valueOf(ArithmeticOperators.Add.valueOf("availableCopies").add(request.totalCopies()))
                        .subtract("totalCopies"))
                .set("totalCopies").toValue(request.totalCopies())
                .set("title").toValue(request.title())
                .set("author").toValue(request.author())
                .set("category").toValue(request.category())
                .set("isbn").toValue(request.isbn());

        Book updated = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Book.class);
        if (updated == null) {
            Book current = getBook(id);
            int loanedCopies = current.getTotalCopies() - current.getAvailableCopies();
            throw new BusinessException("Total copies cannot be less than copies on loan (" + loanedCopies + ")");
        }
        return BookResponse.from(updated);
    }

    public void delete(String id) {
        // Atomic: only removes the book while no copy is on loan
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("id").is(id),
                Criteria.expr(ComparisonOperators.Eq.valueOf("availableCopies").equalTo("totalCopies"))));
        if (mongoTemplate.remove(query, Book.class).getDeletedCount() == 0) {
            getBook(id);
            throw new BusinessException("Cannot delete a book with copies on loan");
        }
    }

    /**
     * Atomic conditional increment: the filter and the $inc run as one operation in MongoDB,
     * so concurrent requests can never take availableCopies below 0 or above totalCopies.
     */
    public BookResponse changeAvailability(String id, AvailabilityRequest request) {
        int change = request.change();
        if (change == 0) {
            return findById(id);
        }

        Criteria limit = change < 0
                ? Criteria.where("availableCopies").gte(-change)
                : Criteria.expr(ComparisonOperators.Lte
                        .valueOf(ArithmeticOperators.Add.valueOf("availableCopies").add(change))
                        .lessThanEqualTo("totalCopies"));
        Query query = new Query(new Criteria().andOperator(Criteria.where("id").is(id), limit));

        Book updated = mongoTemplate.findAndModify(query, new Update().inc("availableCopies", change),
                FindAndModifyOptions.options().returnNew(true), Book.class);
        if (updated == null) {
            getBook(id);
            throw new BusinessException(change < 0
                    ? "No copies available"
                    : "Available copies cannot exceed total copies");
        }
        return BookResponse.from(updated);
    }

    private Book getBook(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }
}