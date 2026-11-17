package br.com.curralzap.backend.whatsapp.infrastructure;

record ProcessExecutionResult(
        int exitCode,
        byte[] stdout,
        byte[] stderr
) {
}
