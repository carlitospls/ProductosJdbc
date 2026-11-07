package es.daw.productojdbc.exception;

/** Regla de negocio: no se borra un fabricante que tiene productos. */
// >>> EXAMEN [EXCEPCIÓN NUEVA]: plantilla. Para una regla nueva: copia esta clase, cámbiale el nombre y el mensaje.
//     extends RuntimeException = no obliga a try/catch ni a throws.
public class FabricanteConProductosException extends RuntimeException {

    public FabricanteConProductosException(Integer codigo) {
        super("No se puede borrar el fabricante " + codigo + ": tiene productos asociados.");
    }
}
