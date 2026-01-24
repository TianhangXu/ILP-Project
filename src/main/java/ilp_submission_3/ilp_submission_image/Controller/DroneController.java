package ilp_submission_3.ilp_submission_image.Controller;

import ilp_submission_3.ilp_submission_image.dto.DeliveryPathResponse;
import ilp_submission_3.ilp_submission_image.Service.DroneService;
import ilp_submission_3.ilp_submission_image.Service.PathPlanningService;
import ilp_submission_3.ilp_submission_image.dto.DeliveryPathResponse;
import ilp_submission_3.ilp_submission_image.dto.Drone;
import ilp_submission_3.ilp_submission_image.dto.MedDispatchRec;
import ilp_submission_3.ilp_submission_image.dto.QueryAttribute;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DroneController {
    private final DroneService droneService;
    private final PathPlanningService pathPlanningService;

    public DroneController(DroneService droneService, PathPlanningService pathPlanningService) {
        this.droneService = droneService;
        this.pathPlanningService = pathPlanningService;
    }

    @GetMapping("/dronesWithCooling/{state}")
    public ResponseEntity<List<String>> dronesWithCooling(@PathVariable boolean state) {
        List<String> droneIds = droneService.getDronesWithCooling(state);
        return ResponseEntity.ok(droneIds);
    }


    @GetMapping("/droneDetails/{id}")
    public ResponseEntity<Drone> droneDetails(@PathVariable String id) {
        Drone drone = droneService.getDroneById(id);
        if (drone == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(drone);
    }

    @GetMapping("/queryAsPath/{attributeName}/{attributeValue}")
    public ResponseEntity<List<String>> queryAsPath(
            @PathVariable String attributeName,
            @PathVariable String attributeValue) {
        List<String> droneIds = droneService.queryDronesByPath(attributeName, attributeValue);
        return ResponseEntity.ok(droneIds);
    }

    @PostMapping("/query")
    public ResponseEntity<List<String>> query(@RequestBody List<QueryAttribute> queryAttributes) {
        List<String> droneIds = droneService.queryDrones(queryAttributes);
        return ResponseEntity.ok(droneIds);
    }

    @PostMapping("/queryAvailableDrones")
    public ResponseEntity<List<String>> queryAvailableDrones(
            @RequestBody List<MedDispatchRec> dispatchRecords) {
        List<String> droneIds = droneService.queryAvailableDrones(dispatchRecords);
        return ResponseEntity.ok(droneIds);
    }

    @PostMapping("/calcDeliveryPath")
    public ResponseEntity<DeliveryPathResponse> calculateDeliveryPath(
            @RequestBody List<MedDispatchRec> dispatchRecords) {
        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatchRecords);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/calcDeliveryPathAsGeoJson")
    public ResponseEntity<String> calcDeliveryPathAsGeoJson(
            @RequestBody List<MedDispatchRec> dispatchRecords) {
        String geoJson = pathPlanningService.calculateDeliveryPathAsGeoJson(dispatchRecords);
        return ResponseEntity.ok(geoJson);
    }

}
