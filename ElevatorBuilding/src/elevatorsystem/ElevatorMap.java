package elevatorsystem;

import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ElevatorMap {

    private final Map<Integer, Set<ElevatorRequest>> map = new HashMap<>();

    private int numberOfRequests = 0;

    public void putRequest(ElevatorRequest request) {
        int floor = request.getTo();
        if (!map.containsKey(floor)) {
            map.put(floor, new HashSet<>());
        }
        map.get(floor).add(request);
        numberOfRequests ++;
    }

    public boolean isEmpty() {
        return numberOfRequests == 0;
    }

    public boolean completeRequests(int floor) {
        Set<ElevatorRequest> set = map.getOrDefault(floor, new HashSet<>());
        boolean containsRequests = set.size() != 0;
        set.forEach(request -> {
            request.complete();
            numberOfRequests --;
        });
        map.remove(floor);
        return containsRequests;
    }
}