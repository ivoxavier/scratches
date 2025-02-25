/*
 * 2025  Ivo Xavier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3.
 *
 * TiffToPdfConverter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */




package com.ixsvf.ttiftopdfconverter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TiffToPdfConverter {

    public static void convertTiffToPdf(String inputFolderPath, String outputFolderPath) throws IOException {
        File outputDir = new File(outputFolderPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        Path inputFolder = Paths.get(inputFolderPath);

        try (var files = Files.list(inputFolder)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> file.toString().toLowerCase().endsWith(".tif") || file.toString().toLowerCase().endsWith(".tiff"))
                    .forEach(tiffFile -> {
                        try {
                            if (isTiff(tiffFile)) {
                                convertSingleTiffToPdf(tiffFile, outputFolderPath);
                            } else {
                                System.out.println("File ignored (not a valid TIFF): " + tiffFile.getFileName());
                            }
                        } catch (IOException e) {
                            System.err.println("Error during processing : " + tiffFile.getFileName() + " - " + e.getMessage());
                        }
                    });
        }
    }

    private static void convertSingleTiffToPdf(Path tiffFile, String outputFolderPath) throws IOException {

        String fileNameWithoutExtension = com.google.common.io.Files.getNameWithoutExtension(tiffFile.getFileName().toString());
        String outputPdfFilePath = outputFolderPath + File.separator + fileNameWithoutExtension + ".pdf";

        try (PDDocument document = new PDDocument()) {
            List<BufferedImage> images = readTiffImages(tiffFile.toFile());

            if (images == null || images.isEmpty()) {
                System.err.println("Error: TIFF image empty or not valid: " + tiffFile.getFileName());
                return;
            }
            for (BufferedImage image : images) {
                PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
                document.addPage(page);

                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, toByteArray(image), tiffFile.getFileName().toString());

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, 0, 0, image.getWidth(), image.getHeight());
                }
            }
            document.save(outputPdfFilePath);
            System.out.println("Converted: " + tiffFile.getFileName() + " -> " + fileNameWithoutExtension + ".pdf");

            // Delete the tif file after convertion
            try {
                Files.delete(tiffFile);
                System.out.println("TIFF file erased: " + tiffFile.getFileName());
            } catch (IOException e) {
                System.err.println("Error during erasing TIFF: " + tiffFile.getFileName() + " - " + e.getMessage());
            }


        } catch (IOException e) {
            throw new IOException("Error during conversion " + tiffFile.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private static byte[] toByteArray(BufferedImage image) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "tif", baos);
        return baos.toByteArray();
    }

    private static boolean isTiff(Path file) {
        try {
            byte[] header = Files.readAllBytes(file);
            if (header.length >= 4) {
                byte[] firstFourBytes = Arrays.copyOfRange(header, 0, 4);
                return Arrays.equals(firstFourBytes, new byte[]{0x49, 0x49, 0x2A, 0x00}) ||
                        Arrays.equals(firstFourBytes, new byte[]{0x4D, 0x4D, 0x00, 0x2A});
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    private static List<BufferedImage> readTiffImages(File tiffFile) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(tiffFile)) {
            if (imageInputStream == null) {
                throw new IOException("Error it was not possible create the ImageInputStream para o file: " + tiffFile.getName());
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new IOException("No ImageReader found in TIFF: " + tiffFile.getName());
            }

            ImageReader reader = readers.next();
            reader.setInput(imageInputStream);

            try {
                int numImages = reader.getNumImages(true);
                for (int i = 0; i < numImages; i++) {
                    BufferedImage image = reader.read(i);
                    if (image != null) {
                        images.add(image);
                    } else {
                        System.err.println("Warning: No image " + i + " of tiff file: " + tiffFile.getName());
                    }
                }
            } finally {
                reader.dispose();
            }

        }
        return images;
    }

    public static void main(String[] args) {
        String inputFolderPath = "/in";
        String outputFolderPath = "out";
        try {
            convertTiffToPdf(inputFolderPath, outputFolderPath);
        } catch (IOException e) {
            System.err.println("Err during conversion: " + e.getMessage());
            e.printStackTrace();
        }
    }
}