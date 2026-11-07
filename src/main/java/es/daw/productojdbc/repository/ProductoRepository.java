package es.daw.productojdbc.repository;

import es.daw.productojdbc.model.Producto;

/**
 * Repositorio de productos: hereda el CRUD fijando T = Producto, ID = Integer
 * y añade las consultas propias de productos.
 */
// >>> EXAMEN [CONSULTA NUEVA]: declara aquí (sin cuerpo) el método nuevo y luego impleméntalo en ProductoRepositoryJdbc.
public interface ProductoRepository extends GenericRepository<Producto, Integer> {

    /** ¿Hay algún producto de este fabricante? Lo usa FabricanteService antes de borrar. */
    boolean existsByFabricanteCodigo(Integer codigoFabricante);
}
