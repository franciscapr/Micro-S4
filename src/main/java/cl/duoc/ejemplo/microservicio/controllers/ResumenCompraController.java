
package cl.duoc.ejemplo.microservicio.controllers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/resumenes")
public class ResumenCompraController {

    public record Compra(
        Long numeroResumen,
        String cliente,
        String evento,
        Integer cantidadEntradas,
        BigDecimal precioUnitario
    ) {}

    @PostMapping("/generar")
    public ResponseEntity<byte[]> generarResumen(
            @RequestBody Compra compra) {

        if (compra.numeroResumen() == null
                || compra.numeroResumen() <= 0
                || compra.cliente() == null
                || compra.cliente().isBlank()
                || compra.evento() == null
                || compra.evento().isBlank()
                || compra.cantidadEntradas() == null
                || compra.cantidadEntradas() <= 0
                || compra.precioUnitario() == null
                || compra.precioUnitario().signum() < 0) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Datos de compra invalidos"
            );
        }

        BigDecimal total = compra.precioUnitario()
            .multiply(BigDecimal.valueOf(compra.cantidadEntradas()));

        String contenido = String.join("\n",
            "RESUMEN DE COMPRA",
            "---------------------------",
            "Numero de resumen: " + compra.numeroResumen(),
            "Cliente: " + compra.cliente(),
            "Evento: " + compra.evento(),
            "Cantidad de entradas: " + compra.cantidadEntradas(),
            "Precio unitario: $" + compra.precioUnitario(),
            "Total de compra: $" + total
        ) + "\n";

        byte[] archivo = contenido.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"resumen-" +
                compra.numeroResumen() + ".txt\""
            )
            .contentType(
                new MediaType("text", "plain", StandardCharsets.UTF_8)
            )
            .contentLength(archivo.length)
            .body(archivo);
    }
}
