package es.daw.productojdbc.controller;

import es.daw.productojdbc.dto.ProductoForm;
import es.daw.productojdbc.model.Producto;
import es.daw.productojdbc.service.FabricanteService;
import es.daw.productojdbc.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// CAPA WEB: recibe la petición HTTP, llama al servicio y elige la vista. Aquí NO hay SQL ni reglas de negocio.
// Mapa de URLs:  GET /productos (lista) | GET /productos/nuevo | GET /productos/editar/{codigo}
//                POST /productos/guardar (alta Y edición) | POST /productos/borrar/{codigo}
@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    // El controlador solo conoce SERVICIOS, nunca repositorios
    private final ProductoService productoService;
    private final FabricanteService fabricanteService;

//    public ProductoController(ProductoService productoService,
//                              FabricanteService fabricanteService) {
//        this.productoService = productoService;
//        this.fabricanteService = fabricanteService;
//    }

    // READ: listado
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoService.listar());
        return "productos/lista";
    }

    // (aquí irán los métodos de los pasos 12 y 13)

    // Formulario vacío para alta
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("productoForm", new ProductoForm());
        model.addAttribute("fabricantes", fabricanteService.listar());
        return "productos/formulario";
    }

    // Formulario relleno para edición
    @GetMapping("/editar/{codigo}")
    public String editar(@PathVariable("codigo") Integer codigo, Model model) {

        Producto producto = productoService.buscarPorCodigo(codigo);   // 404 si no existe

        model.addAttribute("productoForm", ProductoForm.toDto(producto));
        model.addAttribute("fabricantes", fabricanteService.listar());
        return "productos/formulario";
    }

    // CREATE / UPDATE: el mismo formulario sirve para las dos operaciones
    @PostMapping("/guardar")
    // Un solo método sirve para alta y edición: si codigo == null es alta; si tiene valor, es edición.
    // @Valid + BindingResult (justo detrás): comprueba las anotaciones del ProductoForm.
    // Con errores -> se vuelve a la MISMA vista SIN redirect (para conservar lo escrito) y hay que recargar
    // los datos del <select> (fabricantes), porque no sobreviven a la petición.
    // Sin errores -> redirect (patrón PRG) con mensaje flash.
    public String guardar(@Valid @ModelAttribute("productoForm") ProductoForm productoForm,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        // >>> EXAMEN [VALIDACIÓN]: si la regla nueva falla en el servicio (ej.: duplicado) y quieres el error junto al campo,
        //     captura la excepción aquí con try/catch y haz result.rejectValue("campo", "codigo.error", "mensaje");
        //     y vuelve a la vista del formulario (recargando el <select>).
        if (result.hasErrors()) {
            // Volvemos al formulario: hay que recargar el desplegable
            model.addAttribute("fabricantes", fabricanteService.listar());
            return "productos/formulario";
        }

        if (productoForm.getCodigo() == null) {
            Producto creado = productoService.crear(productoForm.toModelEntity());
            redirectAttributes.addFlashAttribute("mensaje",
                    "Producto creado con código " + creado.getCodigo());
        } else {
            productoService.actualizar(productoForm.toModelEntity());
            redirectAttributes.addFlashAttribute("mensaje",
                    "Producto " + productoForm.getCodigo() + " actualizado");
        }

        // PRG (Post/Redirect/Get): si el usuario pulsa F5 no se reenvía el formulario
        return "redirect:/productos";
    }




    // DELETE: por POST, nunca por GET (un GET no debe modificar datos)
    @PostMapping("/borrar/{codigo}")
    public String borrar(@PathVariable Integer codigo,
                         RedirectAttributes redirectAttributes) {
        productoService.borrar(codigo);
        redirectAttributes.addFlashAttribute("mensaje", "Producto " + codigo + " borrado");
        return "redirect:/productos";
    }


}