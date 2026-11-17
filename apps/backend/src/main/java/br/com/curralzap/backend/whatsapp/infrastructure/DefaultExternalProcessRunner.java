package br.com.curralzap.backend.whatsapp.infrastructure;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

@Component
class DefaultExternalProcessRunner implements ExternalProcessRunner {

    @Override
    public ProcessExecutionResult run(List<String> command, byte[] stdin) {
        try {
            Process process = new ProcessBuilder(command).start();
            try (OutputStream outputStream = process.getOutputStream()) {
                outputStream.write(stdin);
            }
            byte[] stdout = readAll(process.getInputStream());
            byte[] stderr = readAll(process.getErrorStream());
            int exitCode = process.waitFor();
            return new ProcessExecutionResult(exitCode, stdout, stderr);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to start external process.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("External process execution was interrupted.", exception);
        }
    }

    private byte[] readAll(InputStream inputStream) throws IOException {
        return inputStream.readAllBytes();
    }
}
