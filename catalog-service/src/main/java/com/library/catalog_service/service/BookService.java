package com.library.catalog_service.service;


import com.library.catalog_service.dto.AvailabilityRequest;
import com.library.catalog_service.dto.BookResponse;
import com.library.catalog_service.exception.BusinessException;
import com.library.catalog_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import com.library.catalog_service.model.Book;
import org.springframework.stereotype.Service;
import com.library.catalog_service.repository.BookRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

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

        int loanedCopies = book.getTotalCopies() - book.getAvailableCopies();
        if (request.totalCopies() < loanedCopies) {
            throw new BusinessException("Total copies cannot be less than copies on loan (" + loanedCopies + ")");
        }

        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setCategory(request.category());
        book.setIsbn(request.isbn());
        book.setTotalCopies(request.totalCopies());
        book.setAvailableCopies(request.totalCopies() - loanedCopies);
        return BookResponse.from(bookRepository.save(book));
    }

    public void delete(String id) {
        Book book = getBook(id);
        if (book.getAvailableCopies() < book.getTotalCopies()) {
            throw new BusinessException("Cannot delete a book with copies on loan");
        }
        bookRepository.delete(book);
    }

    public BookResponse changeAvailability(String id, AvailabilityRequest request) {
        Book book = getBook(id);
        int newAvailable = book.getAvailableCopies() + request.change();

        if (newAvailable < 0) {
            throw new BusinessException("No copies available");
        }
        if (newAvailable > book.getTotalCopies()) {
            throw new BusinessException("Available copies cannot exceed total copies");
        }

        book.setAvailableCopies(newAvailable);
        return BookResponse.from(bookRepository.save(book));
    }

    private Book getBook(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }
}