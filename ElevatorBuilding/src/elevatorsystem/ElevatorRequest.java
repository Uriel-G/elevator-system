package elevatorsystem;

public class ElevatorRequest {

    private final int from;
    private final int to;
    private final int min_floor;
    private final int max_floor;
    private final ElevatorStatus type;
    private ElevatorRequestStatus status = ElevatorRequestStatus.WAITING;

    enum ElevatorRequestStatus {
        WAITING, PROCESSED, COMPLETED
    }

    public ElevatorRequest(int fromFloor, int toFloor, Building building) {
        type = fromFloor > toFloor ? ElevatorStatus.DOWN : ElevatorStatus.UP;
        min_floor = building.minFloor();
        max_floor = building.maxFloor();
        to = toFloor < min_floor ? min_floor : toFloor > max_floor ? max_floor : toFloor;
        from = fromFloor < min_floor ? min_floor : fromFloor > max_floor ? max_floor : fromFloor;
        if (to == from) {
            throw new RuntimeException("Invalid Elevator Request: " + fromFloor + " to " + toFloor);
        }
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public ElevatorStatus getType() {
        return type;
    }

    public void process() {
        if (status == ElevatorRequestStatus.WAITING) {
            status = ElevatorRequestStatus.PROCESSED;
        }
    }

    public boolean isProcessed() {
        return status == ElevatorRequestStatus.PROCESSED || status == ElevatorRequestStatus.COMPLETED;
    }

    public void complete() {
        if (status == ElevatorRequestStatus.PROCESSED || status == ElevatorRequestStatus.COMPLETED) {
            status = ElevatorRequestStatus.COMPLETED;
        }
        else {
            throw new RuntimeException("Not processed yet");
        }
    }

    public boolean isComplete() {
        return status == ElevatorRequestStatus.COMPLETED;
    }

    ElevatorRequest(int from) {
        this.from = from;
        this.to = -1;
        min_floor = -1;
        max_floor = -1;
        type = ElevatorStatus.DOWN;

    }



}
