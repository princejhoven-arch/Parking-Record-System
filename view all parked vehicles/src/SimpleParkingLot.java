import java.util.*;  // Imports for collections, date handling, and I/O
import java.util.Date;
import java.text.SimpleDateFormat;  // For date and time formatting
import java.io.FileWriter;  // For writing to files
import java.io.IOException;  // For handling file I/O errors

// Represents a parking record with vehicle details and timestamps
class ParkingRecord {
    String date;  // Parking date
    String timeIn;  // Entry time
    String plateNo;  // License plate number
    String vehicle;  // Vehicle type (Car, Motorcycle, Van)
    String slot;  // Parking slot
    String timeOut;  // Exit time or "Still Parked"
    double hours;  // Hours parked
    double fee;  // Parking fee
    Date entryDate;  // Full entry timestamp

    // Initializes a new parking record
    public ParkingRecord(String date, String timeIn, String plateNo, String vehicle, String slot, Date entryDate) {
        this.date = date;
        this.timeIn = timeIn;
        this.plateNo = plateNo;
        this.vehicle = vehicle;
        this.slot = slot;
        this.timeOut = "Still Parked";  // Default for new entries
        this.hours = 0.0;
        this.fee = 0.0;
        this.entryDate = entryDate;
    }

    // Updates record on vehicle exit
    public void updateOnExit(String timeOut, double hours, double fee) {
        this.timeOut = timeOut;
        this.hours = hours;
        this.fee = fee;
    }

    // Formats record as a string for display
    public String toString() {
        return String.format("%-10s | %-8s | %-8s | %-9s | %-12s | %-13s | %-6.2f | $%-5.2f",
                date, timeIn, plateNo, vehicle, slot, timeOut, hours, fee);
    }
}

// Main class for the parking lot system
public class SimpleParkingLot {
    private static HashMap<String, ParkingRecord> currentParked = new HashMap<>();  // Tracks parked vehicles
    private static ArrayList<ParkingRecord> allRecords = new ArrayList<>();  // All parking records
    private static int capacity = 5;  // Max parking spots
    private static double feePerHour = 5.0;  // Unused; rates are per vehicle type
    private static int nextSlot = 1;  // Helper for slot numbering

