package ilp_submission_3.ilp_submission_image.Service;


import com.fasterxml.jackson.databind.ObjectMapper;
import ilp_submission_3.ilp_submission_image.Configuration.ILPEndpointProvider;
import ilp_submission_3.ilp_submission_image.dto.*;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
@Service
public class DroneServiceImpl implements DroneService {
    private final RestTemplate restTemplate;

    private final ILPServiceInterface ilpService;

    private final ILPEndpointProvider endpointProvider;
    public DroneServiceImpl(RestTemplate restTemplate, ILPServiceInterface ilpService, ILPEndpointProvider endpointProvider) {
        this.restTemplate = restTemplate;
        this.ilpService = ilpService;
        this.endpointProvider = endpointProvider;
    }

    private static final double MOVE_DISTANCE = 0.00015;


    @Override
    public List<Drone> getAllDrones() {
        String url = endpointProvider.getEndpoint() + "/drones";
        Drone[] drones = restTemplate.getForObject(url, Drone[].class);

        List<Drone> droneList = new ArrayList<>();
        if (drones != null) {
            droneList.addAll(Arrays.asList(drones));
        }
        return droneList;
    }

    @Override
    public List<DroneForServicePoint> getAllServicePoints() {

        String url = endpointProvider.getEndpoint() + "/drones-for-service-points";
        DroneForServicePoint[] droneForServicePoints = restTemplate.getForObject(url, DroneForServicePoint[].class);

        List<DroneForServicePoint> droneForServicePointList = new ArrayList<>();
        if (droneForServicePoints != null) {
            droneForServicePointList.addAll(Arrays.asList(droneForServicePoints));
        }
        return droneForServicePointList;
    }

    @Override
    public List<DroneServicePoint> getServicePointLocations() {

        String url = endpointProvider.getEndpoint() + "/service-points";
        DroneServicePoint[] points = restTemplate.getForObject(url, DroneServicePoint[].class);
        if (points != null) {
            return Arrays.asList(points);
        } else {
            return new ArrayList<>();
        }
    }


    @Override
    public List<String> getDronesWithCooling(boolean hasCooling) {
        List<Drone> allDrones = getAllDrones();
        List<String> result = new ArrayList<>();

        for (Drone drone : allDrones) {
            if (drone.capability() != null) {
                Boolean cooling = drone.capability().cooling();
                boolean actualCooling = (cooling != null && cooling);

                if (actualCooling == hasCooling) {
                    result.add(drone.id());
                }
            }
        }
        return result;
    }
    @Override
    public Drone getDroneById(String id) {
        List<Drone> allDrones = getAllDrones();

        for (Drone drone : allDrones) {
            if (drone.id().equals(id)) {
                return drone;
            }
        }
        return null;
    }

    @Override
    public List<String> queryDronesByPath(String attributeName, String attributeValue) {
        List<Drone> allDrones = getAllDrones();
        List<String> result = new ArrayList<>();

        for (Drone drone : allDrones) {
            if (matchesAttribute(drone, attributeName, "=", attributeValue)) {
                result.add(drone.id());
            }
        }

        return result;
    }

    @Override
    public List<String> queryDrones(List<QueryAttribute> queryAttributes) {
        List<Drone> allDrones = getAllDrones();
        List<String> result = new ArrayList<>();

        for (Drone drone : allDrones) {
            boolean matchesAll = true;

            for (QueryAttribute query : queryAttributes) {
                if (!matchesAttribute(drone, query.attribute(), query.operator(), query.value())) {
                    matchesAll = false;
                    break;
                }
            }

            if (matchesAll) {
                result.add(drone.id());
            }
        }
        return result;
    }


