package br.com.curralzap.backend.animals.application;

public class AnimalHasRecordsException extends RuntimeException {

    public AnimalHasRecordsException() {
        super("Animal possui registros associados e não pode ser excluído.");
    }
}
