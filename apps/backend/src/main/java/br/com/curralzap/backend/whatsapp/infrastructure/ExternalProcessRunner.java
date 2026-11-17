package br.com.curralzap.backend.whatsapp.infrastructure;

import java.util.List;

interface ExternalProcessRunner {

    ProcessExecutionResult run(List<String> command, byte[] stdin);
}
