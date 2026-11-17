package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.farms.application.FarmSummaryView;
import br.com.curralzap.backend.farms.application.MyFarmsService;
import br.com.curralzap.backend.shared.api.ControllerSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/my-farms")
class MyFarmsController {

    private final MyFarmsService myFarmsService;

    MyFarmsController(MyFarmsService myFarmsService) {
        this.myFarmsService = myFarmsService;
    }

    @GetMapping
    MyFarmsApiResponse listFarms(HttpServletRequest request) {
        AuthTokenClaims claims = ControllerSupport.requireClaims(request);
        List<FarmSummaryView> farms = myFarmsService.listFarms(claims);
        int maxFarms = myFarmsService.maxFarmsForPlan(claims);
        return MyFarmsApiResponse.from(farms, maxFarms);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MyFarmsApiResponse.FarmEntry addFarm(
            HttpServletRequest request,
            @RequestBody CreateFarmApiRequest body
    ) {
        AuthTokenClaims claims = ControllerSupport.requireClaims(request);
        FarmSummaryView created = myFarmsService.addFarm(claims, body.farmName());
        return new MyFarmsApiResponse.FarmEntry(
                created.farmId().toString(),
                created.farmName(),
                created.role(),
                created.isCurrent()
        );
    }
}
