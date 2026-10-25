import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Q6: no Car/Bike/Truck subclasses. They only returned a constant, so the
// type is a field. The enum carries the per-type data (rate in paise/hour).
enum VehicleType {
    BIKE(500, SpotSize.SMALL),
    CAR(1000, SpotSize.MEDIUM),
    TRUCK(2000, SpotSize.LARGE);

    private final long ratePerHour; // paise, never double for money
    private final SpotSize minSize;

    VehicleType(long ratePerHour, SpotSize minSize) {
        this.ratePerHour = ratePerHour;
        this.minSize = minSize;
    }

    long getRatePerHour() {
        return ratePerHour;
    }

    // sizes are declared smallest first, so "this size or bigger" is an ordinal compare
    boolean fits(SpotSize size) {
        return size.compareTo(minSize) >= 0;
    }
}

enum FuelType {
    PETROL,
    DIESEL,
    ELECTRIC
}

enum SpotSize {
    SMALL,
    MEDIUM,
    LARGE
}

class Spot {
    private final int floor;
    private final int number;
    private final SpotSize size;
    private boolean free = true;

    Spot(int floor, int number, SpotSize size) {
        this.floor = floor;
        this.number = number;
        this.size = size;
    }

    SpotSize getSize() { return size; }
    boolean isFree() { return free; }
    void setFree(boolean free) { this.free = free; }

    public String toString() {
        return "F" + floor + "-" + number + " " + size;
    }
}

class Vehicle {
    private final String plate;
    private final VehicleType type;
    private final FuelType fuel;

    Vehicle(String plate, VehicleType type, FuelType fuel) {
        this.plate = plate;
        this.type = type;
        this.fuel = fuel;
    }

    String getPlate() { return plate; }
    VehicleType getType() { return type; }
    FuelType getFuel() { return fuel; }
}

// Q5: one object carries the whole parking event. Rules read what they need,
// so a new condition never changes the Rule signature.
class ParkingSession {
    private final Vehicle vehicle;
    private final LocalDateTime entry;
    private final LocalDateTime exit;

    ParkingSession(Vehicle vehicle, LocalDateTime entry, LocalDateTime exit) {
        if (!exit.isAfter(entry)) {
            throw new IllegalArgumentException("exit must be after entry");
        }
        this.vehicle = vehicle;
        this.entry = entry;
        this.exit = exit;
    }

    Vehicle getVehicle() { return vehicle; }
    LocalDateTime getEntry() { return entry; }
    LocalDateTime getExit() { return exit; }

    // any started hour is billed as a full hour
    long billableHours() {
        long minutes = Duration.between(entry, exit).toMinutes();
        return (minutes + 59) / 60;
    }
}

class Ticket {
    private final int id;
    private final Vehicle vehicle;
    private final Spot spot;
    private final LocalDateTime entry;

    Ticket(int id, Vehicle vehicle, Spot spot, LocalDateTime entry) {
        this.id = id;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entry = entry;
    }

    int getId() { return id; }
    String getPlate() { return vehicle.getPlate(); }
    Vehicle getVehicle() { return vehicle; }
    Spot getSpot() { return spot; }
    LocalDateTime getEntry() { return entry; }
}

interface Rule {
    long apply(long fee, ParkingSession session);
}

class WeekendRule implements Rule {
    public long apply(long fee, ParkingSession session) {
        DayOfWeek day = session.getEntry().getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return fee * 150 / 100;
        }
        return fee;
    }
}

class ElectricRule implements Rule {
    public long apply(long fee, ParkingSession session) {
        if (session.getVehicle().getFuel() == FuelType.ELECTRIC) {
            return fee * 80 / 100;
        }
        return fee;
    }
}

// The new requirement: one new class. Nothing else above had to change.
// Simplification: half price when the session STARTS between 22:00 and 06:00.
class NightRule implements Rule {
    public long apply(long fee, ParkingSession session) {
        int hour = session.getEntry().getHour();
        if (hour >= 22 || hour < 6) {
            return fee * 50 / 100;
        }
        return fee;
    }
}

class FeeCalculator {
    private final List<Rule> rules;

    // order is explicit and owned by the caller, not hidden in a static array
    FeeCalculator(List<Rule> rules) {
        this.rules = rules;
    }

    long calculate(ParkingSession session) {
        long fee = session.getVehicle().getType().getRatePerHour() * session.billableHours();
        for (Rule rule : rules) {
            fee = rule.apply(fee, session);
        }
        return fee;
    }
}

public class ParkingLot {
    private final List<Spot> spots = new ArrayList<>(); // floor by floor, so first match = first fitting spot
    private final Map<Integer, Ticket> open = new HashMap<>();
    private final FeeCalculator calculator;
    private int nextId = 1;

    ParkingLot(List<List<SpotSize>> floors, FeeCalculator calculator) {
        for (int f = 0; f < floors.size(); f++) {
            for (int n = 0; n < floors.get(f).size(); n++) {
                spots.add(new Spot(f, n, floors.get(f).get(n)));
            }
        }
        this.calculator = calculator;
    }

