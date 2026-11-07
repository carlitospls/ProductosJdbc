package es.daw.productojdbc.repository;

import es.daw.productojdbc.exception.AccesoDatosException;
import es.daw.productojdbc.model.Fabricante;
import es.daw.productojdbc.model.Producto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// CAPA DE DATOS (la ÚNICA que escribe SQL). Aquí vive todo el JDBC de productos.
// Cada método sigue SIEMPRE el mismo patrón de 6 pasos (míralo en findById):
//   1) abrir Connection  2) preparar PreparedStatement con ?  3) rellenar los ?
//   4) ejecutar  5) recorrer ResultSet (si es SELECT)  6) cerrar (try-with-resources lo hace solo).
// Si no sabes cómo hacer algo nuevo: COPIA un método parecido de este archivo y cámbiale el SQL.
@Repository
@RequiredArgsConstructor
public class ProductoRepositoryJdbc implements ProductoRepository {

    /*
     * SELECT común a findById y findAll.
     * Hacemos JOIN con fabricante para traer también su nombre y poder construir
     * el objeto Fabricante dentro de cada Producto.
     * Los alias (f_codigo, f_nombre) evitan la ambigüedad: las dos tablas tienen "codigo" y "nombre".
     */
    // >>> EXAMEN [COLUMNA NUEVA]: añade aquí la columna al SELECT (p.stock). Si el nombre choca con
    //     otra tabla, ponle alias (AS) y lee ese alias en mapRow. Este SELECT lo usan findById y findAll.
    private static final String SELECT_BASE = """
            SELECT p.codigo, p.nombre, p.precio,
                   f.codigo AS f_codigo, f.nombre AS f_nombre
            FROM producto p
            JOIN fabricante f ON p.codigo_fabricante = f.codigo
            """;

    private final DataSource dataSource;

    // Spring crea el DataSource a partir de application.properties y lo inyecta aquí
//    public ProductoRepositoryJdbc(DataSource dataSource) {
//        this.dataSource = dataSource;
//    }

    // ---------------------------------------------------------------- READ (uno)
    @Override
    // UN registro: devuelve Optional (puede no existir). El ? se rellena con ps.setInt(1, ...).
    // El número es la POSICIÓN del ? en el SQL (empieza en 1, no en 0).
    public Optional<Producto> findById(Integer codigo) {
        String sql = SELECT_BASE + "WHERE p.codigo = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();

                //---------------------------
                // si devuelve una fila, es que ha encontrado el producto por código
//                if (rs.next()) {
//                    Fabricante fabricante = new Fabricante(
//                            rs.getInt("f_codigo"),
//                            rs.getString("f_nombre"));
//
//                    Producto producto=  new Producto(
//                            rs.getInt("codigo"),
//                            rs.getString("nombre"),
//                            rs.getDouble("precio"),
//                            fabricante);
//
//                    return Optional.of(producto);
//                }else{ // no ha encontrado el producto por código
//                    return Optional.empty();
//                }
                //---------------------------

            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar el producto " + codigo, e);
        }
    }

    // ---------------------------------------------------------------- READ (todos)
    @Override
    // VARIOS registros: while (rs.next()) recorre fila a fila y mapRow convierte cada fila en un Producto.
    // >>> EXAMEN [CONSULTA NUEVA que devuelve lista]: copia este método, añade WHERE ... = ? a la SQL y
    //     un ps.setX(1, parametro). Ejemplo: findByFabricanteCodigo (Reto 2).
    public List<Producto> findAll() {
        String sql = SELECT_BASE + "ORDER BY p.codigo";
        List<Producto> productos = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(mapRow(rs));

            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar los productos", e);
        }
        return productos;
    }

    // ---------------------------------------------------------------- CREATE
    @Override
    // >>> EXAMEN [COLUMNA NUEVA]: 3 sitios en este método -> (1) la lista de columnas del INSERT,
    //     (2) un ? más en VALUES, (3) un ps.setX(n, ...) más. El orden de los ? debe coincidir con el de las columnas.
    public void save(Producto producto) {
        String sql = "INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES (?, ?, ?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, producto.getNombre());
            ps.setDouble(2, producto.getPrecio());
            ps.setInt(3, producto.getFabricante().getCodigo());
            ps.executeUpdate();

            // Recuperamos el código que ha generado AUTO_INCREMENT y se lo asignamos al objeto
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    producto.setCodigo(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al guardar el producto", e);
        }
    }

    // ---------------------------------------------------------------- UPDATE
    @Override
    // >>> EXAMEN [COLUMNA NUEVA]: añade 'columna = ?' al SET y un ps.setX en su posición.
    //     OJO: el ? del WHERE codigo = ? es el ÚLTIMO; si añades uno antes, hay que renumerarlo.
    public void update(Producto producto) {
        String sql = "UPDATE producto SET nombre = ?, precio = ?, codigo_fabricante = ? WHERE codigo = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, producto.getNombre());
            ps.setDouble(2, producto.getPrecio());
            ps.setInt(3, producto.getFabricante().getCodigo());
            ps.setInt(4, producto.getCodigo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al actualizar el producto " + producto.getCodigo(), e);
        }
    }

    // ---------------------------------------------------------------- DELETE
    @Override
    public void delete(Integer codigo) {
        String sql = "DELETE FROM producto WHERE codigo = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al borrar el producto " + codigo, e);
        }
    }

    // ---------------------------------------------------------------- Consulta propia
    @Override
    // Consulta de EXISTENCIA: SELECT 1 ... y devolver rs.next() (true si hay al menos una fila).
    // >>> EXAMEN [REGLA NUEVA]: es la plantilla para 'comprobar si ya existe algo' (ej.: nombre duplicado).
    public boolean existsByFabricanteCodigo(Integer codigoFabricante) {
        String sql = "SELECT 1 FROM producto WHERE codigo_fabricante = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigoFabricante);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();   // con que haya una fila, basta
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al comprobar los productos del fabricante " + codigoFabricante, e);
        }
    }

    // ---------------------------------------------------------------- Mapeo fila → objeto
    // MAPEO fila -> objeto. Se usa en findById y findAll.
    // >>> EXAMEN [COLUMNA NUEVA]: lee la columna con rs.getX("nombre_o_alias") y pásala al constructor
    //     en el MISMO ORDEN que los campos de la clase Producto (@AllArgsConstructor sigue el orden de los campos).
    private Producto mapRow(ResultSet rs) throws SQLException {
        Fabricante fabricante = new Fabricante(
                rs.getInt("f_codigo"),
                rs.getString("f_nombre"));

        return new Producto(
                rs.getInt("codigo"),
                rs.getString("nombre"),
                rs.getDouble("precio"),
                fabricante);
    }
}