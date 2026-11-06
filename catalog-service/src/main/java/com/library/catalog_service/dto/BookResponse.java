package com.library.catalog_service.dto;

import com.library.catalog_service.model.Book;

public record BookResponse(
        String id,
        String title,
        String author,
        String category,
        String isbn,
        Integer totalCopies,
        Integer availableCopies
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getCategory(),
                book.getIsbn(),
                book.getTotalCopies(),
                book.getAvailableCopies()
        );
    }
}