package org.example.springairobot.service.file.ocr;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class OcrService {

    @Value("${ocr.tesseract.datapath:}")
    private String tessDataPath;

    @Value("${ocr.tesseract.language:chi_sim+eng}")
    private String language;

    public String recognizeText(MultipartFile imageFile) throws IOException, TesseractException {
        ITesseract tesseract = new Tesseract();
        if (tessDataPath != null && !tessDataPath.isBlank()) {
            tesseract.setDatapath(tessDataPath);
        }
        tesseract.setLanguage(language);
        BufferedImage image = ImageIO.read(imageFile.getInputStream());
        return tesseract.doOCR(image);
    }

    public String recognizeText(MultipartFile imageFile, String lang) throws IOException, TesseractException {
        ITesseract tesseract = new Tesseract();
        if (tessDataPath != null && !tessDataPath.isBlank()) {
            tesseract.setDatapath(tessDataPath);
        }
        tesseract.setLanguage(lang);
        BufferedImage image = ImageIO.read(imageFile.getInputStream());
        return tesseract.doOCR(image);
    }
}
