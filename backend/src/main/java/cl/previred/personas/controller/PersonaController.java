package cl.previred.personas.controller;

import cl.previred.personas.dto.request.PersonaRequest;
import cl.previred.personas.dto.response.PersonaResponse;
import cl.previred.personas.service.PersonaService;
import cl.previred.personas.validation.Rut;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints REST del CRUD de personas. Es una capa delgada: solo traduce HTTP hacia
 * {@link PersonaService} y deja toda la logica de negocio y de resiliencia en el
 * service (ver {@code PersonaServiceImpl}).
 * <p>
 * El RUT identifica el recurso en la URL (es la PK, ver {@code Persona}). Se valida
 * con {@code @Rut} directamente sobre el path variable: un RUT con formato/digito
 * verificador invalido se rechaza con 400 antes de llegar al service.
 */
@RestController
@RequestMapping("/api/personas")
@Tag(name = "Personas", description = "CRUD de personas")
@Validated
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    /** Crea una nueva persona. Responde 201 con el registro creado. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonaResponse crear(@Valid @RequestBody PersonaRequest request) {
        return personaService.crear(request);
    }

    /** Lista todas las personas, incluyendo las que aun estan pendientes de sincronizacion. */
    @GetMapping
    public List<PersonaResponse> listar() {
        return personaService.listar();
    }

    /** Busca una persona por su RUT. Responde 404 si no existe. */
    @GetMapping("/{rut}")
    public PersonaResponse obtenerPorRut(@PathVariable @Rut String rut) {
        return personaService.obtenerPorRut(rut);
    }

    /** Actualiza los datos de una persona existente. El RUT de la URL y del cuerpo deben coincidir. */
    @PutMapping("/{rut}")
    public PersonaResponse actualizar(@PathVariable @Rut String rut, @Valid @RequestBody PersonaRequest request) {
        return personaService.actualizar(rut, request);
    }

    /** Elimina una persona por su RUT. Responde 204 sin contenido. */
    @DeleteMapping("/{rut}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable @Rut String rut) {
        personaService.eliminar(rut);
    }
}
