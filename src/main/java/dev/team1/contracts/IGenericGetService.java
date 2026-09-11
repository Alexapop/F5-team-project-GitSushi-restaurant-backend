package dev.team1.contracts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IGenericGetService<T> {

    public Page<T> getAll(Pageable pageable);
    public T getById(Long id);

}
