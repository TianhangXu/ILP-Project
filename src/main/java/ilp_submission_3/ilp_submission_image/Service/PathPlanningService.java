package ilp_submission_3.ilp_submission_image.Service;

import ilp_submission_3.ilp_submission_image.dto.*;

import java.util.List;

public interface PathPlanningService {
    DeliveryPathResponse calculateDeliveryPath(List<MedDispatchRec> dispatchRecords);
    List<RestrictedArea> getRestrictedAreas();
    List<Position> calculateFlightPath(Position from, Position to, List<RestrictedArea> restrictedAreas);
    String calculateDeliveryPathAsGeoJson(List<MedDispatchRec> dispatchRecords);
}

