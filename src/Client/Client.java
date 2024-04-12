package Client;

import Implementation.WebInterface;
import FrontEnd.IFEServices;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.lang.invoke.SwitchPoint;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class Client {

    static Service montrealSer, quebecSer, sherbrookeSer;

    public static final int USER_TYPE_PATIENT = 1;
    public static final int USER_TYPE_ADMIN = 2;
    public static final int PATIENT_BOOK_APPOINTMENT = 1;
    public static final int PATIENT_GET_APPOINTMENT_SCHEDULE = 2;
    public static final int PATIENT_CANCEL_APPOINTMENT = 3;
    public static final int PATIENT_SWAP_APPOINTMENT = 4;
    public static final int PATIENT_LOGOUT = 5;
    public static final int ADMIN_ADD_APPOINTMENT = 1;
    public static final int ADMIN_REMOVE_APPOINTMENT = 2;
    public static final int ADMIN_LIST_APPOINTMENT_AVAILABILITY = 3;
    public static final int ADMIN_BOOK_APPOINTMENT = 4;
    public static final int ADMIN_GET_BOOKING_SCHEDULE = 5;
    public static final int ADMIN_CANCEL_APPOINTMENT = 6;
    public static final int ADMIN_SWAP_APPOINTMENT = 7;
    public static final int ADMIN_LOGOUT = 8;
    public static final int SHUTDOWN = 0;
    public static final String DENTAL = "Dental";
    public static final String SURGEON = "Surgeon";
    public static final String PHYSICIAN = "Physician";

    static Scanner userInput = new Scanner(System.in);

    //TODO: add concurrency to test
    //TODO: find a way to include replica number in namespace

    public static void main(String[] args) {

        try {
            System.out.println("Please enter:\n1- Run the test\n2- Enter your user ID ");
            String input = userInput.nextLine().toLowerCase();

            if (input.equals("1"))
                runTest();
            else if (input.equals("2"))
                Run();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runTest() throws Exception {

        URL montrealURL = new URL("http://localhost:6231/montreal?wsdl");
        QName montrealQName = new QName("http://Service/", "ServerImplementationService");
        montrealSer = Service.create(montrealURL, montrealQName);
        IFEServices ifeServices = montrealSer.getPort(IFEServices.class);

        Runnable addApp1 = () -> {
            String response = ifeServices.addAppointment("MTLA404040", "MTLA808080", "Dental", 5);
            System.out.println("addAppointment(\"MTLA909090\", \"Dental\", 2) -->" + response);
        };

        Runnable bookApp1 = () -> {
            String response = ifeServices.bookAppointment("MTLP123456", "MTLA808080", "Dental");
            System.out.println("bookAppointment(\"MTLP123456\", \"MTLA808080\", \"Dental\") -->" + response);
        };
        Runnable bookApp2 = () -> {
            String response = ifeServices.bookAppointment("MTLP567890", "MTLA808080", "Dental");
            System.out.println("bookAppointment(\"MTLP567890\", \"MTLA808080\", \"Dental\") -->" + response);
        };
        Runnable bookApp3 = () -> {
            String response = ifeServices.bookAppointment("MTLP654321", "MTLA808080", "Dental");
            System.out.println("bookAppointment(\"MTLP654321\", \"MTLA808080\", \"Dental\") -->" + response);
        };
        Runnable bookApp4 = () -> {
            String response = ifeServices.bookAppointment("MTLP876543", "MTLA808080", "Dental");
            System.out.println("bookAppointment(\"MTLP876543\", \"MTLA808080\", \"Dental\") -->" + response);
        };



        Thread thread1 = new Thread(addApp1);
        thread1.start();
        Thread.sleep(500);
        System.out.println("------------------------------------------------------------");

        Thread thread3 = new Thread(bookApp1);
        Thread thread4 = new Thread(bookApp2);
        Thread thread5 = new Thread(bookApp3);
        Thread thread6 = new Thread(bookApp4);

        thread3.start();
        thread4.start();
        thread5.start();
        thread6.start();
        Thread.sleep(500);


    }

    private static void Run() throws MalformedURLException {

        System.out.println("Please enter your user ID:");
        Scanner userInput = new Scanner(System.in);
        String input = userInput.nextLine().toLowerCase();
        String replicaNum = "";
        IFEServices feServices;

        if (input.length() != 10) {
            System.out.println("Please enter a valid ID");
            Run();
        } else{
            switch(input.substring(0, 3).toUpperCase().trim()){
                case "MTL":
                    //.SecondReplica
                    URL montrealURL = new URL("http://localhost:6231/montreal?wsdl");
                    QName montrealQName = new QName("http://Service/", "ServerImplementationService");
                    montrealSer = Service.create(montrealURL, montrealQName);
                    feServices = montrealSer.getPort(IFEServices.class);
                    break;
                case "QUE":
                    URL quebecURL = new URL("http://localhost:6230/quebec?wsdl");
                    QName quebecQName = new QName("http://Service", "ServerImplementationService");
                    quebecSer = Service.create(quebecURL, quebecQName);
                    feServices = quebecSer.getPort(IFEServices.class);
                    break;
                case "SHE":
                    URL sherbrookeURL = new URL("http://localhost:6229/sherbrooke?wsdl");
                    QName sherbrookeQName = new QName("http://Service", "ServerImplementationService");
                    sherbrookeSer = Service.create(sherbrookeURL, sherbrookeQName);
                    feServices = sherbrookeSer.getPort(IFEServices.class);
                    break;
            }

            checkUserType(userInput.nextLine().toLowerCase());

            if (USER_TYPE_PATIENT == 1) {
                try {
                    patient(input);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (USER_TYPE_ADMIN == 2) {
                try {
                    admin(input);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Your user access is not correct!");
                Run();
            }
        }
    }

    private static int checkUserType(String userID) {
        if (userID.length() == 8) {
            if (userID.substring(0, 3).equalsIgnoreCase("MTL") ||
                    userID.substring(0, 3).equalsIgnoreCase("QUE") ||
                    userID.substring(0, 3).equalsIgnoreCase("SHE")) {
                if (userID.substring(3, 4).equalsIgnoreCase("P")) {
                    return USER_TYPE_PATIENT;
                } else if (userID.substring(3, 4).equalsIgnoreCase("A")) {
                    return USER_TYPE_ADMIN;
                }
            }
        }
        return 0;
    }



    private static void patient(String patientID) throws Exception {
        String branchCode = serverBranch(patientID);
        if (branchCode.equals("Null")) {
            System.out.println("Invalid branch! Please re-enter a correct branch code.");
            return;
        }

        String city = "";
        int portNumb = -1;

        switch (branchCode) {
            case "MTL":
                city = "montreal";
                portNumb = 6231;
                break;
            case "QUE":
                city = "quebec";
                portNumb = 6230;
                break;
            case "SHE":
                city = "sherbrooke";
                portNumb = 6229;
                break;
            default:
                break;
        }
        Service service;
        IFEServices feServices;
        URL serviceURL = new URL("http://localhost:" + portNumb + "/" + city + "?wsdl");
        QName serviceQName = new QName("http://Service/", "ServerImplementationService");
        service = Service.create(serviceURL, serviceQName);
        feServices = service.getPort(IFEServices.class);

        boolean repeat = true;
        printMenu(USER_TYPE_PATIENT);
        int menuSelection = userInput.nextInt();
        String appointmentType;
        String appointmentID;
        String serverResponse;

        switch (menuSelection) {
                case PATIENT_BOOK_APPOINTMENT:
                    appointmentType = promptForAppointmentType();
                    appointmentID = promptForAppointmentID();
//                    ClientLogger.clientLog(patientID, " attempting to bookAppointment");
                    serverResponse = feServices.bookAppointment(patientID, appointmentID, appointmentType);
                    System.out.println(serverResponse);
//                    ClientLogger.clientLog(patientID, " bookAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                    break;
                case PATIENT_GET_APPOINTMENT_SCHEDULE:
//                    ClientLogger.clientLog(patientID, " attempting to getBookingSchedule");
                    serverResponse = feServices.getAppointmentSchedule(patientID);
                    System.out.println(serverResponse);
//                    ClientLogger.clientLog(patientID, " bookAppointment", " null ", serverResponse);
                    break;
                case PATIENT_CANCEL_APPOINTMENT:
                    appointmentType = promptForAppointmentType();
                    appointmentID = promptForAppointmentID();
//                    ClientLogger.clientLog(patientID, " attempting to cancelAppointment");
                    serverResponse = feServices.cancelAppointment(patientID, appointmentID, appointmentType);
                    System.out.println(serverResponse);
//                    ClientLogger.clientLog(patientID, " bookAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                    break;
                case PATIENT_SWAP_APPOINTMENT:
                    System.out.println("Please Enter the OLD appointment to be replaced");
                    appointmentType = promptForAppointmentType();
                    appointmentID = promptForAppointmentID();
                    System.out.println("Please Enter the NEW appointment to be replaced");
                    String newAppointmentType = promptForAppointmentType();
                    String newAppointmentID = promptForAppointmentID();
//                    ClientLogger.clientLog(patientID, " attempting to swapAppointment");
                    serverResponse = feServices.swapAppointment(patientID, newAppointmentID, newAppointmentType, appointmentID, appointmentType);
                    System.out.println(serverResponse);
//                    ClientLogger.clientLog(patientID, " swapAppointment", " oldAppointmentID: " + appointmentID + " oldAppointmentType: " + appointmentType + " newAppointmentID: " + newAppointmentID + " newAppointmentType: " + newAppointmentType + " ", serverResponse);
                    break;
                case SHUTDOWN:
//                    ClientLogger.clientLog(patientID, " attempting ORB shutdown");
                    feServices.shutdown();
//                    ClientLogger.clientLog(patientID, " shutdown");
                    return;
                case PATIENT_LOGOUT:
                    repeat = false;
//                    ClientLogger.clientLog(patientID, " attempting to Logout");
                    Run();
                    break;
            }
            if (repeat) {
                patient(patientID);
            }
    }

    private static void admin(String appointmentAdminID) throws Exception {
        String branchCode = serverBranch(userInput.nextLine().trim());
        String city = "";
        int portNumb = -1;
        if (branchCode == "Null") {
            System.out.println("Invalid branch! Please re-enter a valid user ID");
            return;
        }
        switch (branchCode) {
            case "MTL":
                city = "montreal";
                portNumb = 6231;
                break;
            case "QUE":
                city = "quebec";
                portNumb = 6230;
                break;
            case "SHE":
                city = "sherbrooke";
                portNumb = 6229;
                break;
            default:
                break;
        }
        Service service;
        IFEServices FEService;
        URL serviceURL = new URL("http://localhost:" + portNumb + "/" + city + "?wsdl");
        QName serviceQName = new QName("http://Service/", "ServerImplementationService");
        service = Service.create(serviceURL, serviceQName);
        FEService = service.getPort(IFEServices.class);
        boolean repeat = true;
        printMenu(USER_TYPE_ADMIN);
        String patientID;
        String appointmentType;
        String appointmentID;
        String serverResponse;
        int capacity;
        int menuSelection = userInput.nextInt();
        switch (menuSelection) {
            case ADMIN_ADD_APPOINTMENT:
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
                capacity = promptForCapacity();
//                ClientLogger.clientLog(appointmentAdminID, " attempting to addAppointment");
                serverResponse = FEService.addAppointment(appointmentAdminID, appointmentID, appointmentType, capacity);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " addAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " appointmentCapacity: " + capacity + " ", serverResponse);
                break;
            case ADMIN_REMOVE_APPOINTMENT:
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
//                ClientLogger.clientLog(appointmentAdminID, " attempting to removeAppointment");
                serverResponse = FEService.removeAppointment(appointmentAdminID, appointmentID, appointmentType);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " removeAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_LIST_APPOINTMENT_AVAILABILITY:
                appointmentType = promptForAppointmentType();
//                ClientLogger.clientLog(appointmentAdminID, " attempting to listAppointmentAvailability");
                serverResponse = FEService.listAppointmentAvailability(appointmentAdminID, appointmentType);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " listAppointmentAvailability", " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_BOOK_APPOINTMENT:
                patientID = askForPatientIDFromAdmin(appointmentAdminID.substring(0, 3));
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
//                ClientLogger.clientLog(appointmentAdminID, " attempting to bookAppointment");
                serverResponse = FEService.bookAppointment(patientID, appointmentID, appointmentType);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " bookAppointment", " patientID: " + patientID + " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_GET_BOOKING_SCHEDULE:
                patientID = askForPatientIDFromAdmin(appointmentAdminID.substring(0, 3));
//                ClientLogger.clientLog(appointmentAdminID, " attempting to getBookingSchedule");
                serverResponse = FEService.getAppointmentSchedule(patientID);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " getBookingSchedule", " patientID: " + patientID + " ", serverResponse);
                break;
            case ADMIN_CANCEL_APPOINTMENT:
                patientID = askForPatientIDFromAdmin(appointmentAdminID.substring(0, 3));
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
//                ClientLogger.clientLog(appointmentAdminID, " attempting to cancelAppointment");
                serverResponse = FEService.cancelAppointment(patientID, appointmentID, appointmentType);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " cancelAppointment", " patientID: " + patientID + " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_SWAP_APPOINTMENT:
                patientID = askForPatientIDFromAdmin(appointmentAdminID.substring(0, 3));
                System.out.println("Please Enter the OLD appointment to be swapped");
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
                System.out.println("Please Enter the NEW appointment to be swapped");
                String newAppointmentType = promptForAppointmentType();
                String newAppointmentID = promptForAppointmentID();
//                ClientLogger.clientLog(appointmentAdminID, " attempting to swapAppointment");
                serverResponse = FEService.swapAppointment(patientID, newAppointmentID, newAppointmentType, appointmentID, appointmentType);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " swapAppointment", " patientID: " + patientID + " oldAppointmentID: " + appointmentID + " oldAppointmentType: " + appointmentType + " newAppointmentID: " + newAppointmentID + " newAppointmentType: " + newAppointmentType + " ", serverResponse);
                break;
            case SHUTDOWN:
//                ClientLogger.clientLog(appointmentAdminID, " attempting ORB shutdown");
                FEService.shutdown();
//                ClientLogger.clientLog(appointmentAdminID, " shutdown");
                return;
            case ADMIN_LOGOUT:
                repeat = false;
//                ClientLogger.clientLog(appointmentAdminID, "attempting to Logout");
                Run();
                break;
        }
        if (repeat) {
            admin(appointmentAdminID);
        }
    }

    private static String askForPatientIDFromAdmin(String branchCode) {
        System.out.println("Please enter a patientID(Within " + branchCode + " Server):");
        String userID =userInput.next().trim().toUpperCase();
        if (checkUserType(userID) != USER_TYPE_PATIENT|| !userID.substring(0, 3).equals(branchCode)) {
            return askForPatientIDFromAdmin(branchCode);
        } else {
            return userID;
        }
    }

    private static void printMenu(int userType) {
        System.out.println("Please select one of the following actions:");
        if (userType == USER_TYPE_PATIENT) {
            System.out.println("1. Book Appointment");
            System.out.println("2. Get Appointment Schedule");
            System.out.println("3. Cancel Appointment");
            System.out.println("4. Swap Appointment");
            System.out.println("5. Logout");
            System.out.println("0. ShutDown");
        } else if (userType == USER_TYPE_ADMIN) {
            System.out.println("1. Add Appointment");
            System.out.println("2. Remove Appointment");
            System.out.println("3. List Appointment Availability");
            System.out.println("4. Book Appointment");
            System.out.println("5. Get Appointment Schedule");
            System.out.println("6. Cancel Appointment");
            System.out.println("7. Swap Appointment");
            System.out.println("8. Logout");
            System.out.println("0. ShutDown");
        }
    }



    private static String promptForAppointmentType() {
        System.out.println("Please select your appointment type below:");
        System.out.println("1. Dental");
        System.out.println("2. Surgeon");
        System.out.println("3. Physician");
        switch (userInput.nextInt()) {
            case 1:
                return DENTAL;
            case 2:
                return SURGEON;
            case 3:
                return PHYSICIAN;
        }
        return promptForAppointmentType();
    }

    private static String promptForAppointmentID() {
        System.out.println("Please enter the AppointmentID (e.g MTLA190724)");
        String appointmentID = userInput.next().trim().toUpperCase();
        if (appointmentID.length() == 10) {
            if (appointmentID.substring(0, 3).equalsIgnoreCase("MTL") ||
                    appointmentID.substring(0, 3).equalsIgnoreCase("SHE") ||
                    appointmentID.substring(0, 3).equalsIgnoreCase("QUE")) {
                if (appointmentID.substring(3, 4).equalsIgnoreCase("M") ||
                        appointmentID.substring(3, 4).equalsIgnoreCase("A") ||
                        appointmentID.substring(3, 4).equalsIgnoreCase("E")) {
                    return appointmentID;
                }
            }
        }
        return promptForAppointmentID();
    }

    private static int promptForCapacity() {
        System.out.println("Please enter the booking capacity:");
        int cap = userInput.nextInt();
        if (cap > 0) {
            return cap;
        }
        return promptForCapacity();
    }


    private static String serverBranch(String input) {
        String branch = input.substring(0, 3);
        String branchCode = "Null";

        if (branch.equalsIgnoreCase("que"))
            branchCode = "QUE";
        else if (branch.equalsIgnoreCase("mtl"))
            branchCode = "MTL";
        else if (branch.equalsIgnoreCase("she"))
            branchCode = "SHE";

        return branchCode;
    }
}