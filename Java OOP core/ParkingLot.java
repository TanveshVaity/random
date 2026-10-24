enum VehicleType {
    BIKE(5),
    CAR(10),
    TRUCK(20);

    int rate;

    VehicleType(int rate) {
        this.rate = rate;
    }
}

enum FuelType {
    PETROL,
    ELECTRIC
}


abstract class Vehicle {
    String plate;
    FuelType fuel;

    Vehicle(String plate, FuelType fuel) {
        this.plate = plate;
        this.fuel = fuel;
    }

    abstract VehicleType getType();
}

class Bike extends Vehicle {
    Bike(String plate, FuelType fuel) { super(plate, fuel); }
    VehicleType getType() { return VehicleType.BIKE; }
}

class Car extends Vehicle {
    Car(String plate, FuelType fuel) { super(plate, fuel); }
    VehicleType getType() { return VehicleType.CAR; }
}

class Truck extends Vehicle {
    Truck(String plate, FuelType fuel) { super(plate, fuel); }
    VehicleType getType() { return VehicleType.TRUCK; }
}

interface Rule {
    double apply(double fee, Vehicle vehicle, boolean weekend);
}

class WeekendRule implements Rule {
    public double apply(double fee, Vehicle vehicle, boolean weekend) {
        if (weekend) {
            return fee * 1.5;
        }
        return fee;
    }
}

class ElectricRule implements Rule {
    public double apply(double fee, Vehicle vehicle, boolean weekend) {
        if (vehicle.fuel == FuelType.ELECTRIC) {
            return fee * 0.8;
        }
        return fee;
    }
}

public class ParkingLot {
    static Rule[] rules = { new WeekendRule(), new ElectricRule() };

    static double calculateFee(Vehicle vehicle, int hours, boolean weekend) {
        double fee = vehicle.getType().rate * hours;
        for (Rule rule : rules) {
            fee = rule.apply(fee, vehicle, weekend);
        }
        return fee;
    }

    public static void main(String[] args) {
        Vehicle petrolCar = new Car("MH12AB1234", FuelType.PETROL);
        Vehicle electricCar = new Car("MH12EV0001", FuelType.ELECTRIC);

        System.out.println("Petrol car:   " + calculateFee(petrolCar, 3, true) + " (expected 45.0)");
        System.out.println("Electric car: " + calculateFee(electricCar, 3, true) + " (expected 36.0)");
    }
}
