package org.example.springairobot.service.file.document;

import org.example.springairobot.PO.DTO.ParsedDocumentResult;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentParserService {

    public String parseDocument(MultipartFile file) throws IOException {
        TikaDocumentReader reader = new TikaDocumentReader(
                new InputStreamResource(file.getInputStream())
        );
        List<Document> documents = reader.read();
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
    }

    public List<Document> parseDocumentAsDocuments(MultipartFile file) throws IOException {
        TikaDocumentReader reader = new TikaDocumentReader(
                new InputStreamResource(file.getInputStream())
        );
        return reader.read();
    }

    public ParsedDocumentResult parseWithMetadata(MultipartFile file) throws IOException {
        TikaDocumentReader reader = new TikaDocumentReader(
                new InputStreamResource(file.getInputStream())
        );
        List<Document> documents = reader.read();
        String text = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        return ParsedDocumentResult.builder()
                .text(text)
                .metadata(documents.isEmpty() ? null : documents.get(0).getMetadata())
                .build();
    }
}