    // Main entry point; runs the menu loop
    static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);  // For user input
        boolean running = true;  // Controls the loop

        while (running) {
            System.out.println("\n--- Parking Lot Management System ---");
            System.out.println("f. View All Parked Vehicles");
            System.out.println("g. Park a Vehicle");
            System.out.println("h. Remove a Vehicle");
            System.out.println("i. Generate Parking Report");
            System.out.println("j. Exit Application");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine().trim().toLowerCase();  // Read and normalize input

            switch (choice) {
                case "f":  // Show parked vehicles
                    printParkingRecords();
                    break;
                case "g":  // Add a vehicle
                    System.out.print("Enter license plate: ");
                    String plateNo = scanner.nextLine();
                    System.out.print("Enter vehicle type (Car, Motorcycle, Van): ");
                    String vehicleType = scanner.nextLine();
                    System.out.print("Enter parking slot (e.g., P1): ");
                    String parkingSlot = scanner.nextLine();
                    System.out.print("Enter time in (e.g., 08:00 AM): ");
                    String timeIn = scanner.nextLine();
                    addVehicle(plateNo, vehicleType, parkingSlot, timeIn);
                    break;
                case "h":  // Remove a vehicle
                    System.out.print("Enter license plate to remove: ");
                    String removePlate = scanner.nextLine();
                    removeVehicle(removePlate);
                    break;
                case "i":  // Generate and save report
                    saveParkingReportToFile("ParkingReport.txt");
                    break;
                case "j":  // Exit the program
                    running = false;
                    System.out.println("Thank You!");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter f, g, h, i, or j.");
            }
        }
        scanner.close();  // Clean up scanner
    }

    // Adds a vehicle if space available; parses time and creates record
    static boolean addVehicle(String plateNo, String vehicle, String slot, String timeIn) {
        if (currentParked.size() >= capacity) {  // Check capacity
            System.out.println("Parking lot full! Can't add " + plateNo + ".");
            return false;
        }
        if (currentParked.containsKey(plateNo)) {  // Check for duplicates
            System.out.println(plateNo + " is already parked!");
            return false;
        }

        // Parse timeIn and combine with current date
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a");
        Date now = new Date();
        String date = dateFmt.format(now);
        Date entryDate;
        try {
            entryDate = timeFmt.parse(timeIn);  // Parse input time
            Calendar cal = Calendar.getInstance();
            cal.setTime(now);
            Calendar timeCal = Calendar.getInstance();
            timeCal.setTime(entryDate);
            cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            entryDate = cal.getTime();  // Create full timestamp
        } catch (Exception e) {
            System.out.println("Invalid time format for Time In. Use hh:mm a (e.g., 08:00 AM).");
            return false;
        }

        ParkingRecord record = new ParkingRecord(date, timeIn, plateNo, vehicle, slot, entryDate);
        currentParked.put(plateNo, record);
        allRecords.add(record);
        System.out.println(plateNo + " (" + vehicle + ") parked in slot " + slot + ".");
        return true;
    }

    // Removes a vehicle; prompts for exit time, calculates fee
    public static boolean removeVehicle(String plateNo) {
        ParkingRecord record = currentParked.get(plateNo);
        if (record == null) {  // Check if vehicle exists
            System.out.println(plateNo + " not found.");
            return false;
        }

        // Prompt for exit time
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter time out (e.g., 10:00 AM): ");
        String timeOutStr = scanner.nextLine();

        // Parse timeOut and handle overnight if needed
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a");
        Date exitDate;
        try {
            exitDate = timeFmt.parse(timeOutStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(record.entryDate);
            Calendar timeCal = Calendar.getInstance();
            timeCal.setTime(exitDate);
            cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            exitDate = cal.getTime();
            if (exitDate.before(record.entryDate)) {  // Handle overnight
                cal.add(Calendar.DAY_OF_MONTH, 1);
                exitDate = cal.getTime();
            }
        } catch (Exception e) {
            System.out.println("Invalid time format for Time Out. Use hh:mm a (e.g., 10:00 AM).");
            return false;
        }

        // Calculate hours and fee based on vehicle type
        long timeDiffMs = exitDate.getTime() - record.entryDate.getTime();
        double hours = timeDiffMs / (1000.0 * 60 * 60);
        double feeRate = record.vehicle.equals("Car") || record.vehicle.equals("Van") ? 20.0 : 10.0;
        double fee = Math.round(hours * feeRate * 100) / 100.0;
        String timeOut = timeOutStr;
        record.updateOnExit(timeOut, hours, fee);
        currentParked.remove(plateNo);

        // Display details
        System.out.println("\nPlate Number: " + plateNo);
        System.out.println("Time in: " + record.timeIn);
        System.out.println("Time out: " + timeOut);
        System.out.println("Total hours: " + String.format("%.2f", hours));
        System.out.println("Type: " + record.vehicle);
        System.out.println("Fee: " + String.format("%.2f", fee));
        return true;
    }

    // Displays currently parked vehicles in a table
    public static void printParkingRecords() {
        List<ParkingRecord> parkedVehicles = new ArrayList<>();
        for (ParkingRecord record : allRecords) {
            if (record.timeOut.equals("Still Parked")) {  // Filter parked only
                parkedVehicles.add(record);
            }
        }

        if (parkedVehicles.isEmpty()) {  // No vehicles to show
            System.out.println("No parked vehicles.");
            return;
        }

        // Print header and rows
        System.out.println("\n--- Parked Vehicles ---");
        System.out.println(String.format("%-2s   %-10s   %-10s   %-12s   %-12s   %-12s",
                "#", "Date", "Time-in", "Plate Number", "Vehicle Type", "Parking Slot"));
        for (int i = 0; i < parkedVehicles.size(); i++) {
            ParkingRecord record = parkedVehicles.get(i);
            System.out.println(String.format("%-2d   %-10s   %-10s   %-12s   %-12s   %-12s",
                    (i + 1), record.date, record.timeIn, record.plateNo, record.vehicle, record.slot));
        }
    }

    // Generates and saves a full parking report to file and console
    public static void saveParkingReportToFile(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header to file
            writer.write(String.format("%-2s | %-12s | %-12s | %-14s | %-14s | %-14s | %-12s | %-12s%n",
                    "#", "Date", "Time-in", "Time-out", "Plate Number", "Vehicle Type", "Hours Parked", "Fee"));
            writer.write("---------------------------------------------------------------------\n");

            double totalFees = 0.0;  // Track total fees
            for (int i = 0; i < allRecords.size(); i++) {
                ParkingRecord record = allRecords.get(i);
                double hours = record.hours;
                double fee = record.fee;
                totalFees += fee;
                writer.write(String.format("%-2d | %-12s | %-12s | %-14s | %-14s | %-14s | %-12.2f | ₱%-12.2f%n",
                        (i + 1), record.date, record.timeIn, record.timeOut, record.plateNo, record.vehicle, hours, fee));
            }

            // Write summary to file
            writer.write("\nTotal Number of Vehicles: " + allRecords.size() + "\n");
            writer.write("Total Fees Collected: PHP " + String.format("%.2f", totalFees) + "\n");

            // Print to console
            System.out.println("\n--- Parking Report ---");
            System.out.println(String.format("%-2s %-12s %-12s %-14s %-14s %-14s %-12s %-12s",
                    "#", "Date", "Time-in", "Time-out", "Plate Number", "Vehicle Type", "Hours Parked", "Fee"));
            System.out.println("---------------------------------------------------------------------");
            for (int i = 0; i < allRecords.size(); i++) {
                ParkingRecord record = allRecords.get(i);
                double hours = record.hours;
                double fee = record.fee;
                System.out.println(String.format("%-2d %-12s %-12s %-14s %-14s %-14s %-12.2f ₱%-12.2f",
                        (i + 1), record.date, record.timeIn, record.timeOut, record.plateNo, record.vehicle, hours, fee));
            }

            // Print summary to console
            System.out.println("\nTotal Number of Vehicles: " + allRecords.size());
            System.out.println("Total Fees Collected: ₱" + String.format("%.2f", totalFees));
            System.out.println("\nParking report saved to: " + filename);
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }
}
