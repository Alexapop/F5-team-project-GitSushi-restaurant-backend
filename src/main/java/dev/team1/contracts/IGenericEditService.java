package dev.team1.contracts;

// C - create DTO, U - update DTO, T - response DTO
public interface IGenericEditService<C, U, T> {

    public T store(C requestDTO);

    public T update(Long id, U requestDTO);

}
