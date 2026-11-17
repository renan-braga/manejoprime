package br.com.curralzap.backend.medications.application;

import java.util.List;

public record MedicationListView(
        List<MedicationView> items,
        long total,
        int limit,
        int offset
) {
}
