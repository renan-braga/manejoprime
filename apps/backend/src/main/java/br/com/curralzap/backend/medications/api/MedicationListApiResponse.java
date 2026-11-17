package br.com.curralzap.backend.medications.api;

import br.com.curralzap.backend.medications.application.MedicationListView;

import java.util.List;

record MedicationListApiResponse(
        List<MedicationApiResponse> items,
        long total,
        int limit,
        int offset
) {
    static MedicationListApiResponse from(MedicationListView medications) {
        return new MedicationListApiResponse(
                medications.items().stream().map(MedicationApiResponse::from).toList(),
                medications.total(),
                medications.limit(),
                medications.offset()
        );
    }
}
