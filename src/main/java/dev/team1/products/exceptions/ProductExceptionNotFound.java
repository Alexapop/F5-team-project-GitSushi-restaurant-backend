package dev.team1.products.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Product not found")
public class ProductExceptionNotFound extends ProductException {

    public ProductExceptionNotFound(String message) {
        super(message);
    }

    // public ProductExceptionNotFound(String message, Throwable cause) {
    //     super(message, cause);
    // }
}


