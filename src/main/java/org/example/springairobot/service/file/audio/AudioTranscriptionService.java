package org.example.springairobot.service.file.audio;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AudioTranscriptionService {

    @Value("${audio.whisper.model-path}")
    private String modelPath;

    @Value("${audio.whisper.model-file}")
    private String modelFile;

    @Value("${audio.whisper.language:zh}")
    private String language;

    @Value("${audio.whisper.threads:4}")
    private int threads;

    private WhisperJNI whisper;
    private WhisperContext ctx;

    @PostConstruct
    public void init() throws IOException {
        WhisperJNI.loadLibrary();
        whisper = new WhisperJNI();
        Path modelFilePath = Paths.get(modelPath, modelFile);
        if (!Files.exists(modelFilePath)) {
            throw new IOException("Whisper model file not found: " + modelFilePath);
        }
        ctx = whisper.init(modelFilePath);
    }

    @PreDestroy
    public void destroy() {
        if (ctx != null) {
            ctx.close();
        }
    }

    public String transcribe(MultipartFile audioFile) throws IOException, UnsupportedAudioFileException {
        float[] samples = decodeAudio(audioFile);
        WhisperFullParams params = new WhisperFullParams();
        params.language = this.language;
        params.nThreads = this.threads;
        params.printProgress = false;
        params.printTimestamps = false;
        params.translate = false;
        params.singleSegment = true;

        int result = whisper.full(ctx, params, samples, samples.length);
        if (result != 0) {
            throw new RuntimeException("Transcription failed with code: " + result);
        }

        int numSegments = whisper.fullNSegments(ctx);
        StringBuilder transcription = new StringBuilder();
        for (int i = 0; i < numSegments; i++) {
            transcription.append(whisper.fullGetSegmentText(ctx, i));
        }
        return transcription.toString();
    }

    private float[] decodeAudio(MultipartFile audioFile) throws IOException, UnsupportedAudioFileException {
        Path tempFile = Files.createTempFile("audio_", ".tmp");
        audioFile.transferTo(tempFile.toFile());

        try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(tempFile.toFile())) {
            AudioFormat targetFormat = new AudioFormat(16000, 16, 1, true, false);
            try (AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream)) {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = convertedStream.read(buffer)) != -1) {
                    byteOut.write(buffer, 0, bytesRead);
                }
                byte[] pcmData = byteOut.toByteArray();
                float[] samples = new float[pcmData.length / 2];
                for (int i = 0; i < samples.length; i++) {
                    short sample = (short) ((pcmData[i * 2] & 0xFF) | (pcmData[i * 2 + 1] << 8));
                    samples[i] = sample / 32768.0f;
                }
                return samples;
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
