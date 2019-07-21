package elevatorsystem;

import java.util.*;
import java.util.logging.*;

public class Building {
    private static final Logger LOGGER = Logger.getLogger(Building.class.getName());
    private final Map<ElevatorStatus, TreeSet<Elevator>> statusToElevators = new HashMap<>();
    private final int minFloor;
    private final int maxFloor;
    private static final Comparator<Elevator> comparator = new Comparator<Elevator>() {
        @Override
        public int compare(Elevator o1, Elevator o2) {
            if (o1.getCurrentFloor() != o2.getCurrentFloor()) {
                return Integer.compare(o1.getCurrentFloor(), o2.getCurrentFloor());
            }
            return o1.getID().compareTo(o2.getID());
        }
    };
    private final String buildingID;

    public Building(String building_ID, List<String> elevators, int min_floor, int max_floor) {
        buildingID = building_ID;
        minFloor = min_floor;
        maxFloor = max_floor;
        this.statusToElevators.put(ElevatorStatus.UP, new TreeSet<>(comparator));
        this.statusToElevators.put(ElevatorStatus.DOWN, new TreeSet<>(comparator));
        this.statusToElevators.put(ElevatorStatus.STATIONARY, new TreeSet<>(comparator));
        elevators.forEach(ID -> {
                Elevator e = new Elevator(ID, min_floor, this, false);
                this.statusToElevators.get(ElevatorStatus.STATIONARY).add(e);
        });
        LoggerSetup.setUpLogger(LOGGER, Level.INFO);
    }



    synchronized void updateElevator(Elevator elevator) {
        // add elevator to appropriate key
        ElevatorStatus status = elevator.getCurrentStatus();
        for (ElevatorStatus s : statusToElevators.keySet()) {
            if (status.equals(s)) {
                boolean notInSet = !statusToElevators.get(s).remove(elevator);
                if (notInSet) {
                    notifyAll(); // if there is a valid status change, notify requests
                }
                statusToElevators.get(s).add(elevator);
            }
            else {
                statusToElevators.get(s).remove(elevator);
            }
        }
    }

    synchronized void removeElevatorFromService(Elevator elevator) {
        statusToElevators.forEach( (e,elevators) -> elevators.remove(elevator));
    }

    public synchronized void moveFloors(ElevatorRequest request) {
        Elevator target = new Elevator("", request.getFrom(), this, true);
        Elevator stationary = getClosestStationaryElevator(target);
        Elevator moving = getClosestMovingElevator(target, request.getType());

        Elevator best = getClosestElevator(stationary, moving, target);
        try {
            if (best != null) {
                LOGGER.info(String.format("Moving floors from %d to %d. Best elevator is %s",
                        request.getFrom(), request.getTo(), best.getID()));
                request.process();
                best.move(request);
            } else {
                wait(); // wait until elevator becomes available, then try again
                moveFloors(request);
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ElevatorRequest makeRequest(int from, int to) {
        return new ElevatorRequest(from, to, this);
    }

    private Elevator getClosestMovingElevator(Elevator target, ElevatorStatus type) {
        Elevator moving;
        if (type == ElevatorStatus.UP) { //only elevators going up can take it
            moving = statusToElevators.get(ElevatorStatus.UP).headSet(target, false).floor(target);
        }
        else { // only elevators going down can take it
            moving = statusToElevators.get(ElevatorStatus.DOWN).tailSet(target, false).ceiling(target);
        }
        return moving;
    }

    private Elevator getClosestStationaryElevator(Elevator target) {
        Elevator higher = statusToElevators.get(ElevatorStatus.STATIONARY).ceiling(target);
        Elevator lower = statusToElevators.get(ElevatorStatus.STATIONARY).lower(target);
        return getClosestElevator(higher, lower ,target);
    }

    private Elevator getClosestElevator(Elevator e1, Elevator e2, Elevator target) {
        if (e1 != null && e2 != null) {
            return Math.abs(e1.getCurrentFloor() - target.getCurrentFloor()) >
                    Math.abs(e2.getCurrentFloor() - target.getCurrentFloor())? e2 : e1;
        }
        return e1 != null ? e1 : e2;
    }

    public String getID() {
        return buildingID;
    }

    public int minFloor() {
        return minFloor;
    }

    public int maxFloor() {
        return maxFloor;
    }

}