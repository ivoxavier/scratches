import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;


public class TiffToPdfConverter {

    public void convertTiffToPdf(String tiffFilePath) throws IOException {

        File tiffFile = new File(tiffFilePath);
        if (!tiffFile.exists() || !tiffFile.isFile() || !tiffFilePath.toLowerCase().endsWith(".tif") && !tiffFilePath.toLowerCase().endsWith(".tiff")) {
            throw new IllegalArgumentException("Invalid TIFF file path: " + tiffFilePath);
        }
        
        String pdfFilePath = tiffFilePath.substring(0, tiffFilePath.lastIndexOf('.')) + ".pdf";
        File pdfFile = new File(pdfFilePath);

        try {
            convertImageToPdf(tiffFilePath, pdfFilePath);
            
            if (!tiffFile.delete()) {
                 System.err.println("Failed to delete TIFF file: " + tiffFilePath); //Melhor logging em caso de falha.
            }

        } catch (IOException e) {
          
            System.err.println("Error converting TIFF to PDF: " + e.getMessage());
            e.printStackTrace(); 
            throw e;
        }

    }



    private void convertImageToPdf(String imagePath, String pdfPath) throws IOException {
        
        try (FileOutputStream fos = new FileOutputStream(pdfPath);
             PdfWriter writer = new PdfWriter(fos);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {


            ImageData imageData = ImageDataFactory.create(imagePath);
            Image image = new Image(imageData);

            
            float pageWidth = pdf.getDefaultPageSize().getWidth() - document.getLeftMargin() - document.getRightMargin();
            float pageHeight = pdf.getDefaultPageSize().getHeight() - document.getTopMargin() - document.getBottomMargin();
            float imageWidth = image.getImageWidth();
            float imageHeight = image.getImageHeight();

            if (imageWidth > pageWidth || imageHeight > pageHeight) {
                image.scaleToFit(pageWidth, pageHeight);
            }


            document.add(image);
        }
    }


    public static void main(String[] args) {
        TiffToPdfConverter converter = new TiffToPdfConverter();
        String filePath = "image.tif";

        
        try {
            converter.convertTiffToPdf(filePath);
            System.out.println("TIFF converted to PDF successfully!");

        } catch (IOException e) {
             System.err.println("An error occurred: " + e.getMessage()); // Mensagem amigável ao usuário
        }
        catch (IllegalArgumentException e) {
             System.err.println("Error: " + e.getMessage());
        }
    }
}
