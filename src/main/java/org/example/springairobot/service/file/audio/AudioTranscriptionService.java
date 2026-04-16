package org.example.springairobot.service.file.audio;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.example.springairobot.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 音频转录服务
 * 
 * 使用 Whisper 模型将音频文件转换为文本
 * 
 * 功能特点：
 * - 支持多种音频格式
 * - 自动转换为 Whisper 所需的音频格式（16kHz, 16bit, 单声道）
 * - 多语言识别
 * - 可配置识别参数
 * 
 * 处理流程：
 * 1. 初始化 Whisper 模型
 * 2. 解码音频文件并转换为 PCM 格式
 * 3. 使用 Whisper 进行语音识别
 * 4. 拼接所有片段的识别结果
 */
@Service
public class AudioTranscriptionService {

    /** Whisper 模型文件路径 */
    @Value("${audio.whisper.model-path}")
    private String modelPath;

    /** Whisper 模型文件名 */
    @Value("${audio.whisper.model-file}")
    private String modelFile;

    /** 识别语言 */
    @Value("${audio.whisper.language:" + AppConstants.AudioConstants.WHISPER_DEFAULT_LANGUAGE + "}")
    private String language;

    /** 处理线程数 */
    @Value("${audio.whisper.threads:" + AppConstants.AudioConstants.WHISPER_DEFAULT_THREADS + "}")
    private int threads;

    /** Whisper JNI 实例 */
    private WhisperJNI whisper;
    
    /** Whisper 上下文 */
    private WhisperContext ctx;

    /**
     * 初始化 Whisper 模型
     * 
     * 加载 Whisper 库并初始化模型上下文
     * 
     * @throws IOException 模型文件不存在时抛出
     */
    @PostConstruct
    public void init() throws IOException {
        // 加载 Whisper 库
        WhisperJNI.loadLibrary();
        whisper = new WhisperJNI();
        
        // 检查模型文件是否存在
        Path modelFilePath = Paths.get(modelPath, modelFile);
        if (!Files.exists(modelFilePath)) {
            throw new IOException(AppConstants.AudioConstants.ERROR_MODEL_FILE_NOT_FOUND + modelFilePath);
        }
        
        // 初始化模型上下文
        ctx = whisper.init(modelFilePath);
    }

    /**
     * 销毁资源
     * 
     * 关闭 Whisper 上下文，释放资源
     */
    @PreDestroy
    public void destroy() {
        if (ctx != null) {
            ctx.close();
        }
    }

    /**
     * 转录音频文件
     * 
     * 将音频文件转换为文本
     * 
     * @param audioFile 音频文件
     * @return 转录文本
     * @throws IOException IO 异常
     * @throws UnsupportedAudioFileException 不支持的音频格式
     * @throws RuntimeException 转录失败时抛出
     */
    public String transcribe(MultipartFile audioFile) throws IOException, UnsupportedAudioFileException {
        // 1. 解码音频为 PCM 采样
        float[] samples = decodeAudio(audioFile);
        
        // 2. 配置 Whisper 参数
        WhisperFullParams params = new WhisperFullParams();
        params.language = this.language;
        params.nThreads = this.threads;
        params.printProgress = AppConstants.AudioConstants.WHISPER_PRINT_PROGRESS;
        params.printTimestamps = AppConstants.AudioConstants.WHISPER_PRINT_TIMESTAMPS;
        params.translate = AppConstants.AudioConstants.WHISPER_TRANSLATE;
        params.singleSegment = AppConstants.AudioConstants.WHISPER_SINGLE_SEGMENT;

        // 3. 执行转录
        int result = whisper.full(ctx, params, samples, samples.length);
        if (result != 0) {
            throw new RuntimeException(AppConstants.AudioConstants.ERROR_TRANSCRIPTION_FAILED + result);
        }

        // 4. 拼接所有片段
        int numSegments = whisper.fullNSegments(ctx);
        StringBuilder transcription = new StringBuilder();
        for (int i = 0; i < numSegments; i++) {
            transcription.append(whisper.fullGetSegmentText(ctx, i));
        }
        return transcription.toString();
    }

    /**
     * 解码音频文件
     * 
     * 将任意格式的音频转换为 Whisper 所需的格式：
     * - 采样率：16kHz
     * - 位深：16bit
     * - 声道：单声道
     * - 符号：有符号
     * - 字节序：小端
     * 
     * @param audioFile 音频文件
     * @return PCM 采样数组（归一化到 [-1, 1]）
     * @throws IOException IO 异常
     * @throws UnsupportedAudioFileException 不支持的音频格式
     */
    private float[] decodeAudio(MultipartFile audioFile) throws IOException, UnsupportedAudioFileException {
        // 创建临时文件
        Path tempFile = Files.createTempFile(
                AppConstants.AudioConstants.TEMP_FILE_PREFIX_AUDIO, 
                AppConstants.AudioConstants.TEMP_FILE_SUFFIX_AUDIO);
        audioFile.transferTo(tempFile.toFile());

        try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(tempFile.toFile())) {
            // 定义目标格式：16kHz, 16bit, 单声道，有符号，小端
            AudioFormat targetFormat = new AudioFormat(
                    AppConstants.AudioConstants.AUDIO_SAMPLE_RATE,
                    AppConstants.AudioConstants.AUDIO_BITS_PER_SAMPLE,
                    AppConstants.AudioConstants.AUDIO_CHANNELS,
                    AppConstants.AudioConstants.AUDIO_SIGNED,
                    AppConstants.AudioConstants.AUDIO_BIG_ENDIAN
            );
            
            try (AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream)) {
                // 读取转换后的数据
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                byte[] buffer = new byte[AppConstants.AudioConstants.AUDIO_BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = convertedStream.read(buffer)) != -1) {
                    byteOut.write(buffer, 0, bytesRead);
                }
                byte[] pcmData = byteOut.toByteArray();
                
                // 将 PCM 数据转换为归一化的 float 数组
                float[] samples = new float[pcmData.length / 2];
                for (int i = 0; i < samples.length; i++) {
                    // 小端序读取 16bit 有符号整数
                    short sample = (short) ((pcmData[i * 2] & 0xFF) | (pcmData[i * 2 + 1] << 8));
                    // 归一化到 [-1, 1]
                    samples[i] = sample / AppConstants.AudioConstants.AUDIO_NORMALIZATION_FACTOR;
                }
                return samples;
            }
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempFile);
        }
    }
}
