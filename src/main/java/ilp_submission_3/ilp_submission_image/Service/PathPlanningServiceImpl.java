package ilp_submission_3.ilp_submission_image.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ilp_submission_3.ilp_submission_image.Configuration.ILPEndpointProvider;
import ilp_submission_3.ilp_submission_image.WebSocket.PathfindingProgressHandler;
import ilp_submission_3.ilp_submission_image.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class PathPlanningServiceImpl implements PathPlanningService {

    private final RestTemplate restTemplate;
    private final ILPServiceInterface ilpService;
    private final DroneService droneService;
    private final ObjectMapper objectMapper;
    private final PathfindingProgressHandler progressHandler;

    private final ILPEndpointProvider endpointProvider;
    private static final double MOVE_DISTANCE = 0.00015;
    private static final Set<Double> VALID_ANGLES = Set.of(
            0.0, 22.5, 45.0, 67.5, 90.0, 112.5, 135.0, 157.5,
            180.0, 202.5, 225.0, 247.5, 270.0, 292.5, 315.0, 337.5
    );

    public PathPlanningServiceImpl(RestTemplate restTemplate, ILPEndpointProvider endpointProvider,
                                   ILPServiceInterface ilpService, DroneService droneService,
                                   PathfindingProgressHandler progressHandler) {
        this.restTemplate = restTemplate;
        this.ilpService = ilpService;
        this.droneService = droneService;
        this.progressHandler = progressHandler;
        this.objectMapper = new ObjectMapper();
        this.endpointProvider = endpointProvider;
    }

    @Override
    public List<RestrictedArea> getRestrictedAreas() {
        String url = endpointProvider.getEndpoint() + "/restricted-areas";
        RestrictedArea[] areas = restTemplate.getForObject(url, RestrictedArea[].class);
        if (areas != null) {
            return Arrays.asList(areas);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public DeliveryPathResponse calculateDeliveryPath(List<MedDispatchRec> dispatchRecords) {
        if (dispatchRecords == null || dispatchRecords.isEmpty()) {
            return new DeliveryPathResponse(0.0, 0, new ArrayList<>());
        }

        List<Drone> allDrones = droneService.getAllDrones();
        List<DroneForServicePoint> droneForServicePoints = droneService.getAllServicePoints();
        List<DroneServicePoint> droneServicePoints = droneService.getServicePointLocations();
        List<RestrictedArea> restrictedAreas = getRestrictedAreas();

        List<String> singleDroneCandidates = droneService.queryAvailableDrones(dispatchRecords);
        DeliveryPathResponse singleDroneSolution = null;
        if (!singleDroneCandidates.isEmpty()) {
            singleDroneSolution = trySingleDroneSolution(
                    dispatchRecords, singleDroneCandidates, allDrones,
                    droneForServicePoints, droneServicePoints, restrictedAreas
            );
        }

        List<String> multiDroneCandidates = droneService.queryAvailableDronesWithOr(dispatchRecords);
        DeliveryPathResponse multiDroneSolution = null;
        if (!multiDroneCandidates.isEmpty()) {
            multiDroneSolution = tryMultiDroneSolution(
                    dispatchRecords, multiDroneCandidates, allDrones,
                    droneForServicePoints, droneServicePoints, restrictedAreas
            );
        }

        return chooseBestSolution(singleDroneSolution, multiDroneSolution);
    }

    private DeliveryPathResponse chooseBestSolution(
            DeliveryPathResponse singleDroneSolution,
            DeliveryPathResponse multiDroneSolution) {

        if (singleDroneSolution != null && multiDroneSolution != null) {
            if (singleDroneSolution.totalCost() <= multiDroneSolution.totalCost()) {
                return singleDroneSolution;
            } else {
                return multiDroneSolution;
            }
        }

        if (singleDroneSolution != null) {
            return singleDroneSolution;
        }

        if (multiDroneSolution != null) {
            return multiDroneSolution;
        }

        return new DeliveryPathResponse(0.0, 0, new ArrayList<>());
    }

    private DeliveryPathResponse trySingleDroneSolution(
            List<MedDispatchRec> dispatchRecords,
            List<String> availableDroneIds,
            List<Drone> allDrones,
            List<DroneForServicePoint> droneForServicePoints,
            List<DroneServicePoint> droneServicePoints,
            List<RestrictedArea> restrictedAreas) {

        if (availableDroneIds == null || availableDroneIds.isEmpty()) {
            return null;
        }

        Position centroid = calculateCentroid(dispatchRecords);

        List<ServicePointDistance> sortedSPs = new ArrayList<>();
        for (DroneServicePoint sp : droneServicePoints) {
            if (sp.location() != null) {
                Position spPos = new Position(sp.location().lng(), sp.location().lat());
                double distance = ilpService.distance(centroid, spPos);
                sortedSPs.add(new ServicePointDistance(sp, distance));
            }
        }
        sortedSPs.sort(Comparator.comparingDouble(d -> d.distance));

        for (ServicePointDistance spDist : sortedSPs) {
            DroneServicePoint servicePoint = spDist.servicePoint;
            Position spPosition = new Position(
                    servicePoint.location().lng(),
                    servicePoint.location().lat()
            );

            List<String> dronesAtThisSP = new ArrayList<>();
            for (DroneForServicePoint sp : droneForServicePoints) {
                if (!sp.servicePointId().equals(servicePoint.id())) continue;
                if (sp.drones() == null) continue;

                for (DroneForServicePoint.DroneAvailability da : sp.drones()) {
                    if (availableDroneIds.contains(da.id())) {
                        dronesAtThisSP.add(da.id());
                    }
                }
            }

            if (dronesAtThisSP.isEmpty()) {
                continue;
            }

            for (String droneId : dronesAtThisSP) {
                Drone drone = findDroneById(droneId, allDrones);
                if (drone == null || drone.capability() == null) continue;

                List<MedDispatchRec> optimizedOrder = optimizeDeliveryOrder(
                        dispatchRecords, spPosition
                );

                DeliveryPathResponse solution = tryDeliverySequence(
                        drone, optimizedOrder, restrictedAreas,
                        droneForServicePoints, droneServicePoints
                );

                if (solution != null) {
                    return solution;
                }
            }
        }

        return null;
    }

    private Position calculateCentroid(List<MedDispatchRec> dispatches) {
        double sumLng = 0.0;
        double sumLat = 0.0;

        for (MedDispatchRec dispatch : dispatches) {
            sumLng += dispatch.delivery().lng();
            sumLat += dispatch.delivery().lat();
        }

        int count = dispatches.size();
        return new Position(sumLng / count, sumLat / count);
    }

    private static class ServicePointDistance {
        DroneServicePoint servicePoint;
        double distance;

        ServicePointDistance(DroneServicePoint sp, double distance) {
            this.servicePoint = sp;
            this.distance = distance;
        }
    }

    private DeliveryPathResponse tryMultiDroneSolution(
            List<MedDispatchRec> dispatchRecords,
            List<String> availableDroneIds,
            List<Drone> allDrones,
            List<DroneForServicePoint> droneForServicePoints,
            List<DroneServicePoint> droneServicePoints,
            List<RestrictedArea> restrictedAreas) {

        if (availableDroneIds == null || availableDroneIds.isEmpty()) {
            return null;
        }

        Map<String, List<MedDispatchRec>> dispatchesByDate = new HashMap<>();
        for (MedDispatchRec record : dispatchRecords) {
            String date = record.date() != null ? record.date() : "unknown";
            dispatchesByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(record);
        }

        List<DeliveryPathResponse.DronePath> allDronePaths = new ArrayList<>();
        double totalCost = 0.0;
        int totalMoves = 0;

        for (Map.Entry<String, List<MedDispatchRec>> entry : dispatchesByDate.entrySet()) {
            List<MedDispatchRec> dailyDispatches = new ArrayList<>(entry.getValue());

            while (!dailyDispatches.isEmpty()) {
                GreedyAssignment assignment = findBestAssignmentByDistance(
                        dailyDispatches,
                        availableDroneIds,
                        allDrones,
                        droneForServicePoints,
                        droneServicePoints,
                        restrictedAreas
                );

                if (assignment == null) {
                    return null;
                }

                allDronePaths.add(assignment.dronePath);
                totalCost += assignment.cost;
                totalMoves += assignment.moves;
                dailyDispatches.removeAll(assignment.completedOrders);
            }
        }

        return mergeDronePaths(allDronePaths, totalCost, totalMoves);
    }

    private GreedyAssignment findBestAssignmentByDistance(
            List<MedDispatchRec> remainingOrders,
            List<String> availableDroneIds,
            List<Drone> allDrones,
            List<DroneForServicePoint> droneForServicePoints,
            List<DroneServicePoint> droneServicePoints,
            List<RestrictedArea> restrictedAreas) {

        MedDispatchRec anchorOrder = remainingOrders.get(0);
        Position anchorPos = anchorOrder.delivery();

        List<ServicePointDistance> sortedSPs = new ArrayList<>();
        for (DroneServicePoint sp : droneServicePoints) {
            if (sp.location() != null) {
                Position spPos = new Position(sp.location().lng(), sp.location().lat());
                double distance = ilpService.distance(anchorPos, spPos);
                sortedSPs.add(new ServicePointDistance(sp, distance));
            }
        }
        sortedSPs.sort(Comparator.comparingDouble(d -> d.distance));

        for (ServicePointDistance spDist : sortedSPs) {
            DroneServicePoint servicePoint = spDist.servicePoint;
            Position spPosition = new Position(
                    servicePoint.location().lng(),
                    servicePoint.location().lat()
            );

            List<String> dronesAtThisSP = new ArrayList<>();
            for (DroneForServicePoint sp : droneForServicePoints) {
                if (!sp.servicePointId().equals(servicePoint.id())) continue;
                if (sp.drones() == null) continue;

                for (DroneForServicePoint.DroneAvailability da : sp.drones()) {
                    String droneId = da.id();
                    if (availableDroneIds.contains(droneId)) {
                        dronesAtThisSP.add(droneId);
                    }
                }
            }

            if (dronesAtThisSP.isEmpty()) {
                continue;
            }

            for (String droneId : dronesAtThisSP) {
                Drone drone = findDroneById(droneId, allDrones);
                if (drone == null || drone.capability() == null) {
                    continue;
                }

                List<MedDispatchRec> batch = buildBatchForDrone(
                        anchorOrder,
                        remainingOrders,
                        drone,
                        droneForServicePoints
                );

                if (batch.isEmpty()) {
                    continue;
                }

                DeliveryPathResponse solution = tryDeliverySequence(
                        drone, batch, restrictedAreas,
                        droneForServicePoints, droneServicePoints
                );

                if (solution == null) {
                    continue;
                }

                if (solution.dronePaths().isEmpty()) {
                    continue;
                }

                return new GreedyAssignment(
                        droneId,
                        solution.dronePaths().get(0),
                        solution.totalCost(),
                        solution.totalMoves(),
                        batch
                );
            }
        }

        return null;
    }

    private List<MedDispatchRec> buildBatchForDrone(
            MedDispatchRec anchorOrder,
            List<MedDispatchRec> remainingOrders,
            Drone drone,
            List<DroneForServicePoint> droneForServicePoints) {

        List<MedDispatchRec> batch = new ArrayList<>();
        double currentCapacity = 0.0;
        double maxCapacity = drone.capability().capacity() != null ?
                drone.capability().capacity() : Double.MAX_VALUE;

        if (!canDroneHandleOrder(drone, anchorOrder, droneForServicePoints)) {
            return batch;
        }

        double anchorCapacity = anchorOrder.requirements() != null &&
                anchorOrder.requirements().capacity() != null ?
                anchorOrder.requirements().capacity() : 0.0;

        if (anchorCapacity > maxCapacity) {
            return batch;
        }

        batch.add(anchorOrder);
        currentCapacity += anchorCapacity;

        List<MedDispatchRec> candidates = new ArrayList<>(remainingOrders);
        candidates.remove(anchorOrder);

        Position anchorPos = anchorOrder.delivery();
        candidates.sort(Comparator.comparingDouble(order ->
                ilpService.distance(anchorPos, order.delivery())
        ));

        for (MedDispatchRec candidate : candidates) {
            if (batch.size() >= 3) {
                break;
            }

            if (!Objects.equals(candidate.date(), anchorOrder.date())) {
                continue;
            }

            if (!canDroneHandleOrder(drone, candidate, droneForServicePoints)) {
                continue;
            }

            double requiredCapacity = candidate.requirements() != null &&
                    candidate.requirements().capacity() != null ?
                    candidate.requirements().capacity() : 0.0;

            if (currentCapacity + requiredCapacity <= maxCapacity) {
                batch.add(candidate);
                currentCapacity += requiredCapacity;
            }
        }

        return batch;
    }

    private boolean canDroneHandleOrder(
            Drone drone,
            MedDispatchRec order,
            List<DroneForServicePoint> droneForServicePoints) {

        if (drone.capability() == null || order.requirements() == null) {
            return false;
        }

        DroneForServicePoint.DroneAvailability availability =
                findDroneAvailabilityForOrder(
                        drone.id(),
                        order.date(),
                        order.time(),
                        droneForServicePoints
                );

        if (availability == null) {
            return false;
        }

        boolean requiresCooling = order.requirements().cooling() != null &&
                order.requirements().cooling();
        boolean hasCooling = drone.capability().cooling() != null &&
                drone.capability().cooling();
        if (requiresCooling && !hasCooling) {
            return false;
        }

        boolean requiresHeating = order.requirements().heating() != null &&
                order.requirements().heating();
        boolean hasHeating = drone.capability().heating() != null &&
                drone.capability().heating();
        if (requiresHeating && !hasHeating) {
            return false;
        }

        if (order.requirements().capacity() != null) {
            double required = order.requirements().capacity();
            double available = drone.capability().capacity() != null ?
                    drone.capability().capacity() : 0.0;
            if (required > available) {
                return false;
            }
        }

        return true;
    }

    private DroneForServicePoint.DroneAvailability findDroneAvailabilityForOrder(
            String droneId,
            String date,
            String time,
            List<DroneForServicePoint> droneForServicePoints) {

        if (date == null || time == null) {
            return null;
        }

        try {
            LocalDate orderDate = LocalDate.parse(date);
            LocalTime orderTime = LocalTime.parse(time);
            DayOfWeek dayOfWeek = orderDate.getDayOfWeek();

            for (DroneForServicePoint sp : droneForServicePoints) {
                if (sp.drones() == null) continue;

                for (DroneForServicePoint.DroneAvailability da : sp.drones()) {
                    if (!da.id().equals(droneId)) continue;

                    if (da.availability() == null) continue;

                    for (DroneForServicePoint.DroneAvailability.Availability slot : da.availability()) {
                        if (slot.dayOfWeek() == null) continue;

                        DayOfWeek slotDay;
                        try {
                            slotDay = DayOfWeek.valueOf(slot.dayOfWeek().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            continue;
                        }

                        if (slotDay.equals(dayOfWeek)) {
                            if (slot.from() == null || slot.until() == null) continue;

                            LocalTime fromTime = LocalTime.parse(slot.from());
                            LocalTime untilTime = LocalTime.parse(slot.until());

                            if (!orderTime.isBefore(fromTime) && !orderTime.isAfter(untilTime)) {
                                return da;
                            }
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private DeliveryPathResponse mergeDronePaths(
            List<DeliveryPathResponse.DronePath> allPaths,
            double totalCost,
            int totalMoves) {

        Map<String, List<DeliveryPathResponse.Delivery>> pathsByDrone = new HashMap<>();

        for (DeliveryPathResponse.DronePath path : allPaths) {
            String droneId = path.droneId();
            pathsByDrone.computeIfAbsent(droneId, k -> new ArrayList<>())
                    .addAll(path.deliveries());
        }

        List<DeliveryPathResponse.DronePath> mergedPaths = new ArrayList<>();
        for (Map.Entry<String, List<DeliveryPathResponse.Delivery>> entry : pathsByDrone.entrySet()) {
            mergedPaths.add(new DeliveryPathResponse.DronePath(
                    entry.getKey(),
                    entry.getValue()
            ));
        }

        return new DeliveryPathResponse(totalCost, totalMoves, mergedPaths);
    }

    private static class GreedyAssignment {
        String droneId;
        DeliveryPathResponse.DronePath dronePath;
        double cost;
        int moves;
        List<MedDispatchRec> completedOrders;

        GreedyAssignment(String droneId, DeliveryPathResponse.DronePath dronePath,
                         double cost, int moves, List<MedDispatchRec> completedOrders) {
            this.droneId = droneId;
            this.dronePath = dronePath;
            this.cost = cost;
            this.moves = moves;
            this.completedOrders = completedOrders;
        }
    }

    private Drone findDroneById(String droneId, List<Drone> drones) {
        for (Drone d : drones) {
            if (d.id().equals(droneId)) {
                return d;
            }
        }
        return null;
    }

    @Override
    public String calculateDeliveryPathAsGeoJson(List<MedDispatchRec> dispatchRecords) {
        DeliveryPathResponse response = calculateDeliveryPath(dispatchRecords);

        Map<String, Object> geoJson = new HashMap<>();
        geoJson.put("type", "FeatureCollection");

        List<Map<String, Object>> features = new ArrayList<>();

        if (response.dronePaths().isEmpty()) {
            geoJson.put("features", features);
            try {
                return objectMapper.writeValueAsString(geoJson);
            } catch (JsonProcessingException e) {
                return "{}";
            }
        }

        for (int i = 0; i < response.dronePaths().size(); i++) {
            DeliveryPathResponse.DronePath dronePath = response.dronePaths().get(i);

            List<List<Double>> coordinates = new ArrayList<>();
            for (DeliveryPathResponse.Delivery delivery : dronePath.deliveries()) {
                for (Position pos : delivery.flightPath()) {
                    coordinates.add(Arrays.asList(pos.lng(), pos.lat()));
                }
            }

            if (!coordinates.isEmpty()) {
                Map<String, Object> feature = new HashMap<>();
                feature.put("type", "Feature");

                Map<String, Object> geometry = new HashMap<>();
                geometry.put("type", "LineString");
                geometry.put("coordinates", coordinates);
                feature.put("geometry", geometry);

                Map<String, Object> properties = new HashMap<>();
                properties.put("name", "Drone " + (i + 1) + " Path");
                properties.put("stroke", getColorForDrone(i));
                properties.put("stroke-width", 3);
                properties.put("stroke-opacity", 0.8);
                feature.put("properties", properties);

                features.add(feature);
            }
        }

        geoJson.put("features", features);

        try {
            return objectMapper.writeValueAsString(geoJson);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String getColorForDrone(int index) {
        String[] colors = {
                "#0000FF", "#00FF00", "#FF00FF", "#00FFFF", "#FFA500",
                "#FFD700", "#FF1493", "#00CED1", "#9370DB", "#32CD32"
        };
        return colors[index % colors.length];
    }

    private List<MedDispatchRec> optimizeDeliveryOrder(
            List<MedDispatchRec> dispatches, Position start) {

        List<MedDispatchRec> remaining = new ArrayList<>(dispatches);
        List<MedDispatchRec> ordered = new ArrayList<>();
        Position current = start;

        while (!remaining.isEmpty()) {
            MedDispatchRec nearest = null;
            double minDistance = Double.MAX_VALUE;

            for (MedDispatchRec dispatch : remaining) {
                Position deliveryPos = getDeliveryPosition(dispatch);
                double distance = ilpService.distance(current, deliveryPos);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = dispatch;
                }
            }

            if (nearest != null) {
                ordered.add(nearest);
                remaining.remove(nearest);
                current = getDeliveryPosition(nearest);
            } else {
                break;
            }
        }

        return ordered;
    }

    private DeliveryPathResponse tryDeliverySequence(
            Drone drone, List<MedDispatchRec> dispatches,
            List<RestrictedArea> restrictedAreas,
            List<DroneForServicePoint> droneForServicePoints,
            List<DroneServicePoint> droneServicePoints) {

        if (drone.capability() == null) return null;

        Map<String, List<MedDispatchRec>> dispatchesByDate = new HashMap<>();
        for (MedDispatchRec record : dispatches) {
            String date = record.date();
            if (date == null) date = "unknown";
            if (!dispatchesByDate.containsKey(date)) {
                dispatchesByDate.put(date, new ArrayList<>());
            }
            dispatchesByDate.get(date).add(record);
        }

        List<DeliveryPathResponse.Delivery> allDeliveries = new ArrayList<>();
        double totalCost = 0.0;
        int totalMoves = 0;

        for (Map.Entry<String, List<MedDispatchRec>> entry : dispatchesByDate.entrySet()) {
            List<MedDispatchRec> dailyDispatches = entry.getValue();

            List<List<MedDispatchRec>> batches = createBatchesByServicePointAndCapacity(
                    dailyDispatches,
                    drone.capability().capacity(),
                    drone,
                    droneForServicePoints
            );

            if (batches.isEmpty()) {
                return null;
            }

            for (List<MedDispatchRec> batch : batches) {
                Position currentServicePoint = findServicePointForBatch(
                        drone, batch, droneForServicePoints, droneServicePoints
                );

                if (currentServicePoint == null) {
                    return null;
                }

                DeliveryBatchResult batchResult = executeDeliveryBatch(
                        drone, currentServicePoint, batch, restrictedAreas
                );

                if (batchResult == null) {
                    return null;
                }

                allDeliveries.addAll(batchResult.deliveries);
                totalCost += batchResult.cost;
                totalMoves += batchResult.moves;
            }
        }

        List<DeliveryPathResponse.DronePath> dronePaths = new ArrayList<>();
        dronePaths.add(new DeliveryPathResponse.DronePath(drone.id(), allDeliveries));

        return new DeliveryPathResponse(totalCost, totalMoves, dronePaths);
    }

    private Position findServicePointForBatch(
            Drone drone,
            List<MedDispatchRec> batch,
            List<DroneForServicePoint> droneForServicePoints,
            List<DroneServicePoint> droneServicePoints) {

        if (batch.isEmpty()) return null;

        MedDispatchRec firstOrder = batch.get(0);

        DroneForServicePoint.DroneAvailability availability =
                findDroneAvailabilityForOrder(
                        drone.id(),
                        firstOrder.date(),
                        firstOrder.time(),
                        droneForServicePoints
                );

        if (availability == null) return null;

        Integer servicePointId = null;
        for (DroneForServicePoint sp : droneForServicePoints) {
            if (sp.drones() != null) {
                for (DroneForServicePoint.DroneAvailability da : sp.drones()) {
                    if (da.id().equals(drone.id()) && da.equals(availability)) {
                        servicePointId = sp.servicePointId();
                        break;
                    }
                }
            }
            if (servicePointId != null) break;
        }

        if (servicePointId == null) return null;

        for (DroneServicePoint sp : droneServicePoints) {
            if (sp.id().equals(servicePointId) && sp.location() != null) {
                return new Position(sp.location().lng(), sp.location().lat());
            }
        }

        return null;
    }

    private List<List<MedDispatchRec>> createBatchesByServicePointAndCapacity(
            List<MedDispatchRec> dispatches,
            Double droneCapacity,
            Drone drone,
            List<DroneForServicePoint> droneForServicePoints) {

        if (droneCapacity == null || droneCapacity <= 0) {
            return Collections.emptyList();
        }

        Map<Integer, List<MedDispatchRec>> dispatchesByServicePoint = new LinkedHashMap<>();

        for (MedDispatchRec dispatch : dispatches) {
            DroneForServicePoint.DroneAvailability availability =
                    findDroneAvailabilityForOrder(
                            drone.id(),
                            dispatch.date(),
                            dispatch.time(),
                            droneForServicePoints
                    );

            if (availability == null) continue;

            Integer servicePointId = null;
            for (DroneForServicePoint sp : droneForServicePoints) {
                if (sp.drones() != null) {
                    for (DroneForServicePoint.DroneAvailability da : sp.drones()) {
                        if (da.id().equals(drone.id()) && da == availability) {
                            servicePointId = sp.servicePointId();
                            break;
                        }
                    }
                }
                if (servicePointId != null) break;
            }

            if (servicePointId != null) {
                dispatchesByServicePoint
                        .computeIfAbsent(servicePointId, k -> new ArrayList<>())
                        .add(dispatch);
            }
        }

        List<List<MedDispatchRec>> allBatches = new ArrayList<>();

        for (List<MedDispatchRec> servicePointDispatches : dispatchesByServicePoint.values()) {
            List<MedDispatchRec> currentBatch = new ArrayList<>();
            double currentCapacity = 0.0;

            for (MedDispatchRec dispatch : servicePointDispatches) {
                double requiredCapacity = 0.0;
                if (dispatch.requirements() != null &&
                        dispatch.requirements().capacity() != null) {
                    requiredCapacity = dispatch.requirements().capacity();
                }

                if (requiredCapacity > droneCapacity) {
                    return Collections.emptyList();
                }

                if (currentCapacity + requiredCapacity > droneCapacity &&
                        !currentBatch.isEmpty()) {
                    allBatches.add(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                    currentCapacity = 0.0;
                }

                currentBatch.add(dispatch);
                currentCapacity += requiredCapacity;
            }

            if (!currentBatch.isEmpty()) {
                allBatches.add(currentBatch);
            }
        }

        return allBatches;
    }

    private DeliveryBatchResult executeDeliveryBatch(
            Drone drone,
            Position servicePoint,
            List<MedDispatchRec> batch,
            List<RestrictedArea> restrictedAreas) {

        if (progressHandler.hasActiveConnections()) {
            progressHandler.broadcastProgress(
                    PathfindingProgress.batchStarted(1, drone.id(), batch.size())
            );
        }

        for (MedDispatchRec dispatch : batch) {
            Position deliveryPos = getDeliveryPosition(dispatch);
            if (isInRestrictedArea(deliveryPos, restrictedAreas)) {
                if (progressHandler.hasActiveConnections()) {
                    progressHandler.broadcastProgress(
                            PathfindingProgress.error(
                                    "Delivery " + dispatch.id() + " location is in restricted area"
                            )
                    );
                }
                return null;
            }
        }

        List<DeliveryPathResponse.Delivery> deliveries = new ArrayList<>();
        Position currentPos = servicePoint;
        int totalMoves = 0;

        for (int i = 0; i < batch.size(); i++) {
            MedDispatchRec dispatch = batch.get(i);
            Position deliveryPos = getDeliveryPosition(dispatch);

            if (progressHandler.hasActiveConnections()) {
                progressHandler.broadcastProgress(
                        PathfindingProgress.deliveryStarted(dispatch.id(), currentPos, deliveryPos)
                );
            }

            List<Position> pathToDelivery = calculateFlightPath(currentPos, deliveryPos, restrictedAreas);

            if (pathToDelivery.isEmpty()) {
                if (progressHandler.hasActiveConnections()) {
                    progressHandler.broadcastProgress(
                            PathfindingProgress.error("Cannot find path to delivery " + dispatch.id())
                    );
                }
                return null;
            }

            List<Position> flightPath = new ArrayList<>(pathToDelivery);
            flightPath.add(deliveryPos);
            flightPath.add(deliveryPos);

            totalMoves += pathToDelivery.size() + 1;
            deliveries.add(new DeliveryPathResponse.Delivery(dispatch.id(), flightPath));
            currentPos = deliveryPos;
        }

        List<Position> returnPath = calculateFlightPath(currentPos, servicePoint, restrictedAreas);
        if (returnPath.isEmpty()) {
            if (progressHandler.hasActiveConnections()) {
                progressHandler.broadcastProgress(
                        PathfindingProgress.error("Cannot find return path to service point")
                );
            }
            return null;
        }

        List<Position> returnFlightPath = new ArrayList<>(returnPath);
        returnFlightPath.add(servicePoint);

        deliveries.add(new DeliveryPathResponse.Delivery(null, returnFlightPath));
        totalMoves += returnPath.size();

        if (drone.capability().maxMoves() != null && totalMoves > drone.capability().maxMoves()) {
            return null;
        }

        if (drone.capability().maxMoves() == null && totalMoves > 5000) {
            return null;
        }

        double costPerMove = drone.capability().costPerMove() != null ?
                drone.capability().costPerMove() : 0.0;
        double costInitial = drone.capability().costInitial() != null ?
                drone.capability().costInitial() : 0.0;
        double costFinal = drone.capability().costFinal() != null ?
                drone.capability().costFinal() : 0.0;

        double batchCost = costInitial + (totalMoves * costPerMove) + costFinal;

        int numDispatches = batch.size();
        if (numDispatches > 0) {
            double perDeliveryCost = batchCost / numDispatches;

            for (MedDispatchRec dispatch : batch) {
                if (dispatch.requirements() != null &&
                        dispatch.requirements().maxCost() != null) {
                    double maxCost = dispatch.requirements().maxCost();
                    if (perDeliveryCost > maxCost) {
                        return null;
                    }
                }
            }
        }

        if (progressHandler.hasActiveConnections()) {
            progressHandler.broadcastProgress(
                    PathfindingProgress.batchCompleted(1, drone.id(), batchCost, totalMoves)
            );
        }

        return new DeliveryBatchResult(deliveries, batchCost, totalMoves);
    }

    private static class DeliveryBatchResult {
        List<DeliveryPathResponse.Delivery> deliveries;
        double cost;
        int moves;

        DeliveryBatchResult(List<DeliveryPathResponse.Delivery> deliveries, double cost, int moves) {
            this.deliveries = deliveries;
            this.cost = cost;
            this.moves = moves;
        }
    }

    @Override
    public List<Position> calculateFlightPath(Position from, Position to,
                                              List<RestrictedArea> restrictedAreas) {
        return aStarPathfinding(from, to, restrictedAreas);
    }

    private List<Position> aStarPathfinding(Position from, Position to,
                                            List<RestrictedArea> restrictedAreas) {

        if (ilpService.isClose(from, to)) {
            return Arrays.asList(from, to);
        }

        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(
                Comparator.comparingDouble((AStarNode node) -> node.fScore)
                        .thenComparingDouble(node -> node.gScore)
        );

        Set<String> closedSet = new HashSet<>();
        Map<String, Position> cameFrom = new HashMap<>();
        Map<String, Double> gScore = new HashMap<>();

        String fromKey = positionToKey(from);
        openSet.add(new AStarNode(from, 0.0, improvedHeuristic(from, to)));
        gScore.put(fromKey, 0.0);

        int nodesExplored = 0;

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            String currentKey = positionToKey(current.position);

            nodesExplored++;

            if (progressHandler.hasActiveConnections() && nodesExplored % 10 == 0) {
                progressHandler.broadcastProgress(
                        PathfindingProgress.nodeExplored(current.position, nodesExplored)
                );
            }

            if (ilpService.isClose(current.position, to)) {
                List<Position> path = reconstructPath(cameFrom, current.position, from, to);

                if (progressHandler.hasActiveConnections()) {
                    progressHandler.broadcastProgress(
                            PathfindingProgress.pathFound(null, nodesExplored, path.size())
                    );
                }

                return path;
            }

            if (closedSet.contains(currentKey)) {
                continue;
            }
            closedSet.add(currentKey);

            double currentG = gScore.getOrDefault(currentKey, Double.MAX_VALUE);
            List<AngleScore> sortedAngles = getSortedAngles(current.position, to);

            for (AngleScore angleScore : sortedAngles) {
                Position neighbor = ilpService.nextPosition(current.position, angleScore.angle);
                String neighborKey = positionToKey(neighbor);

                if (closedSet.contains(neighborKey) ||
                        isInRestrictedArea(neighbor, restrictedAreas) ||
                        isPathThroughRestrictedArea(current.position, neighbor, restrictedAreas)) {
                    continue;
                }

                double tentativeG = currentG + 1.0;
                double neighborG = gScore.getOrDefault(neighborKey, Double.MAX_VALUE);

                if (tentativeG < neighborG) {
                    cameFrom.put(neighborKey, current.position);
                    gScore.put(neighborKey, tentativeG);
                    double h = improvedHeuristic(neighbor, to);
                    openSet.add(new AStarNode(neighbor, tentativeG, tentativeG + 1.3 * h));
                }
            }
        }

        if (progressHandler.hasActiveConnections()) {
            progressHandler.broadcastProgress(
                    PathfindingProgress.error("No path found after exploring " + nodesExplored + " nodes")
            );
        }

        return new ArrayList<>();
    }

    private double improvedHeuristic(Position from, Position to) {
        double euclideanDistance = ilpService.distance(from, to);
        double euclideanMoves = euclideanDistance / MOVE_DISTANCE;

        double dlng = Math.abs(to.lng() - from.lng());
        double dlat = Math.abs(to.lat() - from.lat());

        double diagonalMoves = Math.min(dlng, dlat) / MOVE_DISTANCE;
        double straightMoves = Math.abs(dlng - dlat) / MOVE_DISTANCE;

        double octileDistance = (diagonalMoves * Math.sqrt(2)) + straightMoves;

        return Math.min(euclideanMoves, octileDistance);
    }

    private List<AngleScore> getSortedAngles(Position current, Position target) {
        List<AngleScore> angles = new ArrayList<>();

        double targetAngle = calculateAngle(current, target);

        for (Double angle : VALID_ANGLES) {
            double angleDiff = Math.abs(normalizeAngle(angle - targetAngle));

            if (angleDiff > 180) {
                angleDiff = 360 - angleDiff;
            }
            angles.add(new AngleScore(angle, angleDiff));
        }

        angles.sort(Comparator.comparingDouble(a -> a.score));

        return angles;
    }

    private double calculateAngle(Position from, Position to) {
        double dlng = to.lng() - from.lng();
        double dlat = to.lat() - from.lat();

        double angleRad = Math.atan2(dlat, dlng);
        double angleDeg = Math.toDegrees(angleRad);

        return normalizeAngle(angleDeg);
    }

    private double normalizeAngle(double angle) {
        angle = angle % 360.0;
        if (angle < 0) {
            angle += 360.0;
        }
        return angle;
    }

    private static class AngleScore {
        Double angle;
        double score;

        AngleScore(Double angle, double score) {
            this.angle = angle;
            this.score = score;
        }
    }

    private String positionToKey(Position pos) {
        long lngRounded = Math.round(pos.lng() * 1e10);
        long latRounded = Math.round(pos.lat() * 1e10);
        return lngRounded + "," + latRounded;
    }

    private static class AStarNode {
        Position position;
        double gScore;
        double fScore;

        AStarNode(Position position, double gScore, double fScore) {
            this.position = position;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    private List<Position> reconstructPath(Map<String, Position> cameFrom,
                                           Position current, Position start, Position goal) {
        List<Position> path = new ArrayList<>();
        path.add(current);

        String currentKey = positionToKey(current);

        while (cameFrom.containsKey(currentKey)) {
            Position parent = cameFrom.get(currentKey);
            path.add(parent);
            currentKey = positionToKey(parent);
        }

        Collections.reverse(path);
        return path;
    }

    private boolean isInRestrictedArea(Position pos, List<RestrictedArea> restrictedAreas) {
        for (RestrictedArea area : restrictedAreas) {
            if (area.vertices() != null && !area.vertices().isEmpty()) {
                if (ilpService.isInRegion(pos, area.vertices())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPathThroughRestrictedArea(Position from, Position to, List<RestrictedArea> restrictedAreas) {
        for (RestrictedArea area : restrictedAreas) {
            if (area.vertices() != null && !area.vertices().isEmpty()) {
                List<Position> vertices = area.vertices();

                for (int i = 0; i < vertices.size(); i++) {
                    Position v1 = vertices.get(i);
                    Position v2 = vertices.get((i + 1) % vertices.size());

                    if (lineSegmentsIntersect(from, to, v1, v2)) {
                        return true;
                    }
                }

                Position midPoint = new Position(
                        (from.lng() + to.lng()) / 2.0,
                        (from.lat() + to.lat()) / 2.0
                );
                if (ilpService.isInRegion(midPoint, vertices)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean lineSegmentsIntersect(Position p1, Position p2, Position p3, Position p4) {
        double d1 = direction(p3, p4, p1);
        double d2 = direction(p3, p4, p2);
        double d3 = direction(p1, p2, p3);
        double d4 = direction(p1, p2, p4);

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
                ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }

        if (Math.abs(d1) < 1e-10 && onSegment(p3, p1, p4)) return true;
        if (Math.abs(d2) < 1e-10 && onSegment(p3, p2, p4)) return true;
        if (Math.abs(d3) < 1e-10 && onSegment(p1, p3, p2)) return true;
        if (Math.abs(d4) < 1e-10 && onSegment(p1, p4, p2)) return true;

        return false;
    }

    private double direction(Position p1, Position p2, Position p3) {
        return (p3.lng() - p1.lng()) * (p2.lat() - p1.lat()) -
                (p2.lng() - p1.lng()) * (p3.lat() - p1.lat());
    }

    private boolean onSegment(Position p, Position q, Position r) {
        return q.lng() <= Math.max(p.lng(), r.lng()) &&
                q.lng() >= Math.min(p.lng(), r.lng()) &&
                q.lat() <= Math.max(p.lat(), r.lat()) &&
                q.lat() >= Math.min(p.lat(), r.lat());
    }

    private Position getDeliveryPosition(MedDispatchRec dispatch) {
        return dispatch.delivery();
    }
}