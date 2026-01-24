package ilp_submission_3.ilp_submission_image.Controller;

import ilp_submission_3.ilp_submission_image.Service.DroneService;
import ilp_submission_3.ilp_submission_image.Service.PathPlanningService;
import ilp_submission_3.ilp_submission_image.dto.Drone;
import ilp_submission_3.ilp_submission_image.dto.DroneServicePoint;
import ilp_submission_3.ilp_submission_image.dto.RestrictedArea;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class DataController {

    private final PathPlanningService pathPlanningService;
    private final DroneService droneService;

    public DataController(PathPlanningService pathPlanningService, DroneService droneService) {
        this.pathPlanningService = pathPlanningService;
        this.droneService = droneService;
    }

    /**
     * 获取所有禁飞区域
     * 返回格式与 ILP REST API 一致
     */
    @GetMapping("/restricted-areas")
    public ResponseEntity<List<RestrictedArea>> getRestrictedAreas() {
        List<RestrictedArea> areas = pathPlanningService.getRestrictedAreas();
        return ResponseEntity.ok(areas);
    }

    /**
     * 获取所有服务点
     */
    @GetMapping("/service-points")
    public ResponseEntity<List<DroneServicePoint>> getServicePoints() {
        List<DroneServicePoint> servicePoints = droneService.getServicePointLocations();
        return ResponseEntity.ok(servicePoints);
    }

    /**
     * 获取所有无人机
     */
    @GetMapping("/drones")
    public ResponseEntity<List<Drone>> getDrones() {
        List<Drone> drones = droneService.getAllDrones();
        return ResponseEntity.ok(drones);
    }
}