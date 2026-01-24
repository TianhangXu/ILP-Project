package ilp_submission_3.ilp_submission_image.Service;

import ilp_submission_3.ilp_submission_image.dto.*;

import java.util.List;

public interface DroneService {
    List<Drone> getAllDrones();
    List<String> getDronesWithCooling(boolean hasCooling);
    Drone getDroneById(String id);
    List<String> queryDronesByPath(String attributeName, String attributeValue);
    List<String> queryDrones(List<QueryAttribute> queryAttributes);
    List<DroneServicePoint> getServicePointLocations();
    List<String> queryAvailableDrones(List<MedDispatchRec> dispatchRecords);
    List<DroneForServicePoint> getAllServicePoints();
    List<String> queryAvailableDronesWithOr(List<MedDispatchRec> dispatchRecords);
}


