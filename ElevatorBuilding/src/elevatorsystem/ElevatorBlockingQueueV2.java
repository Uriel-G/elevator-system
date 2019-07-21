package elevatorsystem;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class ElevatorBlockingQueueV2 {

    private static final Logger LOGGER = Logger.getLogger(ElevatorBlockingQueueV2.class.getName());

    private static final Comparator<ElevatorRequest> comparator = new Comparator<ElevatorRequest>() {
        @Override
        public int compare(ElevatorRequest o1, ElevatorRequest o2) {
            if (o1.getFrom() != o2.getFrom()) {
                return Integer.compare(o1.getFrom(), o2.getFrom());
            }
            return Integer.compare(o1.getTo(), o2.getTo());
        }
    };
    private final TreeSet<ElevatorRequest> upRequests = new TreeSet<>(comparator);
    private final TreeSet<ElevatorRequest> downRequests = new TreeSet<>(comparator);

    private final ElevatorMap currentRequests = new ElevatorMap();
    private final long open_close;
    private final Elevator elevator;

    ElevatorBlockingQueueV2(Elevator e, long open_close_milliseconds) {
        this.open_close = open_close_milliseconds;
        elevator = e;
        LoggerSetup.setUpLogger(LOGGER, Level.INFO);
    }


    // updates position and removes floor from queue if it's reached. returns status after floor is reached
    synchronized ElevatorStatus reachFloor(int floor) throws InterruptedException {
        boolean stop = currentRequests.completeRequests(floor);
        ElevatorStatus resultStatus;
        ElevatorRequest dummyCurrentFloor = new ElevatorRequest(floor);
        switch (elevator.getCurrentStatus()) {
            case UP:
                NavigableSet<ElevatorRequest> minHigher = upRequests.tailSet(dummyCurrentFloor, true);
                while (minHigher.size() > 0 && minHigher.first() != null && minHigher.first().getFrom() == floor) {
                    ElevatorRequest req = minHigher.pollFirst();
                    currentRequests.putRequest(req);
                    stop = true;
                }
                resultStatus = !currentRequests.isEmpty() ? ElevatorStatus.UP : ElevatorStatus.STATIONARY;
                break;
            case DOWN:
                NavigableSet<ElevatorRequest> minLower = downRequests.tailSet(dummyCurrentFloor, true);
                while (minLower.size() > 0 && minLower.first() != null && minLower.first().getFrom() == floor) {
                    ElevatorRequest req = minLower.pollFirst();
                    currentRequests.putRequest(req);
                    stop = true;
                }
                resultStatus = !currentRequests.isEmpty() ? ElevatorStatus.DOWN : ElevatorStatus.STATIONARY;
                break;
            case UP_BUSY:
                stop = downRequests.last().getFrom() == floor || stop;
                resultStatus = skipToFurthestUpOrDownRequest(true, floor);
                break;
            case DOWN_BUSY:
                stop = upRequests.first().getFrom() == floor || stop;
                resultStatus = skipToFurthestUpOrDownRequest(false, floor);
                break;
            default:
                throw new RuntimeException("Elevator should not be stationary and moving");
        }
        if (stop) {
            LOGGER.info(String.format("Elevator %s OPENING/CLOSING", elevator.getID()));
            Thread.sleep(open_close);
        }
        return resultStatus;
    }

    private ElevatorStatus skipToFurthestUpOrDownRequest(boolean up, int floor) {
        TreeSet<ElevatorRequest> request = up ? downRequests : upRequests;
        ElevatorRequest furthest = up ? downRequests.last() : upRequests.first();
        if (furthest.getFrom() == floor) {
            while (furthest != null && furthest.getFrom() == floor) {
                ElevatorRequest furthestRemoved = up ? downRequests.pollLast() : upRequests.pollFirst();
                furthest = request.isEmpty() ? null : up ? downRequests.last() : upRequests.first();
                currentRequests.putRequest(furthestRemoved);
            }
            return up ? ElevatorStatus.DOWN : ElevatorStatus.UP;
        }
        return up ? ElevatorStatus.UP_BUSY : ElevatorStatus.DOWN_BUSY;
    }

    synchronized ElevatorStatus put(ElevatorRequest elem) {
        switch(elem.getType()) {
            case UP:
                if (upRequests.contains(elem)) {
                    elem.complete();
                }
                else {
                    upRequests.add(elem);
                }
                break;
            case DOWN:
                if (downRequests.contains(elem)) {
                    elem.complete();
                }
                else {
                    downRequests.add(elem);
                }
                break;
            default:
                throw new RuntimeException("Request cannot be stationary");
        }
        notifyAll();
        return elevator.getCurrentStatus();
    }

    // returns status of elevator to go to requested floor. should call this when elevator is STATIONARY
    synchronized ElevatorStatus process() throws InterruptedException {
        while (upRequests.size() == 0 && downRequests.size() == 0) {
            wait();
        }
        int floor = elevator.getCurrentFloor();
        if (upRequests.size() != 0) {
            return getFurthestRequest(true, floor);
        }
        else if (downRequests.size() != 0) {
            return getFurthestRequest(false, floor);
        }
        throw new RuntimeException("Blocking queue failed to block");
    }

    private ElevatorStatus getFurthestRequest(boolean up, int floor) throws InterruptedException {
        TreeSet<ElevatorRequest> requests = up ? upRequests : downRequests;
        ElevatorRequest furthest = up ? requests.first() : requests.last();
        if (furthest.getFrom() == floor) { // request is coming from floor that elevator is currently in
            while (furthest != null && furthest.getFrom() == floor) {
                ElevatorRequest furthestRemoved = up ? requests.pollFirst() : requests.pollLast();
                furthest = requests.isEmpty() ? null : up ? requests.first() : requests.last();
                currentRequests.putRequest(furthestRemoved);
            }
            LOGGER.info(String.format("Elevator %s OPENING/CLOSING", elevator.getID()));

            wait(open_close);
            currentRequests.completeRequests(floor);
            return up ? ElevatorStatus.UP : ElevatorStatus.DOWN;
        }
        if (up) {
            return furthest.getFrom() > floor ? ElevatorStatus.UP : ElevatorStatus.DOWN_BUSY;
        }
        else {
            return furthest.getFrom() > floor ? ElevatorStatus.UP_BUSY : ElevatorStatus.DOWN;
        }
    }
}
