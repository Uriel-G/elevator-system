package elevatorsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Main {
    private static final Logger LOGGER = Logger.getLogger( Main.class.getName() );
    public static void main(String[] args) {
        LoggerSetup.setUpLogger(LOGGER, Level.WARNING);
        System.out.println("Hello World!");
        test1(1, 10, 100, Arrays.asList("E1", "E2", "E3"), 100);
        //test2(1, 10, Arrays.asList("E1"));

    }
    public static void test2(int minFloor, int maxFloor, List<String> elevators) {
        Building building = new Building("building1", elevators, minFloor, maxFloor);
        building.moveFloors(building.makeRequest(3, 2));
        building.moveFloors(building.makeRequest(3, 1));
    }

    public static void test1(int minFloor, int maxFloor, int numberOfPeople, List<String> elevators, long waitBetweenEachRequest) {
        Building building = new Building("building1", elevators, minFloor, maxFloor);
        ArrayList<Thread> threads = new ArrayList<>();
        for(int i = 0; i < numberOfPeople; i++) {
            threads.add(new Thread(() -> {
                int floor1 = (int) (Math.random() * maxFloor) + (minFloor);
                int floor2 = (int) (Math.random() * maxFloor) + (minFloor);
                while (floor1 == floor2) {
                    floor2 = (int) (Math.random() * maxFloor);
                }
                ElevatorRequest req = building.makeRequest(floor1, floor2);
                building.moveFloors(req);
                while (!req.isComplete()) {
                    try {
                        Thread.sleep(3000); // wait for person to reach their destination floor
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }));
        }
        threads.forEach(thread -> {
            try {
                Thread.sleep(waitBetweenEachRequest); // add a delay between requests
                thread.start();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        // wait for each person to go to their floor. if building elevators work, all should terminate
        threads.forEach(thread -> { try { thread.join();}
        catch (InterruptedException ex) { System.out.println("interrupted-thread"); } });
        System.out.println("DONE"); // all people reached their destinations
    }

}
