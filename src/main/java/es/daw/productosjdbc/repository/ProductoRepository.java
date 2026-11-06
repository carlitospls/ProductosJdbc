package es.daw.productosjdbc.repository;

import es.daw.productosjdbc.model.Producto;
import es.daw.productosjdbc.repository.GenericRepository;

/**
 * Repositorio de productos: hereda el CRUD fijando T = Producto, ID = Integer
 * y añade las consultas propias de productos.
 */
public interface ProductoRepository extends GenericRepository<Producto, Integer> {

    /** ¿Hay algún producto de este fabricante? Lo usa FabricanteService antes de borrar. */
    boolean existsByFabricanteCodigo(Integer codigoFabricante);
}