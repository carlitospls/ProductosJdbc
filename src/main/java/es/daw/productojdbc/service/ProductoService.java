package es.daw.productojdbc.service;

import es.daw.productojdbc.exception.RecursoNoEncontradoException;
import es.daw.productojdbc.model.Fabricante;
import es.daw.productojdbc.model.Producto;
import es.daw.productojdbc.repository.FabricanteRepository;
import es.daw.productojdbc.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// CAPA DE LÓGICA: reglas de negocio. NO sabe nada de HTTP/Model/vistas ni de SQL.
// Habla solo con las INTERFACES de repositorio; el controlador habla solo con servicios.
@Service
@RequiredArgsConstructor
public class ProductoService {

    // Dependemos de las INTERFACES, no de las clases ...Jdbc
    private final ProductoRepository productoRepository;
    private final FabricanteRepository fabricanteRepository;

//    public ProductoService(ProductoRepository productoRepository,
//                           FabricanteRepository fabricanteRepository) {
//        this.productoRepository = productoRepository;
//        this.fabricanteRepository = fabricanteRepository;
//    }

    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    // Patrón: Optional + orElseThrow -> si no existe, excepción propia (el GlobalExceptionHandler la convierte en 404).
    public Producto buscarPorCodigo(Integer codigo) {
        // ------------------------------------ IMPERATIVO ----------------
//        Optional<Producto> producto = productoRepository.findById(codigo);
//
//        if (producto.isPresent()) {
//            return producto.get(); // devuelve el producto, no el envoltorio optional
//        }else{
//            throw new RecursoNoEncontradoException("No existe el producto con código " + codigo);
//        }
        // ---------------------------------------

        return productoRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con código " + codigo));

    }

    /** Alta. Devuelve el producto con el código generado. */
    public Producto crear(Producto producto) {
        prepararParaGuardar(producto);
        productoRepository.save(producto);

        System.out.println("************>"+producto);

        return producto;
    }

    /** Modificación. El producto tiene que existir. */
    public Producto actualizar(Producto producto) {
        buscarPorCodigo(producto.getCodigo());
        prepararParaGuardar(producto);
        productoRepository.update(producto);
        return producto;
    }

    public void borrar(Integer codigo) {
        buscarPorCodigo(codigo);
        productoRepository.delete(codigo);
    }

    // Reglas comunes a crear y actualizar
    // >>> EXAMEN [REGLA NUEVA]: reglas comunes a crear y actualizar. Para una regla nueva: comprueba la condición
    //     y, si no se cumple, lanza una excepción propia (ver FabricanteConProductosException como plantilla).
    //     Si la regla es solo para crear o solo para actualizar, ponla dentro de crear() o actualizar().
    private void prepararParaGuardar(Producto producto) {
        producto.setNombre(producto.getNombre().trim());

        // Regla de negocio: el fabricante tiene que existir
        Integer codigoFabricante = producto.getFabricante().getCodigo();
        Fabricante fabricante = fabricanteRepository.findById(codigoFabricante)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el fabricante con código " + codigoFabricante));
        producto.setFabricante(fabricante);   // ahora lleva también el nombre
    }
}