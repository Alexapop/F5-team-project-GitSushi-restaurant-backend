package dev.team1.contracts;

public interface IGenericEditService<T, S> {

    public S store(T requestDTO);

    public S update(Long id, T requestDTO);

}
