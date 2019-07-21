package elevatorsystem;

import java.util.logging.Logger;
import java.util.logging.Level;

class Elevator {
    private static final Logger LOGGER = Logger.getLogger(Elevator.class.getName());
    private static final int milliseconds_between_floors = 3000;
    private static final int milliseconds_opening_closing_doors = 5000;

    private final String ID;
    private final Building building;

    private int floor;
    private ElevatorStatus status = ElevatorStatus.STATIONARY;

    private ElevatorBlockingQueueV2 queue;

    Elevator(String ID, int starting_floor, Building building, boolean dummy) {
        this.ID = ID;
        this.building = building;
        this.floor = starting_floor;
        if (!dummy) {
            this.queue = new ElevatorBlockingQueueV2(this, milliseconds_opening_closing_doors);
            LoggerSetup.setUpLogger(LOGGER, Level.INFO);
            new Thread(() -> {
                try {
                    handleQueue();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private void handleQueue() throws InterruptedException {
        while (true) {
            LOGGER.info(String.format("Elevator %s at floor %d with status %s", ID, floor, status));
            switch (status) {
                case STATIONARY:
                    changeStatus(queue.process());
                    break;
                case DOWN:
                case DOWN_BUSY:
                case UP:
                case UP_BUSY:
                    int floormove = (status == ElevatorStatus.UP || status == ElevatorStatus.UP_BUSY) ? 1 : -1;
                    Thread.sleep(milliseconds_between_floors);
                    changeFloors(floor + floormove);
                    break;
                default:
                    throw new RuntimeException("Unsupported status: " + status);
            }
        }
    }

    void move(ElevatorRequest request) {
        ElevatorStatus newStatus = queue.put(request);
        changeStatus(newStatus);
    }

    int getCurrentFloor() {
        return floor;
    }

    private void changeStatus(ElevatorStatus status) {
        synchronized (building) {
            if (this.status != status) {
                this.status = status;
                building.updateElevator(this);
                LOGGER.info(String.format("Elevator %s now has status %s", ID, status));
            }
        }
    }

    private void changeFloors(int floor) throws InterruptedException {
        building.removeElevatorFromService(this);
        ElevatorStatus status = queue.reachFloor(floor);
        synchronized(building) {
            this.floor = floor;
            if (floor > building.maxFloor() || floor < building.minFloor()) {
                throw new RuntimeException("Out of bounds floor: " + floor);
            }
            if (this.status != status) {
                LOGGER.info(String.format("Elevator %s now has status %s", ID, status));
                this.status = status;
            }
            building.updateElevator(this);
        }
    }


    ElevatorStatus getCurrentStatus() {
        return status;
    }

    String getID() {
        return ID;
    }






}