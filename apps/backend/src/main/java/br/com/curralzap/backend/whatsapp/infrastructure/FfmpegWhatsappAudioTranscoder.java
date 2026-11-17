package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.TranscodedWhatsappAudio;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequest;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
class FfmpegWhatsappAudioTranscoder implements WhatsappAudioTranscoder {

    private final ExternalProcessRunner processRunner;

    FfmpegWhatsappAudioTranscoder(ExternalProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    @Override
    public TranscodedWhatsappAudio transcode(WhatsappAudioTranscodeRequest request) {
        ProcessExecutionResult result = processRunner.run(List.of(
                "ffmpeg",
                "-y",
                "-i",
                "pipe:0",
                "-vn",
                "-f",
                "mp3",
                "pipe:1"
        ), request.content());

        if (result.exitCode() != 0) {
            throw new IllegalStateException(failureMessage(result));
        }
        if (result.stdout() == null || result.stdout().length == 0) {
            throw new IllegalStateException("ffmpeg produced no transcoded audio output.");
        }
        return new TranscodedWhatsappAudio("audio/mpeg", result.stdout());
    }

    private String failureMessage(ProcessExecutionResult result) {
        String stderr = result.stderr() == null ? "" : new String(result.stderr(), StandardCharsets.UTF_8).trim();
        if (stderr.isBlank()) {
            return "ffmpeg exited with code " + result.exitCode() + ".";
        }
        return "ffmpeg transcode failed: " + stderr;
    }
}