    private boolean matchesAttribute(Drone drone, String attributeName, String operator, String value) {
        try {

            switch (attributeName) {
                case "id":
                    return compareValues(drone.id(), operator, value, true);
                case "name":
                    return compareValues(drone.name(), operator, value, false);

            }

            if (drone.capability() != null) {
                switch (attributeName) {
                    case "capacity":
                        return compareValues(drone.capability().capacity(), operator, value, true);
                    case "cooling":
                        return compareValues(drone.capability().cooling(), operator, value, false);
                    case "heating":
                        return compareValues(drone.capability().heating(), operator, value, false);
                    case "maxMoves":
                        return compareValues(drone.capability().maxMoves(), operator, value, true);
                    case "costPerMove":
                        return compareValues(drone.capability().costPerMove(), operator, value, true);
                    case "costInitial":
                        return compareValues(drone.capability().costInitial(), operator, value, true);
                    case "costFinal":
                        return compareValues(drone.capability().costFinal(), operator, value, true);
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean compareValues(Object droneValue, String operator, String queryValue, boolean isNumeric) {
        if (droneValue == null) {
            return false;
        }

        if (isNumeric) {
            double droneNum;
            double queryNum;

            try {
                if (droneValue instanceof Integer) {
                    droneNum = ((Integer) droneValue).doubleValue();
                } else if (droneValue instanceof Double) {
                    droneNum = (Double) droneValue;
                } else {
                    droneNum = Double.parseDouble(droneValue.toString());
                }

                queryNum = Double.parseDouble(queryValue);
            } catch (NumberFormatException e) {
                return false;
            }

            switch (operator) {
                case "=":
                    return Math.abs(droneNum - queryNum) < 0.0001;
                case "!=":
                    return Math.abs(droneNum - queryNum) >= 0.0001;
                case "<":
                    return droneNum < queryNum;
                case ">":
                    return droneNum > queryNum;
                case "<=":
                    return droneNum <= queryNum;
                case ">=":
                    return droneNum >= queryNum;
                default:
                    return false;
            }
        } else {

            String droneStr = droneValue.toString().toLowerCase();
            String queryStr = queryValue.toLowerCase();

            switch (operator) {
                case "=":
                    return droneStr.equals(queryStr);
                case "!=":
                    return !droneStr.equals(queryStr);
                default:
                    return false;
            }
        }
    }

    @Override
    public List<String> queryAvailableDrones(List<MedDispatchRec> dispatchRecords) {
        if (dispatchRecords == null || dispatchRecords.isEmpty()) {
            return new ArrayList<>();
        }

        // 检查冷热需求冲突
        for (MedDispatchRec record : dispatchRecords) {
            if (record.requirements() != null) {
                boolean requiresCooling = record.requirements().cooling() != null &&
                        record.requirements().cooling();
                boolean requiresHeating = record.requirements().heating() != null &&
                        record.requirements().heating();
                if (requiresCooling && requiresHeating) {
                    return new ArrayList<>();
                }
            }
        }

        // 按日期分组
        Map<String, List<MedDispatchRec>> dispatchesByDate = new HashMap<>();
        for (MedDispatchRec record : dispatchRecords) {
            String date = record.date();
            if (!dispatchesByDate.containsKey(date)) {
                dispatchesByDate.put(date, new ArrayList<>());
            }
            dispatchesByDate.get(date).add(record);
        }

        List<Drone> allDrones = getAllDrones();
        List<DroneForServicePoint> droneForServicePoints = getAllServicePoints();
        List<String> result = new ArrayList<>();

        // 对每个无人机检查
        for (Drone drone : allDrones) {
            if (canDroneHandleAllDateGroups(drone, dispatchesByDate, droneForServicePoints)) {
                result.add(drone.id());
            }
        }

        return result;
    }

    private boolean canDroneHandleAllDateGroups(Drone drone,
                                                Map<String, List<MedDispatchRec>> dispatchesByDate,
                                                List<DroneForServicePoint> droneForServicePoints) {
        if (drone.capability() == null) {
            return false;
        }

        DroneForServicePoint.DroneAvailability droneAvailability =
                findDroneAvailability(drone.id(), droneForServicePoints);

        // 遍历每个日期的配送组
        for (Map.Entry<String, List<MedDispatchRec>> entry : dispatchesByDate.entrySet()) {
            List<MedDispatchRec> dailyDispatches = entry.getValue();

            if (!canDroneHandleDailyDispatches(drone, dailyDispatches, droneAvailability)) {
                return false;
            }
        }

        return true;
    }

    private boolean canDroneHandleDailyDispatches(Drone drone,
                                                  List<MedDispatchRec> dailyDispatches,
                                                  DroneForServicePoint.DroneAvailability droneAvailability) {

        for (MedDispatchRec record : dailyDispatches) {
            if (!canDroneHandleDispatch(drone, record, droneAvailability)) {
                return false;
            }

            if (record.requirements() != null && record.requirements().capacity() != null) {
                if (record.requirements().capacity() > drone.capability().capacity()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public List<String> queryAvailableDronesWithOr(List<MedDispatchRec> dispatchRecords) {
        if (dispatchRecords == null || dispatchRecords.isEmpty()) {
            return new ArrayList<>();
        }

        for (MedDispatchRec record : dispatchRecords) {
            if (record.requirements() != null) {
                boolean requiresCooling = record.requirements().cooling() != null &&
                        record.requirements().cooling();
                boolean requiresHeating = record.requirements().heating() != null &&
                        record.requirements().heating();

                if (requiresCooling && requiresHeating) {
                    return new ArrayList<>();
                }
            }
        }

        List<Drone> allDrones = getAllDrones();
        List<DroneForServicePoint> droneForServicePoints = getAllServicePoints();
        List<String> result = new ArrayList<>();

        for (Drone drone : allDrones) {
            if (canDroneHandleAnyDispatch(drone, dispatchRecords, droneForServicePoints)) {
                result.add(drone.id());
            }
        }

        return result;
    }

    private boolean canDroneHandleAnyDispatch(Drone drone, List<MedDispatchRec> dispatchRecords,
                                              List<DroneForServicePoint> droneForServicePoints) {
        if (drone.capability() == null) {
            return false;
        }

        DroneForServicePoint.DroneAvailability droneAvailability =
                findDroneAvailability(drone.id(), droneForServicePoints);

        for (MedDispatchRec record : dispatchRecords) {
            if (canDroneHandleDispatchAny(drone, record, droneAvailability)) {
                return true;
            }
        }

        return false;
    }

    private DroneForServicePoint.DroneAvailability findDroneAvailability(String droneId, List<DroneForServicePoint> droneForServicePoints) {
        for (DroneForServicePoint sp : droneForServicePoints) {
            if (sp.drones() != null) {
                for (DroneForServicePoint.DroneAvailability da : sp.drones()) {
                    if (da.id().equals(droneId)) {
                        return da;
                    }
                }
            }
        }
        return null;
    }
    private boolean canDroneHandleDispatch(Drone drone, MedDispatchRec record,
                                           DroneForServicePoint.DroneAvailability droneAvailability) {
        if (drone.capability() == null || record.requirements() == null) {
            return false;
        }

        MedDispatchRec.Requirements req = record.requirements();


        // Check cooling
        boolean requiresCooling = req.cooling() != null && req.cooling();
        boolean hasCooling = drone.capability().cooling() != null && drone.capability().cooling();
        if (requiresCooling && !hasCooling) {
            return false;
        }

        // Check heating
        boolean requiresHeating = req.heating() != null && req.heating();
        boolean hasHeating = drone.capability().heating() != null && drone.capability().heating();
        if (requiresHeating && !hasHeating) {
            return false;
        }

        // Cooling and heating are mutually exclusive
        if (requiresCooling && requiresHeating) {
            return false;
        }

        // Check availability (date and time)
        if (record.date() != null && record.time() != null) {
            if (!isDroneAvailable(droneAvailability, record.date(), record.time())) {
                return false;
            }
        }

        // Check maxCost if present
        if (req.maxCost() != null) {
            if (!canAffordDelivery(drone, record, droneAvailability)) {
                return false;
            }
        }

        return true;
    }


    private boolean canAffordDelivery(Drone drone, MedDispatchRec record,
                                      DroneForServicePoint.DroneAvailability droneAvailability) {
        if (drone.capability() == null || record.requirements() == null ||
                record.requirements().maxCost() == null || record.delivery() == null) {
            return true;
        }

        Position servicePointLocation = findServicePointLocation(droneAvailability);
        if (servicePointLocation == null) {
            return false;
        }

        Position deliveryLocation = record.delivery();
        double distance = ilpService.distance(servicePointLocation, deliveryLocation);

        int moves = (int) Math.ceil(distance / MOVE_DISTANCE);
        int roundTripMoves = moves * 2;

        double costPerMove = 0.0;
        if (drone.capability().costPerMove() != null) {
            costPerMove = drone.capability().costPerMove();
        }

        double costInitial = 0.0;
        if (drone.capability().costInitial() != null) {
            costInitial = drone.capability().costInitial();
        }

        double costFinal = 0.0;
        if (drone.capability().costFinal() != null) {
            costFinal = drone.capability().costFinal();
        }

        double totalCost = costInitial + costFinal + (roundTripMoves * costPerMove);

        return totalCost <= record.requirements().maxCost();
    }

    private Position findServicePointLocation(DroneForServicePoint.DroneAvailability droneAvailability) {
        if (droneAvailability == null) {
            return null;
        }

        List<DroneForServicePoint> servicePoints = getAllServicePoints();

        for (DroneForServicePoint sp : servicePoints) {
            if (sp.drones() != null) {
                for (DroneForServicePoint.DroneAvailability da : sp.drones()) {
                    if (da.id().equals(droneAvailability.id())) {
                        return getServicePointLocationById(sp.servicePointId());
                    }
                }
            }
        }

        return null;
    }


    private Position getServicePointLocationById(Integer servicePointId) {
        List<DroneServicePoint> allServicePoints = getServicePointLocations();

        for (DroneServicePoint sp : allServicePoints) {
            if (sp.id().equals(servicePointId)) {
                if (sp.location() != null) {
                    return new Position(
                            sp.location().lng(),
                            sp.location().lat()
                    );
                }
            }
        }

        return null;
    }


    private boolean canDroneHandleDispatchAny(Drone drone, MedDispatchRec record,
                                              DroneForServicePoint.DroneAvailability droneAvailability) {
        if (drone.capability() == null || record.requirements() == null) {
            return false;
        }

        MedDispatchRec.Requirements req = record.requirements();

        if (req.capacity() != null) {
            if (drone.capability().capacity() == null ||
                    drone.capability().capacity() < req.capacity()) {
                return false;
            }
        }

        boolean requiresCooling = req.cooling() != null && req.cooling();
        boolean hasCooling = drone.capability().cooling() != null && drone.capability().cooling();
        if (requiresCooling && !hasCooling) {
            return false;
        }

        boolean requiresHeating = req.heating() != null && req.heating();
        boolean hasHeating = drone.capability().heating() != null && drone.capability().heating();
        if (requiresHeating && !hasHeating) {
            return false;
        }

        if (requiresCooling && requiresHeating) {
            return false;
        }

        if (record.date() != null && record.time() != null) {
            if (!isDroneAvailable(droneAvailability, record.date(), record.time())) {
                return false;
            }
        }
        return true;
    }

    private boolean isDroneAvailable(DroneForServicePoint.DroneAvailability droneAvailability, String dateStr, String timeStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            if (droneAvailability == null || droneAvailability.availability() == null
                    || droneAvailability.availability().isEmpty()) {
                return false;
            }

            for (DroneForServicePoint.DroneAvailability.Availability slot : droneAvailability.availability()) {
                if (slot.dayOfWeek() == null) {
                    continue;
                }

                DayOfWeek slotDay;
                try {
                    slotDay = DayOfWeek.valueOf(slot.dayOfWeek().toUpperCase());
                } catch (IllegalArgumentException e) {
                    continue;
                }

                if (slotDay.equals(dayOfWeek)) {
                    if (slot.from() == null || slot.until() == null) {
                        continue;
                    }

                    LocalTime fromTime = LocalTime.parse(slot.from());
                    LocalTime untilTime = LocalTime.parse(slot.until());

                    if (!time.isBefore(fromTime) && !time.isAfter(untilTime)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }
}


