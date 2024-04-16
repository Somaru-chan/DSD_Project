package Client;

import FrontEnd.FrontEnd;
import FrontEnd.IFEServices;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
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
    public static final int ADMIN_GET_APPOINTMENT_SCHEDULE = 5;
    public static final int ADMIN_CANCEL_APPOINTMENT = 6;
    public static final int ADMIN_SWAP_APPOINTMENT = 7;
    public static final int ADMIN_LOGOUT = 8;
    public static final int SHUTDOWN = 0;
    public static final String DENTAL = "Dental";
    public static final String SURGEON = "Surgeon";
    public static final String PHYSICIAN = "Physician";

    static Scanner userInput = new Scanner(System.in);

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

        URL url = new URL("http://" + FrontEnd.FE_IP_Address + ":9004/fe/?wsdl");
        System.out.println(url);
        QName qName = new QName("http://FrontEnd/", "FEServicesImplService");
        Service service = Service.create(url, qName);
        IFEServices ifeServices = service.getPort(IFEServices.class);

        Runnable addAppointment1 = () -> {
            String response = ifeServices.addAppointment("MTLA010101","MTLE101922", "Dental", 3);
            System.out.println("addAppointment(\"MTLE101922\", \"Dental\", 3) -->" + response);
        };
        Runnable addAppointment2 = () -> {
            String response = ifeServices.addAppointment("QUEA010101", "QUEE101921", "Dental", 3);
            System.out.println("addAppointment(\"QUEE101921\", \"Dental\", 3) -->" + response);
        };
        Runnable addAppointment3 = () -> {
            String response = ifeServices.addAppointment("SHEA010101","SHEE101920", "Dental", 3);
            System.out.println("addAppointment(\"SHEE101920\", \"Dental\", 3) -->" + response);
        };
        Runnable addAppointment4 = () -> {
            String response = ifeServices.addAppointment("MTLA010101","MTLE101923", "Surgeon", 3);
            System.out.println("addAppointment(\"MTLE101923\", \"Surgeon\", 3) -->" + response);
        };
        Runnable bookAppointment1 = () -> {
            String response = ifeServices.bookAppointment("MTLP00001", "MTLE101922", "Dental");
            System.out.println("bookAppointment(\"MTLP00001\", \"MTLE101922\", \"Dental\") -->" + response);
        };
        Runnable bookAppointment2 = () -> {
            String response = ifeServices.bookAppointment("MTLP00002", "MTLE101922", "Dental");
            System.out.println("bookAppointment(\"MTLP00002\", \"MTLE101922\", \"Dental\") -->" + response);;
        };
        Runnable bookAppointment3 = () -> {
            String response = ifeServices.bookAppointment("MTLP00003", "MTLE101922", "Dental");
            System.out.println("bookAppointment(\"MTLP00003\", \"MTLE101922\", \"Dental\") -->" + response);
        };
        Runnable bookAppointment4 = () -> {
            String response = ifeServices.bookAppointment("MTLP00004", "MTLE101922", "Dental");
            System.out.println("bookAppointment(\"MTLP00004\", \"MTLE101922\", \"Dental\") -->" + response);
        };
        Runnable bookAppointment5 = () -> {
            String response = ifeServices.bookAppointment("QUEP00001", "QUEE101921", "Dental");
            System.out.println("bookAppointment(\"QUEP00001\", \"QUEE101921\", \"Dental\") -->" + response);
        };
        Runnable bookAppointment6 = () -> {
            String response = ifeServices.bookAppointment("QUEP00001", "QUEE101921", "Dental");
            System.out.println("bookAppointment(\"QUEP00001\", \"QUEE101921\", \"Dental\") -->" + response);
        };
        Runnable bookAppointment7 = () -> {
            String response = ifeServices.bookAppointment("SHEP00001", "SHEE101920", "Dental");
            System.out.println("bookAppointment(\"SHEP00001\", \"SHEE101921\", \"Dental\") -->" + response);
        };
        Runnable getAppointmentSchedule1 = () -> {
            String response = ifeServices.getAppointmentSchedule("MTLP00002");
            System.out.println("getAppointmentSchedule(\"MTLP00002\") -->" + response);
        };
        Runnable listAppointmentAvailability1 = () -> {
            String response = ifeServices.listAppointmentAvailability( "MTLA010101","Dental");
            System.out.println("listAppointmentAvailability( \"Dental\") -->" + response);
        };
        Runnable swapAppointment1 = () -> {
            String response = ifeServices.swapAppointment("MTLP00002","MTLE101922", "Dental", "MTLE101923", "Surgeon");
            System.out.println("swapAppointment(\"MTLP00002\", \"MTLE101922\",\"Dental\", \"MTLE101923\", \"Surgeon\" -->" + response);
        };
        Runnable cancelAppointment1 = () -> {
            String response = ifeServices.cancelAppointment("MTLP00001","MTLE101922","Dental");
            System.out.println("cancelAppointment(\"MTLP00001\", \"MTLE101922\" -->" + response);
        };
        Runnable removeAppointment1 = () -> {
            String response = ifeServices.removeAppointment("MTLA010101","MTLE101923", "Surgeon");
            System.out.println("removeAppointment(\"MTLE101923\",\"Surgeon\" -->" + response);
        };

        Thread thread1 = new Thread(addAppointment1);
        Thread thread2 = new Thread(addAppointment2);
        Thread thread3 = new Thread(addAppointment3);
        Thread thread4 = new Thread(addAppointment4);
        try {
            thread1.start();
            thread1.join();

            thread2.start();
            thread2.join();

            thread3.start();
            thread3.join();

            thread4.start();
            thread4.join();

            Thread.sleep(500);
            System.out.println("------------------------------------------------------------");

            Thread thread5 = new Thread(bookAppointment1);
            Thread thread6 = new Thread(bookAppointment2);
            Thread thread7 = new Thread(bookAppointment3);
            Thread thread8 = new Thread(bookAppointment4);
            Thread thread9 = new Thread(bookAppointment5);
            Thread thread10 = new Thread(bookAppointment6);
            Thread thread11 = new Thread(bookAppointment7);

            thread5.start();
            thread5.join();

            thread6.start();
            thread6.join();

            thread7.start();
            thread7.join();

            thread8.start();
            thread8.join();

            thread9.start();
            thread9.join();

            thread10.start();
            thread10.join();

            thread11.start();
            thread11.join();

            Thread.sleep(500);
            System.out.println("------------------------------------------------------------");


            Thread thread12 = new Thread(getAppointmentSchedule1);
            Thread thread13 = new Thread(listAppointmentAvailability1);
            Thread thread14 = new Thread(swapAppointment1);

            thread12.start();
            thread12.join();

            thread13.start();
            thread13.join();

            thread14.start();
            thread14.join();

            Thread.sleep(500);
            System.out.println("------------------------------------------------------------");

            Thread thread15 = new Thread(cancelAppointment1);
            Thread thread16 = new Thread(removeAppointment1);
            thread15.start();
            thread15.join();

            thread16.start();
            thread16.start();

            Thread.sleep(500);
            System.out.println("------------------------------------------------------------");

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void Run() throws MalformedURLException {

        System.out.println("Please enter your user ID:");
        Scanner userInput = new Scanner(System.in);
        String input = userInput.nextLine().toLowerCase();

        if (input.length() != 10) {
            System.out.println("Please enter a valid ID");
            Run();
        } else {
            URL url = new URL("http://" + FrontEnd.FE_IP_Address + ":9004/fe/?wsdl");
//            System.out.println(url);
            QName qName = new QName("http://FrontEnd/", "FEServicesImplService");
//            System.out.println(qName);
            Service service = Service.create(url, qName);
            IFEServices userService = service.getPort(IFEServices.class);

            int userType = checkUserType(input);

            if (userType == USER_TYPE_PATIENT) {
//                System.out.println(userType);
                try {
                    patient(input);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (userType == USER_TYPE_ADMIN) {
//                System.out.println(userType);
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
        if (userID.length() == 10) {
            if (userID.substring(0, 3).equalsIgnoreCase("MTL") || userID.substring(0, 3).equalsIgnoreCase("QUE") || userID.substring(0, 3).equalsIgnoreCase("SHE")) {
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

        URL url = new URL("http://" + FrontEnd.FE_IP_Address + ":9004/fe/?wsdl");
//        System.out.println(url);
        QName qName = new QName("http://FrontEnd/", "FEServicesImplService");
//        System.out.println(qName);
        Service service = Service.create(url, qName);
        IFEServices userService = service.getPort(IFEServices.class);

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
                serverResponse = userService.bookAppointment(patientID, appointmentID, appointmentType);
                System.out.println(serverResponse);
//                    ClientLogger.clientLog(patientID, " bookAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case PATIENT_GET_APPOINTMENT_SCHEDULE:
//                    ClientLogger.clientLog(patientID, " attempting to getAppointmentSchedule");
                serverResponse = userService.getAppointmentSchedule(patientID);
                System.out.println(serverResponse);
//                    ClientLogger.clientLog(patientID, " bookAppointment", " null ", serverResponse);
                break;
            case PATIENT_CANCEL_APPOINTMENT:
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
//                    ClientLogger.clientLog(patientID, " attempting to cancelAppointment");
                serverResponse = userService.cancelAppointment(patientID, appointmentID, appointmentType);
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
                serverResponse = userService.swapAppointment(patientID, newAppointmentID, newAppointmentType, appointmentID, appointmentType);
                System.out.println(serverResponse);
//                    ClientLogger.clientLog(patientID, " swapAppointment", " oldAppointmentID: " + appointmentID + " oldAppointmentType: " + appointmentType + " newAppointmentID: " + newAppointmentID + " newAppointmentType: " + newAppointmentType + " ", serverResponse);
                break;
            case SHUTDOWN:
//                    ClientLogger.clientLog(patientID, " attempting ORB shutdown");
                userService.shutdown();
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
        String branchCode = serverBranch(appointmentAdminID);
        if (branchCode == "Null") {
            System.out.println("Invalid branch! Please re-enter a valid user ID");
            return;
        }

        URL url = new URL("http://" + FrontEnd.FE_IP_Address + ":9004/fe/?wsdl");
//        System.out.println(url);
        QName qName = new QName("http://FrontEnd/", "FEServicesImplService");
//        System.out.println(qName);
        Service service = Service.create(url, qName);
        IFEServices userService = service.getPort(IFEServices.class);

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
                serverResponse = userService.addAppointment(appointmentAdminID, appointmentID, appointmentType, capacity);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " addAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " appointmentCapacity: " + capacity + " ", serverResponse);
                break;
            case ADMIN_REMOVE_APPOINTMENT:
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
//                ClientLogger.clientLog(appointmentAdminID, " attempting to removeAppointment");
                serverResponse = userService.removeAppointment(appointmentAdminID, appointmentID, appointmentType);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " removeAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_LIST_APPOINTMENT_AVAILABILITY:
                appointmentType = promptForAppointmentType();
//                ClientLogger.clientLog(appointmentAdminID, " attempting to listAppointmentAvailability");
                serverResponse = userService.listAppointmentAvailability(appointmentAdminID, appointmentType);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " listAppointmentAvailability", " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_BOOK_APPOINTMENT:
                patientID = askForPatientIDFromAdmin(appointmentAdminID.substring(0, 3));
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
//                ClientLogger.clientLog(appointmentAdminID, " attempting to bookAppointment");
                serverResponse = userService.bookAppointment(patientID, appointmentID, appointmentType);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " bookAppointment", " patientID: " + patientID + " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_GET_APPOINTMENT_SCHEDULE:
                patientID = askForPatientIDFromAdmin(appointmentAdminID.substring(0, 3));
//                ClientLogger.clientLog(appointmentAdminID, " attempting to getAppointmentSchedule");
                serverResponse = userService.getAppointmentSchedule(patientID);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " getAppointmentSchedule", " patientID: " + patientID + " ", serverResponse);
                break;
            case ADMIN_CANCEL_APPOINTMENT:
                patientID = askForPatientIDFromAdmin(appointmentAdminID.substring(0, 3));
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
//                ClientLogger.clientLog(appointmentAdminID, " attempting to cancelAppointment");
                serverResponse = userService.cancelAppointment(patientID, appointmentID, appointmentType);
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
                serverResponse = userService.swapAppointment(patientID, newAppointmentID, newAppointmentType, appointmentID, appointmentType);
                System.out.println(serverResponse);
//                ClientLogger.clientLog(appointmentAdminID, " swapAppointment", " patientID: " + patientID + " oldAppointmentID: " + appointmentID + " oldAppointmentType: " + appointmentType + " newAppointmentID: " + newAppointmentID + " newAppointmentType: " + newAppointmentType + " ", serverResponse);
                break;
            case SHUTDOWN:
//                ClientLogger.clientLog(appointmentAdminID, " attempting ORB shutdown");
                userService.shutdown();
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
        String userID = userInput.next().trim().toUpperCase();
        if (checkUserType(userID) != USER_TYPE_PATIENT || !userID.substring(0, 3).equalsIgnoreCase(branchCode)) {

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