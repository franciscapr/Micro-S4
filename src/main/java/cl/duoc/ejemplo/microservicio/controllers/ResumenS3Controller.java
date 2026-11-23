
package cl.duoc.ejemplo.microservicio.controllers;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.exception.SdkException;

@RestController
@RequestMapping("/resumenes")
public class ResumenS3Controller {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public ResumenS3Controller(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostMapping(
        value = "/{numeroResumen}/archivo",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, String>> subirArchivo(
            @PathVariable Long numeroResumen,
            @RequestParam("archivo") MultipartFile archivo) {

        if (numeroResumen == null || numeroResumen <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Numero de resumen invalido"
            );
        }

        if (archivo.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El archivo esta vacio"
            );
        }

        String nombreEsperado = "resumen-" + numeroResumen + ".txt";

        if (!nombreEsperado.equals(archivo.getOriginalFilename())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El archivo debe llamarse " + nombreEsperado
            );
        }

        String key = numeroResumen + "/" + nombreEsperado;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("text/plain; charset=utf-8")
                    .build();

            s3Client.putObject(
                request,
                RequestBody.fromBytes(archivo.getBytes())
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se pudo leer el archivo",
                e
            );

        } catch (SdkException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo subir el archivo a AWS S3",
                e
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
            Map.of(
                "mensaje", "Archivo subido correctamente",
                "bucket", bucket,
                "ruta", key
            )
        );
    }



    @PutMapping(
        value = "/{numeroResumen}/archivo",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, String>> modificarArchivo(
            @PathVariable Long numeroResumen,
            @RequestParam("archivo") MultipartFile archivo) {

        if (numeroResumen == null || numeroResumen <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Numero de resumen invalido"
            );
        }

        if (archivo.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El archivo esta vacio"
            );
        }

        String nombreEsperado = "resumen-" + numeroResumen + ".txt";

        if (!nombreEsperado.equals(archivo.getOriginalFilename())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El archivo debe llamarse " + nombreEsperado
            );
        }

        String key = numeroResumen + "/" + nombreEsperado;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("text/plain; charset=utf-8")
                    .build();

            s3Client.putObject(
                request,
                RequestBody.fromBytes(archivo.getBytes())
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se pudo leer el archivo",
                e
            );

        } catch (SdkException e) {
            e.printStackTrace();

            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo modificar el archivo en AWS S3",
                e
            );
        }

        return ResponseEntity.ok(
            Map.of(
                "mensaje", "Archivo modificado correctamente",
                "bucket", bucket,
                "ruta", key
            )
        );
    }


    @GetMapping("/{numeroResumen}/archivo")
    public ResponseEntity<byte[]> descargarArchivo(
            @PathVariable Long numeroResumen) {

        if (numeroResumen == null || numeroResumen <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Numero de resumen invalido"
            );
        }

        String nombreArchivo = "resumen-" + numeroResumen + ".txt";
        String key = numeroResumen + "/" + nombreArchivo;

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> archivo =
                    s3Client.getObjectAsBytes(request);

            return ResponseEntity.ok()
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreArchivo + "\""
                    )
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(archivo.asByteArray());

        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe el resumen solicitado",
                e
            );

        } catch (SdkException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo descargar el archivo desde AWS S3",
                e
            );
        }
    }


    @DeleteMapping("/{numeroResumen}/archivo")
    public ResponseEntity<Map<String, String>> eliminarArchivo(
            @PathVariable Long numeroResumen) {

        if (numeroResumen == null || numeroResumen <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Numero de resumen invalido"
            );
        }

        String key = numeroResumen + "/resumen-" + numeroResumen + ".txt";

        try {
            // Comprobar que el archivo existe
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.headObject(headRequest);

            // Eliminar el archivo de S3
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);

            return ResponseEntity.ok(
                Map.of(
                    "mensaje", "Archivo eliminado correctamente",
                    "bucket", bucket,
                    "ruta", key
                )
            );

        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "El archivo no existe en S3",
                e
            );

        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El archivo no existe en S3",
                    e
                );
            }

            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Error de AWS S3 al eliminar el archivo",
                e
            );

        } catch (SdkException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo conectar con AWS S3",
                e
            );
        }
    }




    
}