    // ponytail: linear scan over all spots, keep a free-list per size if lots get huge
    Ticket park(Vehicle vehicle, LocalDateTime entry) {
        for (Spot spot : spots) {
            if (spot.isFree() && vehicle.getType().fits(spot.getSize())) {
                spot.setFree(false);
                Ticket ticket = new Ticket(nextId++, vehicle, spot, entry);
                open.put(ticket.getId(), ticket);
                return ticket;
            }
        }
        throw new IllegalStateException("no free spot for " + vehicle.getPlate());
    }

    long release(int ticketId, LocalDateTime exit) {
        Ticket ticket = open.get(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("unknown or already used ticket " + ticketId);
        }
        // fee first: a bad exit time throws before the ticket is consumed
        long fee = calculator.calculate(new ParkingSession(ticket.getVehicle(), ticket.getEntry(), exit));
        open.remove(ticketId);
        ticket.getSpot().setFree(true);
        return fee;
    }

    static String rupees(long paise) {
        return String.format("Rs %d.%02d", paise / 100, paise % 100);
    }

    static void check(String label, long actual, long expected) {
        String status = actual == expected ? "PASS" : "FAIL";
        System.out.println(status + "  " + label + ": " + rupees(actual) + " (expected " + rupees(expected) + ")");
    }

    static void check(String label, Object actual, Object expected) {
        String status = actual.equals(expected) ? "PASS" : "FAIL";
        System.out.println(status + "  " + label + ": " + actual + " (expected " + expected + ")");
    }

    static void fails(String label, Runnable action) {
        try {
            action.run();
            System.out.println("FAIL  " + label + ": no exception");
        } catch (RuntimeException e) {
            System.out.println("PASS  " + label + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        FeeCalculator calculator = new FeeCalculator(List.of(
                new WeekendRule(),
                new ElectricRule(),
                new NightRule() // the only line added for the new rule
        ));

        Vehicle petrolcar = new Vehicle("MH12AB1234", VehicleType.CAR, FuelType.PETROL);
        Vehicle electriccar = new Vehicle("MH12EV0001", VehicleType.CAR, FuelType.ELECTRIC);

        // Sat 24 Oct 2026, 14:00 to 17:00 — weekend, daytime
        LocalDateTime satstart = LocalDateTime.of(2026, 10, 24, 14, 0);
        LocalDateTime satend = satstart.plusHours(3);

        // Mon 26 Oct 2026, 23:00 to 02:00 — weekday, night
        LocalDateTime nightstart = LocalDateTime.of(2026, 10, 26, 23, 0);
        LocalDateTime nightend = nightstart.plusHours(3);

        // 3h x ₹10 = ₹30 -> weekend x1.5 = ₹45
        check("petrol car, Sat day", calculator.calculate(new ParkingSession(petrolcar, satstart, satend)), 4500);
        // ₹45 -> electric x0.8 = ₹36
        check("electric car, Sat day", calculator.calculate(new ParkingSession(electriccar, satstart, satend)), 3600);
        // ₹30 -> night x0.5 = ₹15
        check("petrol car, Mon night", calculator.calculate(new ParkingSession(petrolcar, nightstart, nightend)), 1500);
        // ₹30 -> electric x0.8 = ₹24 -> night x0.5 = ₹12
        check("electric car, Mon night", calculator.calculate(new ParkingSession(electriccar, nightstart, nightend)), 1200);
        // 1h 01m bills as 2h -> ₹20
        check("petrol car, 61 min", calculator.calculate(new ParkingSession(petrolcar,
                LocalDateTime.of(2026, 10, 26, 10, 0), LocalDateTime.of(2026, 10, 26, 11, 1))), 2000);

        // lot: floor 0 = SMALL, MEDIUM; floor 1 = LARGE
        ParkingLot lot = new ParkingLot(List.of(
                List.of(SpotSize.SMALL, SpotSize.MEDIUM),
                List.of(SpotSize.LARGE)), calculator);
        Vehicle bike = new Vehicle("MH12BK0001", VehicleType.BIKE, FuelType.PETROL);
        Vehicle truck = new Vehicle("MH12TR0001", VehicleType.TRUCK, FuelType.DIESEL);
        LocalDateTime mon10 = LocalDateTime.of(2026, 10, 26, 10, 0);

        check("bike takes first spot", lot.park(bike, mon10).getSpot().toString(), "F0-0 SMALL");
        Ticket car1 = lot.park(petrolcar, mon10);
        check("car skips SMALL", car1.getSpot().toString(), "F0-1 MEDIUM");
        Ticket car2 = lot.park(electriccar, mon10);
        check("car takes LARGE when MEDIUM full", car2.getSpot().toString(), "F1-0 LARGE");
        fails("truck with no LARGE free", () -> lot.park(truck, mon10));

        // Mon 10:00-12:30 -> 3h x ₹10 = ₹30, no rules
        check("release car1", lot.release(car1.getId(), mon10.plusMinutes(150)), 3000);
        fails("release same ticket twice", () -> lot.release(car1.getId(), mon10.plusHours(3)));
        fails("release unknown ticket", () -> lot.release(999, mon10.plusHours(3)));
        fails("exit before entry keeps ticket", () -> lot.release(car2.getId(), mon10.minusHours(1)));
        check("release car2 after bad exit", lot.release(car2.getId(), mon10.plusHours(1)), 800);
        check("truck gets freed LARGE", lot.park(truck, mon10).getSpot().toString(), "F1-0 LARGE");
    }
}
