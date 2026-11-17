package br.com.curralzap.backend.animals.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.shared.api.ImportResultApiResponse;
import br.com.curralzap.backend.animals.application.AnimalImportService;
import br.com.curralzap.backend.animals.application.AnimalListQuery;
import br.com.curralzap.backend.animals.application.AnimalReadService;
import br.com.curralzap.backend.animals.application.AnimalWriteService;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/animals")
class AnimalsController {

    private final AnimalReadService animalReadService;
    private final AnimalWriteService animalWriteService;
    private final AnimalImportService animalImportService;

    AnimalsController(
            AnimalReadService animalReadService,
            AnimalWriteService animalWriteService,
            AnimalImportService animalImportService
    ) {
        this.animalReadService = animalReadService;
        this.animalWriteService = animalWriteService;
        this.animalImportService = animalImportService;
    }

    @GetMapping
    AnimalListApiResponse listAnimals(
            HttpServletRequest request,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String lot,
            @RequestParam(required = false) String sexo,
            @RequestParam(required = false) String raca
    ) {
        AuthTokenClaims claims = requireClaims(request);
        AnimalListQuery query = AnimalListQuery.fromApi(limit, offset, search, status, lot, sexo, raca);
        return AnimalListApiResponse.from(animalReadService.list(claims, query));
    }

    @GetMapping("/{animalId}")
    AnimalDetailApiResponse animalDetail(
            HttpServletRequest request,
            @PathVariable UUID animalId
    ) {
        return AnimalDetailApiResponse.from(animalReadService.detail(requireClaims(request), animalId));
    }

    @PostMapping
    AnimalMutationApiResponse createAnimal(
            HttpServletRequest request,
            @RequestBody AnimalMutationApiRequest body
    ) {
        return AnimalMutationApiResponse.from(
                animalWriteService.create(requireClaims(request), body.toCommand())
        );
    }

    @DeleteMapping("/{animalId}")
    ResponseEntity<Void> deleteAnimal(
            HttpServletRequest request,
            @PathVariable UUID animalId
    ) {
        animalWriteService.delete(requireClaims(request), animalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{animalId}")
    AnimalMutationApiResponse updateAnimal(
            HttpServletRequest request,
            @PathVariable UUID animalId,
            @RequestBody AnimalMutationApiRequest body
    ) {
        return AnimalMutationApiResponse.from(
                animalWriteService.update(requireClaims(request), animalId, body.toCommand())
        );
    }

    @PostMapping("/import")
    ImportResultApiResponse importAnimals(
            HttpServletRequest request,
            @RequestBody AnimalImportApiRequest body
    ) {
        return ImportResultApiResponse.from(animalImportService.importAnimals(requireClaims(request), body.rows()));
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
