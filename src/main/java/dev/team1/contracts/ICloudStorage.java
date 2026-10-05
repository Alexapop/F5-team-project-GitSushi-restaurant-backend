package dev.team1.contracts;

import java.io.IOException;

// Almacenamiento de objetos en la nube donde se guardan los resúmenes de ventas en PDF.
public interface ICloudStorage {
    boolean isConfigured();
    void upload(String path, byte[] content) throws IOException, InterruptedException;
}