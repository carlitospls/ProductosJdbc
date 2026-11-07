package es.daw.productojdbc.repository;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones CRUD comunes a cualquier entidad.
 *
 * @param <T>  tipo de la entidad (Producto, Fabricante...)
 * @param <ID> tipo de su clave primaria (Integer, Long, String...)
 */
// CONTRATO CRUD genérico (T = entidad, ID = tipo de la clave). Sin throws SQLException: JDBC no se filtra hacia arriba.
// Si añades un método a una interfaz, hay que IMPLEMENTARLO en la clase ...Jdbc o no compila.
public interface GenericRepository<T, ID> {

    void save(T entidad);

    Optional<T> findById(ID id);

    List<T> findAll();

    void update(T entidad);

    void delete(ID id);
}
