package com.api.service.catalogo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class FileUploadService {

    private final Cloudinary cloudinary;

    // --- AQUÍ ES DONDE VA LA URL ---
    public FileUploadService() {
        // En lugar de pasar el mapa (cloud_name, api_key...),
        // pasas la URL completa como un String.
        // Reemplaza <your_api_key> y <your_api_secret> con tus datos reales.
        this.cloudinary = new Cloudinary("cloudinary://185939882356171:lWhyBM38RbbMEiPj7AAgn2dcW4g@dqb3kmpt2");
    }

    // --- Este método se queda IGUAL ---
    public String uploadFile(MultipartFile file) throws IOException {
        // Aquí solo USAMOS la conexión que creamos arriba
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        return uploadResult.get("secure_url").toString();
    }
}