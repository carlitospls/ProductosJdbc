package es.daw.productojdbc.service;

import es.daw.productojdbc.exception.FabricanteConProductosException;
import es.daw.productojdbc.exception.RecursoNoEncontradoException;
import es.daw.productojdbc.model.Fabricante;
import es.daw.productojdbc.repository.FabricanteRepository;
import es.daw.productojdbc.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// Mismo esquema que ProductoService. Este servicio usa DOS repositorios porque la regla de borrado necesita saber
// si el fabricante tiene productos.
@Service
public class FabricanteService {

    private final FabricanteRepository fabricanteRepository;
    private final ProductoRepository productoRepository;

    public FabricanteService(FabricanteRepository fabricanteRepository,
                             ProductoRepository productoRepository) {
        this.fabricanteRepository = fabricanteRepository;
        this.productoRepository = productoRepository;
    }

    public List<Fabricante> listar() {
        return fabricanteRepository.findAll();
    }

    public Fabricante buscarPorCodigo(Integer codigo) {
        return fabricanteRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el fabricante con código " + codigo));
    }

    // >>> EXAMEN [REGLA NUEVA]: Reto 3. Antes de save(): if (fabricanteRepository.existsByNombreIgnoreCase(nombre))
    //     throw new FabricanteDuplicadoException(nombre);   (hay que crear esa excepción copiando FabricanteConProductosException)
    public Fabricante crear(Fabricante fabricante) {
        fabricante.setNombre(fabricante.getNombre().trim());
        fabricanteRepository.save(fabricante);
        return fabricante;
    }

    // >>> EXAMEN [REGLA NUEVA]: Reto 3 al EDITAR: usar existsByNombreIgnoreCaseAndCodigoNot(nombre, codigo)
    //     (si no excluyes al propio registro, guardar sin cambiar el nombre daría un falso duplicado).
    public Fabricante actualizar(Fabricante fabricante) {
        buscarPorCodigo(fabricante.getCodigo());
        fabricante.setNombre(fabricante.getNombre().trim());
        fabricanteRepository.update(fabricante);
        return fabricante;
    }

    // Ejemplo de REGLA DE NEGOCIO: se comprueba en Java ANTES de tocar la BD y se lanza una excepción con significado.
    public void borrar(Integer codigo) {
        buscarPorCodigo(codigo);

        // Regla de negocio: comprobada aquí, antes de llegar a la BD
        if (productoRepository.existsByFabricanteCodigo(codigo)) {
            throw new FabricanteConProductosException(codigo);
        }
        fabricanteRepository.delete(codigo);
    }


}
