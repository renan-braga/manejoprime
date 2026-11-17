package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FfmpegWhatsappAudioTranscoderTest {

    @Test
    void returnsTranscodedAudioWhenFfmpegSucceeds() {
        CapturingExternalProcessRunner processRunner = new CapturingExternalProcessRunner();
        processRunner.result = new ProcessExecutionResult(
                0,
                "mp3-output".getBytes(StandardCharsets.UTF_8),
                new byte[0]
        );
        FfmpegWhatsappAudioTranscoder transcoder = new FfmpegWhatsappAudioTranscoder(processRunner);

        var transcoded = transcoder.transcode(new WhatsappAudioTranscodeRequest(
                "wamid.ffmpeg-1",
                "audio/ogg",
                "downloaded-audio".getBytes(StandardCharsets.UTF_8)
        ));

        assertThat(processRunner.commands).singleElement().satisfies(command ->
                assertThat(command).containsExactly(
                        "ffmpeg",
                        "-y",
                        "-i",
                        "pipe:0",
                        "-vn",
                        "-f",
                        "mp3",
                        "pipe:1"
                ));
        assertThat(transcoded.mimeType()).isEqualTo("audio/mpeg");
        assertThat(new String(transcoded.content(), StandardCharsets.UTF_8)).isEqualTo("mp3-output");
    }

    @Test
    void surfacesReadableFailureWhenFfmpegExitsNonZero() {
        CapturingExternalProcessRunner processRunner = new CapturingExternalProcessRunner();
        processRunner.result = new ProcessExecutionResult(
                1,
                new byte[0],
                "invalid codec".getBytes(StandardCharsets.UTF_8)
        );
        FfmpegWhatsappAudioTranscoder transcoder = new FfmpegWhatsappAudioTranscoder(processRunner);

        assertThatThrownBy(() -> transcoder.transcode(new WhatsappAudioTranscodeRequest(
                "wamid.ffmpeg-2",
                "audio/ogg",
                "downloaded-audio".getBytes(StandardCharsets.UTF_8)
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ffmpeg transcode failed: invalid codec");
    }

    private static final class CapturingExternalProcessRunner implements ExternalProcessRunner {

        private final List<List<String>> commands = new ArrayList<>();
        private ProcessExecutionResult result;

        @Override
        public ProcessExecutionResult run(List<String> command, byte[] stdin) {
            commands.add(command);
            return result;
        }
    }
}
