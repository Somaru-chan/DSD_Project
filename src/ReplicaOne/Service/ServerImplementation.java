package ReplicaOne.Service;

import ReplicaOne.CommonOutput;
import ReplicaOne.Database.AppointData;
import ReplicaOne.Database.PatientData;

import javax.jws.WebService;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements the web interface
 */

@WebService(endpointInterface = "FirstReplica.Service.WebInterface")
//@SOAPBinding(style = SOAPBinding.Style.RPC)

public class ServerImplementation implements WebInterface {

    private ConcurrentHashMap<String, ConcurrentHashMap<String, AppointData>> appointmentsMap;

    private ConcurrentHashMap<String, ConcurrentHashMap<String, PatientData>> patientMap;

    private String serverName = "";

    private final int qPort;
    private final int mPort;
    private final int shPort;

    public ServerImplementation(int qPort, int mPort, int shPort, String serverName) throws RemoteException {
        super();

        this.qPort = qPort;
        this.mPort = mPort;
        this.shPort = shPort;
        this.serverName = serverName.toUpperCase().trim();
        appointmentsMap = new ConcurrentHashMap<>();
        patientMap = new ConcurrentHashMap<>();

    }

    /**
     * This function uses UDP to implement and send request to the server
     */

    private String sendRequestToServer(int serverPort,String function,String appointmentID, String appointmentType, String patientID) {
        DatagramSocket socket = null;
        String result ="";
        String patientRequest = function+";"+appointmentID.toUpperCase().trim()+";"+appointmentType.toUpperCase().trim()+";" + patientID.toUpperCase().trim();
        try {
            socket = new DatagramSocket();
            byte[] data = patientRequest.getBytes();
            InetAddress host = InetAddress.getLocalHost();
            DatagramPacket request = new DatagramPacket(data, patientRequest.length(), host, serverPort);
            socket.send(request);

            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            socket.receive(reply);
            result = new String(reply.getData());
        } catch (SocketException e) {
            System.out.println("Socket exception: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO Error: " + e.getMessage());
        } finally {
            if (socket != null)
                socket.close();
        }
        return result;

    }
    
    /**
     * Add Appointments function by admins
     */

    public synchronized String addAppointment(String appointmentID, String appointmentType, int bookingCapacity){
        if(appointmentsMap.containsKey(appointmentType.toUpperCase().trim()) && appointmentsMap.get(appointmentType.toUpperCase().trim()).containsKey(appointmentID.toUpperCase().trim()))
        {
            int currentCapacity = appointmentsMap.get(appointmentType.toUpperCase().trim()).get(appointmentID.toUpperCase().trim()).bookingCapacity;
            appointmentsMap.get(appointmentType.toUpperCase().trim()).replace(appointmentID,new AppointData(appointmentType.toUpperCase().trim(), appointmentID.toUpperCase().trim(), currentCapacity + bookingCapacity));

            try
            {
                serverLog("Add appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID +
                        "bookingCapacity:"+ bookingCapacity,"successfully done", "Capacity added to appointment");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return CommonOutput.addAppointmentOutput(true, CommonOutput.addAppointment_success_capacity_updated);
        }
        else if(appointmentsMap.containsKey(appointmentType.toUpperCase().trim()))
        {
            appointmentsMap.get(appointmentType.toUpperCase().trim()).put(appointmentID.toUpperCase().trim(), new AppointData(appointmentType.toUpperCase().trim(), appointmentID.toUpperCase().trim(), bookingCapacity));
            try
            {
                serverLog("Add appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID +
                        "bookingCapacity:"+ bookingCapacity,"successfully done", "appointment added to " + serverName.toUpperCase().trim());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return CommonOutput.addAppointmentOutput(true, CommonOutput.addAppointment_success_added);
        }
        else
        {
            ConcurrentHashMap <String, AppointData> subHashMap = new ConcurrentHashMap<>();
            subHashMap.put(appointmentID.toUpperCase().trim(), new AppointData(appointmentType.toUpperCase().trim(), appointmentID.toUpperCase().trim(), bookingCapacity));
            appointmentsMap.put(appointmentType.toUpperCase().trim(), subHashMap);
            try
            {
                serverLog("Add appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID +
                        "bookingCapacity:"+ bookingCapacity,"successfully done", "appointment added to " + serverName.toUpperCase().trim());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return CommonOutput.addAppointmentOutput(true, CommonOutput.addAppointment_success_added);
        }
    }


    public String add_book_patient(String patientID, String appointmentID, String appointmentType)
    {
        String response = "";
        String appointData = appointmentType.toUpperCase().trim()+ ";" + appointmentID.toUpperCase().trim();

        if(patientMap.containsKey(patientID.toUpperCase().trim()))
        {
            patientMap.get(patientID.toUpperCase().trim()).put(appointData, new PatientData(patientID.toUpperCase().trim(), appointmentType.toUpperCase().trim(), appointmentID.toUpperCase().trim()));
            response = "BOOKED";
        }
        else
        {
            ConcurrentHashMap <String, PatientData> subHashMap = new ConcurrentHashMap<>();
            subHashMap.put(appointData, new PatientData(patientID.toUpperCase().trim(), appointmentType.toUpperCase().trim(), appointmentID.toUpperCase().trim()));
            patientMap.put(patientID.toUpperCase().trim(), subHashMap);
            response = "BOOKED";
        }
        return response;
    }

    /**
     * Function to remove appointsments by admin
     */

    public synchronized String removeAppointment(String appointmentID, String appointmentType){
        if(appointmentsMap.containsKey(appointmentType.toUpperCase().trim()) && appointmentsMap.get(appointmentType.toUpperCase().trim()).containsKey(appointmentID.toUpperCase().trim()))
        {
            String response="";
            String branch = appointmentID.substring(0,3).toUpperCase().trim();
            appointmentsMap.get(appointmentType.toUpperCase().trim()).remove(appointmentID.toUpperCase().trim());
            try {
                serverLog("Remove appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID
                        ,"successfully done", "appointment removed from server" + serverName.toUpperCase().trim());
            } catch (IOException e) {
                e.printStackTrace();
            }

            response = removePatientAppointment(appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim());

            if(branch.trim().equals("QUE"))
            {
                sendRequestToServer(mPort, "removePatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-").trim();
                sendRequestToServer(shPort, "removePatientAppointment",appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-").trim();
            }
            else if(branch.trim().equals("MTL"))
            {
                sendRequestToServer(qPort, "removePatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-").trim();
                sendRequestToServer(shPort, "removePatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-").trim();

            }
            else if(branch.trim().equals("SHE"))
            {
                sendRequestToServer(mPort, "removePatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-").trim();
                sendRequestToServer(qPort, "removePatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-").trim();
            }

            return CommonOutput.removeAppointmentOutput(true, null);
        }
        else
        {
            return CommonOutput.removeAppointmentOutput(false, CommonOutput.removeAppointment_fail_no_such_appointment);
        }
    }

    public String removePatientAppointment(String appointmentID, String appointmentType) {
        String data = "";
        String new_appointmentID = "";
        for(Map.Entry<String, ConcurrentHashMap<String, PatientData>> patient : patientMap.entrySet())
        {
            ConcurrentHashMap<String, PatientData> appointData = patient.getValue();
            String branch = appointmentID.substring(0,3).toUpperCase().trim();

            if(appointData.containsKey(appointmentType.toUpperCase().trim() +";"+ appointmentID.toUpperCase().trim()+""))
            {
                appointData.remove(appointmentType.toUpperCase().trim() +";"+ appointmentID.toUpperCase().trim());
                for (ConcurrentHashMap.Entry<String,PatientData> entry : patient.getValue().entrySet())
                {
                    data +=(entry.getValue().appointmentID.toUpperCase().trim()+":");
                }
                if(branch.trim().equals("QUE"))
                {
                    new_appointmentID = sendRequestToServer(qPort, "bookNextAppointment", data, appointmentType.toUpperCase().trim(),"-");
                }
                else if(branch.trim().equals("MTL"))
                {
                    new_appointmentID = sendRequestToServer(mPort, "bookNextAppointment", data, appointmentType.toUpperCase().trim(),"-");

                }
                else if(branch.trim().equals("SHE"))
                {
                    new_appointmentID = sendRequestToServer(shPort, "bookNextAppointment", data, appointmentType.toUpperCase().trim(),"-");
                }

                try
                {
                    if(new_appointmentID.trim().equals(""))
                    {
                        serverLog("Remove appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID,"successfully done",
                                "appointment removed for patient:" + patient.getKey().toUpperCase().trim());

                        patientLog(patient.getKey().toUpperCase().trim(), "Remove appointment", "appointmentType:" + appointmentType+" appointmentID:"+appointmentID+" has been removed");
                    }
                    else
                    {
                        add_book_patient(patient.getKey().toUpperCase().trim(),new_appointmentID, appointmentType);
                        serverLog("Remove appointment", " appointmentType:"+appointmentType+ " appointmentID:"+new_appointmentID,"successfully done",
                                "appointment has been replaced for patient:" + patient.getKey().toUpperCase().trim());

                        patientLog(patient.getKey().toUpperCase().trim(), "Remove appointment", "appointmentType:" + appointmentType+" appointmentID:"+new_appointmentID+" has been replaced");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "appointment with appointmentID:"+ appointmentID.toUpperCase().trim() +" and appointmentType: "+appointmentType.toUpperCase().trim() +" for patients has been removed.\n";
    }



    /**
     * Function to list availability of Appointments
     */

    public String listAppointmentAvailability( String appointmentType) {
        List<String> allEventIDsWithCapacity = new ArrayList<>();
        String firstResponse="", secondResponse="";
        List<String> firstServer;
        List<String> secondServer;
        if(appointmentsMap.containsKey(appointmentType.toUpperCase().trim()))
        {
            for ( Map.Entry<String, AppointData> entry : appointmentsMap.get(appointmentType.toUpperCase().trim()).entrySet())
            {
                allEventIDsWithCapacity.add(entry.getKey() + " " + entry.getValue().bookingCapacity);
            }
        }
        if(serverName.trim().equals("QUE"))
        {
            firstResponse += sendRequestToServer(mPort, "listAppointments", "-", appointmentType.toUpperCase().trim(),"-").trim();
            secondResponse += sendRequestToServer(shPort, "listAppointments", "-", appointmentType.toUpperCase().trim(),"-").trim();

        }
        else if(serverName.trim().equals("MTL"))
        {
            firstResponse = sendRequestToServer(qPort, "listAppointments", "-", appointmentType.toUpperCase().trim(),"-").trim();
            secondResponse = sendRequestToServer(shPort, "listAppointments", "-", appointmentType.toUpperCase().trim(),"-").trim();
        }
        else if(serverName.trim().equals("SHE"))
        {
            firstResponse = sendRequestToServer(mPort, "listAppointments", "-", appointmentType.toUpperCase().trim(),"-").trim();
            secondResponse = sendRequestToServer(qPort, "listAppointments", "-", appointmentType.toUpperCase().trim(),"-").trim();
        }
        firstServer = Arrays.asList(firstResponse.split("@"));
        secondServer = Arrays.asList(secondResponse.split("@"));
        allEventIDsWithCapacity.addAll(firstServer);
        allEventIDsWithCapacity.addAll(secondServer);
        return CommonOutput.listAppointmentAvailabilityOutput(true, allEventIDsWithCapacity, null);
    }

    public String listAppointments( String eventType)
    {
        String response = "";

        if(appointmentsMap.containsKey(eventType.toUpperCase().trim()))
        {
            for ( ConcurrentHashMap.Entry<String, AppointData> entry : appointmentsMap.get(eventType).entrySet())
            {
                response += entry.getKey() + " " + entry.getValue().bookingCapacity+"@";
            }
        }
        if (response.endsWith("@"))
            response = response.substring(0, response.length() - 1);
        return response;
    }


    /**
     * Function to Book appointments, to be called by patient or admin
     */

    public synchronized String bookAppointment(String patientID, String appointmentID, String appointmentType){
        String response = "";
        String city = appointmentID.substring(0,3).toUpperCase().trim();
        String AppointData = appointmentType.toUpperCase().trim()+ ";" + appointmentID.toUpperCase().trim();

        if(city.trim().equals(serverName))
        {
            response = book_accepted_appointment(patientID, appointmentID, appointmentType);

            if(response.indexOf("ERR_NO_CAPACITY")!=-1)
            {
                try
                {
                    serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID +" patientID:"+ patientID,"failed","There is no capacity for this appointment");
                    patientLog(patientID, "Book an appointment", "There is no capacity for appointmentType:" + appointmentType+" appointmentID:"+appointmentID);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(response.indexOf("ERR_NO_RECORD")!=-1)
            {
                try
                {
                    serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"failed","There is no such an appointment");
                    patientLog(patientID, "Book an appointment", "There is no such an appointment --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                try
                {
                    int capacity = appointmentsMap.get(appointmentType.toUpperCase().trim()).get(appointmentID.toUpperCase().trim()).bookingCapacity;
                    if(serverName.trim().equals(patientID.substring(0,3).toUpperCase().trim()))
                    {
                        if(patientMap.containsKey(patientID.toUpperCase().trim()) && patientMap.get(patientID.toUpperCase().trim()).containsKey(AppointData))
                        {
                            try
                            {
                                serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID +" patientID:"+ patientID,"failed","This appointment has already been booked");
                                patientLog(patientID, "Book an appointment", "This appointment has already been booked --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            return "ERR_RECORD_EXISTS";
                        }
                        appointmentsMap.get(appointmentType.toUpperCase().trim()).replace(appointmentID,new AppointData(appointmentType.toUpperCase().trim(), appointmentID.toUpperCase().trim(), capacity - 1));
                        add_book_patient(patientID, appointmentID, appointmentType);
                    }
                    else
                        appointmentsMap.get(appointmentType.toUpperCase().trim()).replace(appointmentID,new AppointData(appointmentType.toUpperCase().trim(), appointmentID.toUpperCase().trim(), capacity - 1));

                    serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"successfully done","Booking request has been approved");
                    patientLog(patientID, "Book an appointment", "Booking request has been approved --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        else
        {
            if(city.trim().equals("QUE"))
            {
                if(patientMap.containsKey(patientID.toUpperCase().trim()) && patientMap.get(patientID.toUpperCase().trim()).containsKey(AppointData))
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID +" patientID:"+ patientID,"failed","This appointment has already been booked");
                        patientLog(patientID, "Book an appointment", "This appointment has already been booked --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return "ERR_RECORD_EXISTS";
                }
                if(patientMap.containsKey(patientID.toUpperCase().trim()))
                {
                    if(!week_limit_check(patientID.toUpperCase().trim(), appointmentID.substring(4)))
                    {
                        try
                        {
                            serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"failed","This patient has already booked 3 times from other cities!");
                            patientLog(patientID, "Book an appointment", "This patient has already booked 3 times from other cities --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return "This patient has already booked 3 times from other cities!";
                    }
                }
                response = sendRequestToServer(qPort, "bookAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),patientID.toUpperCase().trim());
                if(response.indexOf("BOOKING_APPROVED")!=-1)
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"successfully done","Booking request has been approved");
                        patientLog(patientID, "Book an appointment", "Booking request has been approved --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    add_book_patient(patientID, appointmentID, appointmentType);
                }
                else if(response.indexOf("ERR_NO_CAPACITY")!=-1)
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID +" patientID:"+ patientID,"failed","There is no capacity for this appointment");
                        patientLog(patientID, "Book an appointment", "There is no capacity for appointmentType:" + appointmentType+" appointmentID:"+appointmentID);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if(response.indexOf("ERR_NO_RECORD")!=-1)
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"failed","There is no such an appointment");
                        patientLog(patientID, "Book an appointment", "There is no such an appointment --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if(city.trim().equals("MTL"))
            {
                if(patientMap.containsKey(patientID.toUpperCase().trim()) && patientMap.get(patientID.toUpperCase().trim()).containsKey(AppointData))
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID +" patientID:"+ patientID,"failed","This appointment has already been booked");
                        patientLog(patientID, "Book an appointment", "This appointment has already been booked --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return "ERR_RECORD_EXISTS";
                }
                if(patientMap.containsKey(patientID.toUpperCase().trim()))
                {
                    if(!week_limit_check(patientID.toUpperCase().trim(), appointmentID.substring(4)))
                    {
                        try
                        {
                            serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"failed","This patient has already booked 3 times from other cities!");
                            patientLog(patientID, "Book an appointment", "This patient has already booked 3 times from other cities --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return "This patient has already booked 3 times from other cities!";
                    }
                }
                response = sendRequestToServer(mPort, "bookAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),patientID.toUpperCase().trim());
                if(response.indexOf("BOOKING_APPROVED")!=-1)
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"successfully done","Booking request has been approved");
                        patientLog(patientID, "Book an appointment", "Booking request has been approved --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    add_book_patient(patientID, appointmentID, appointmentType);
                }
                else if(response.indexOf("ERR_NO_CAPACITY")!=-1)
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID +" patientID:"+ patientID,"failed","There is no capacity for this appointment");
                        patientLog(patientID, "Book an appointment", "There is no capacity for appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if(response.indexOf("ERR_NO_RECORD")!=-1)
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"failed","There is no such an appointment");
                        patientLog(patientID, "Book an appointment", "There is no such an appointment --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if(city.trim().equals("SHE"))
            {
                if(patientMap.containsKey(patientID.toUpperCase().trim()) && patientMap.get(patientID.toUpperCase().trim()).containsKey(AppointData))
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID +" patientID:"+ patientID,"failed","This appointment has already been booked");
                        patientLog(patientID, "Book an appointment", "This appointment has already been booked --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return "ERR_RECORD_EXISTS";
                }
                if(patientMap.containsKey(patientID.toUpperCase().trim()))
                {
                    if(!week_limit_check(patientID.toUpperCase().trim(), appointmentID.substring(4)))
                    {
                        try
                        {
                            serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"failed","This patient has already booked 3 times from other cities!");
                            patientLog(patientID, "Book an appointment", "This patient has already booked 3 times from other cities --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return "This patient has already booked 3 times from other cities!";
                    }
                }
                response = sendRequestToServer(shPort, "bookAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),patientID.toUpperCase().trim());
                if(response.indexOf("BOOKING_APPROVED")!=-1)
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"successfully done","Booking request has been approved");
                        patientLog(patientID, "Book an appointment", "Booking request has been approved --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    add_book_patient(patientID, appointmentID, appointmentType);
                }
                else if(response.indexOf("ERR_NO_CAPACITY")!=-1)
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID +" patientID:"+ patientID,"failed","There is no capacity for this appointment");
                        patientLog(patientID, "Book an appointment", "There is no capacity for appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if(response.indexOf("ERR_NO_RECORD")!=-1)
                {
                    try
                    {
                        serverLog("Book an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"failed","There is no such an appointment");
                        patientLog(patientID, "Book an appointment", "There is no such an appointment --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return response;
    }


    public String book_accepted_appointment(String patientID, String appointmentID, String appointmentType)
    {
        String response="";

        if(appointmentsMap.containsKey(appointmentType.toUpperCase().trim()) && appointmentsMap.get(appointmentType.toUpperCase().trim()).containsKey(appointmentID.toUpperCase().trim()))
        {
            int capacity = appointmentsMap.get(appointmentType.toUpperCase().trim()).get(appointmentID.toUpperCase().trim()).bookingCapacity;

            if( capacity == 0)
                return "ERR_NO_CAPACITY";
            else
            {
                return "BOOKING_APPROVED";
            }
        }
        else
        {
            response = "ERR_NO_RECORD!";
        }

        return response;
    }


    /**
     * Function to get appointment schedule
     */

    public String getAppointmentSchedule(String patientID){
        Map<String, List<String>> appointments = new HashMap<>();
        if(patientMap.containsKey(patientID.toUpperCase().trim()))
        {
            for ( ConcurrentHashMap.Entry<String, PatientData> entry : patientMap.get(patientID.toUpperCase().trim()).entrySet())
            {
                String [] data = entry.getKey().split(";");
                List<String> list;
                if(!appointments.containsKey(data[0]))
                    list=new ArrayList<>();
                else
                    list= appointments.get(data[0]);
                list.add(data[1]);
                appointments.put(data[0], list);
            }
            return CommonOutput.getAppointmentScheduleOutput(true, appointments, null);
        }
        else
            return CommonOutput.getAppointmentScheduleOutput(true, new HashMap<>(), null);
    }
    

    /**
     * Function to Cancel Appointments
     */

    public String cancelAppointment(String patientID, String appointmentID, String appointmentType){
        String AppointData = appointmentType.toUpperCase().trim()+ ";" + appointmentID.toUpperCase().trim();
        String branch = appointmentID.substring(0,3).toUpperCase().trim();

        if(patientMap.containsKey(patientID.toUpperCase().trim()) && patientMap.get(patientID.toUpperCase().trim()).containsKey(AppointData))
        {
            patientMap.get(patientID.toUpperCase().trim()).remove(AppointData);

            if(branch.trim().equals(serverName))
            {
                int currentCapacity = appointmentsMap.get(appointmentType.toUpperCase().trim()).get(appointmentID.toUpperCase().trim()).bookingCapacity;
                appointmentsMap.get(appointmentType.toUpperCase().trim()).replace(appointmentID,new AppointData(appointmentType.toUpperCase().trim(), appointmentID.toUpperCase().trim(), currentCapacity + 1));
                try
                {
                    serverLog("Cancel an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"successfully done","appointment has been canceled");
                    patientLog(patientID, "Cancel an appointment", "appointment has been canceled --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(branch.trim().equals("QUE"))
            {
                sendRequestToServer(qPort, "cancelPatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-");
                try
                {
                    serverLog("Cancel an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"successfully done","appointment has been canceled");
                    patientLog(patientID, "Cancel an appointment", "appointment has been canceled --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            else if(branch.trim().equals("MTL"))
            {
                sendRequestToServer(mPort, "cancelPatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-");
                try
                {
                    serverLog("Cancel an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"successfully done","appointment has been canceled");
                    patientLog(patientID, "Cancel an appointment", "appointment has been canceled --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            else if(branch.trim().equals("SHE"))
            {
                sendRequestToServer(shPort, "cancelPatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-");
                try
                {
                    serverLog("Cancel an appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID+" patientID:"+ patientID,"successfully done","appointment has been canceled");
                    patientLog(patientID, "Cancel an appointment", "appointment has been canceled --> appointmentType:" + appointmentType+" appointmentID:"+appointmentID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return "appointment for patient cancelled";
        }
        else
            return "No record for this appointment";
    }

    public String cancelPatientAppointment(String appointmentID, String appointmentType) {
        if(appointmentsMap.containsKey(appointmentType.toUpperCase().trim()) && appointmentsMap.get(appointmentType.toUpperCase().trim()).containsKey(appointmentID.toUpperCase().trim()))
        {
            int currentCapacity = appointmentsMap.get(appointmentType.toUpperCase().trim()).get(appointmentID.toUpperCase().trim()).bookingCapacity;
            appointmentsMap.get(appointmentType.toUpperCase().trim()).replace(appointmentID,new AppointData(appointmentType.toUpperCase().trim(), appointmentID.toUpperCase().trim(), currentCapacity + 1));
        }
        return "CANCELED";

    }

    public String bookNextAppointment(String temp, String removedAppointmentID, String appointmentType) {

        String response="";
        String [] data = temp.split(":");
        String appointmentID="";
        int capacity =0;
        List<String> sortedAppointmentIDs;

        if(appointmentsMap.containsKey(appointmentType.toUpperCase().trim()) && appointmentsMap.get(appointmentType.toUpperCase().trim()).values().size() != 0)
        {
            sortedAppointmentIDs = getSortedAppointmentID(appointmentType, removedAppointmentID);
            if(data.length!= 0)
            {
                for (int c = 0; c<sortedAppointmentIDs.size(); c++) {
                    boolean check = false;
                    for (int i =0; i< data.length; i++)
                    {
                        capacity = appointmentsMap.get(appointmentType).get(sortedAppointmentIDs.get(c)).bookingCapacity;

                        if(!(data[i].indexOf(appointmentsMap.get(appointmentType).get(sortedAppointmentIDs.get(c)).appointmentID)!=-1) && capacity!=0)
                        {
                            check = true;
                        }
                        else
                        {
                            check = false;
                            break;
                        }
                    }
                    if(check)
                    {
                        appointmentID = appointmentsMap.get(appointmentType).get(sortedAppointmentIDs.get(c)).appointmentID.toUpperCase().trim();
                        appointmentsMap.get(appointmentType.toUpperCase().trim()).replace(appointmentID,new AppointData(appointmentType.toUpperCase().trim(), appointmentID.toUpperCase().trim(), capacity - 1));
                        try
                        {
                            serverLog("Remove appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID,"successfully done",
                                    "Next available appointment replaced for patient:");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return appointmentID;
                    }
                }
            }
            else
            {
                capacity = appointmentsMap.get(appointmentType).get(sortedAppointmentIDs.get(0)).bookingCapacity;
                if(capacity!=0)
                {
                    appointmentID = appointmentsMap.get(appointmentType).get(sortedAppointmentIDs.get(0)).appointmentID.toUpperCase().trim();
                    appointmentsMap.get(appointmentType.toUpperCase().trim()).replace(appointmentID,new AppointData(appointmentType.toUpperCase().trim(), appointmentID.toUpperCase().trim(), capacity - 1));
                    try
                    {
                        serverLog("Remove appointment", " appointmentType:"+appointmentType+ " appointmentID:"+appointmentID,"successfully done",
                                "Next available appointment replaced for client:");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return appointmentID;
                }
            }
        }

        return response;
    }


    private List<String> getSortedAppointmentID( String appointmentType,  String removedAppointmentID) {
        List<String> sortedAppointmentIDs = new ArrayList<>();
        List<String> morningAppointmentIDs = new ArrayList<>();
        List<String> afternoonAppointmentIDs = new ArrayList<>();
        List<String> eveningAppointmentIDs = new ArrayList<>();

        for (ConcurrentHashMap.Entry<String, AppointData> entry : appointmentsMap.get(appointmentType).entrySet()) {
            if (entry.getValue().appointmentID.startsWith("M", 3) && !removedAppointmentID.startsWith("A", 3) && !removedAppointmentID.startsWith("E", 3)) {
                if (Integer.parseInt(entry.getValue().appointmentID.substring(8)) >= Integer.parseInt(removedAppointmentID.substring(8)) && Integer.parseInt(entry.getValue().appointmentID.substring(6, 8)) >= Integer.parseInt(removedAppointmentID.substring(6, 8)) && Integer.parseInt(entry.getValue().appointmentID.substring(4, 6)) >= Integer.parseInt(removedAppointmentID.substring(4, 6))) {
                    morningAppointmentIDs.add(entry.getValue().appointmentID);
                }
            } else if (entry.getValue().appointmentID.startsWith("A", 3) && !removedAppointmentID.startsWith("E", 3)) {
                if (Integer.parseInt(entry.getValue().appointmentID.substring(8)) >= Integer.parseInt(removedAppointmentID.substring(8)) && Integer.parseInt(entry.getValue().appointmentID.substring(6, 8)) >= Integer.parseInt(removedAppointmentID.substring(6, 8)) && Integer.parseInt(entry.getValue().appointmentID.substring(4, 6)) >= Integer.parseInt(removedAppointmentID.substring(4, 6))) {
                    afternoonAppointmentIDs.add(entry.getValue().appointmentID);
                }
            } else if (entry.getValue().appointmentID.startsWith("E", 3)) {
                if (Integer.parseInt(entry.getValue().appointmentID.substring(8)) >= Integer.parseInt(removedAppointmentID.substring(8)) && Integer.parseInt(entry.getValue().appointmentID.substring(6, 8)) >= Integer.parseInt(removedAppointmentID.substring(6, 8)) && Integer.parseInt(entry.getValue().appointmentID.substring(4, 6)) >= Integer.parseInt(removedAppointmentID.substring(4, 6))) {
                    eveningAppointmentIDs.add(entry.getValue().appointmentID);
                }
            }
        }

        sortByDate(morningAppointmentIDs);
        sortByDate(afternoonAppointmentIDs);
        sortByDate(eveningAppointmentIDs);

        sortedAppointmentIDs.addAll(morningAppointmentIDs);
        sortedAppointmentIDs.addAll(afternoonAppointmentIDs);
        sortedAppointmentIDs.addAll(eveningAppointmentIDs);

        return sortedAppointmentIDs;
    }

    private List<String> sortByDate( List<String> list) {
        int n = list.size();
        //sort by year
        for (int i = 0; i < n-1; i++)
            for (int j = 0; j < n-i-1; j++)
            {
                int a = Integer.parseInt(list.get(j).substring(8));
                int b = Integer.parseInt(list.get(j+1).substring(8));

                if (a > b)
                {
                    Collections.swap(list, j, j+1);
                }
            }
        //sort by month
        for (int i = 0; i < n-1; i++)
            for (int j = 0; j < n-i-1; j++)
            {
                int a = Integer.parseInt(list.get(j).substring(6,8));
                int b = Integer.parseInt(list.get(j+1).substring(6,8));

                if (a > b)
                {
                    Collections.swap(list, j, j+1);
                }
            }
        //sort by day
        for (int i = 0; i < n-1; i++)
            for (int j = 0; j < n-i-1; j++)
            {
                int a = Integer.parseInt(list.get(j).substring(4,6));
                int b = Integer.parseInt(list.get(j+1).substring(4,6));

                if (a > b)
                {
                    Collections.swap(list, j, j+1);
                }
            }
        return list;
    }
    
    
    /*
    function to swap appointments if available
     */

    public synchronized String swapAppointment(String patientID, String newappointmentID, String newappointmentType, String oldappointmentID, String oldappointmentType) {
        String AppointData = oldappointmentType.toUpperCase().trim()+ ";" + oldappointmentID.toUpperCase().trim();
        String response = "";
        if(!week_limit_check(patientID.toUpperCase().trim(), newappointmentID.substring(4)))
        {
            if(!oldappointmentID.substring(0, 3).equals(serverName.trim()))
            {
                response = cancelAppointment(patientID, oldappointmentID, oldappointmentType);
                if(response.trim().equals("appointment for patient cancelled"))
                {
                    response = bookAppointment(patientID, newappointmentID, newappointmentType);
                    return "appointment for patient swapped";
                }
            }
            else
            {
                try
                {
                    serverLog("Swap an appointment", " oldappointmentType:"+oldappointmentType+ " oldappointmentID:"+oldappointmentID+"newappointmentType:"+ newappointmentType+" newappointmentID:"+newappointmentID+"patientID:"+ patientID,"failed","This patient has already booked 3 times from other cities!");
                    patientLog(patientID, "Book an appointment", "This patient has already booked 3 times from other cities");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return "This patient has already booked 3 times from other cities!";
            }
        }
        else if(patientMap.containsKey(patientID.toUpperCase().trim()) && patientMap.get(patientID.toUpperCase().trim()).containsKey(AppointData))
        {
            response = bookAppointment(patientID, newappointmentID, newappointmentType);
            if(response.trim().equals("BOOKING_APPROVED"))
            {
                response = cancelAppointment(patientID, oldappointmentID, oldappointmentType);
                return "appointment for patient swapped";
            }
        }
        else
            response = "The new appointmentID or appointmentType does not exists!";
        return response;
    }

    @Override
    public String shutdown() {
        String status;
        appointmentsMap = new ConcurrentHashMap<>();
        patientMap = new ConcurrentHashMap<>();
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignored
                }
                System.exit(1);
            }
        });
        status = "Shutting down";
        return status;
    }


    private boolean same_week_check(String newAppointDate, String appointmentID)
    {
        if (appointmentID.substring(2, 4).equals(newAppointDate.substring(2, 4)) && appointmentID.substring(4, 6).equals(newAppointDate.substring(4, 6)))
        {
            int w1 = Integer.parseInt(appointmentID.substring(0, 2)) / 7;
            int w2 = Integer.parseInt(newAppointDate.substring(0, 2)) / 7;

            if(w1 == w2)
                return true;
            else
                return false;
        }
        else
            return false;

    }

    public boolean week_limit_check(String patientID,String appointmentDate)
    {
        int limit = 0;

        for(Map.Entry<String, PatientData> appointments : patientMap.get(patientID).entrySet())
        {
            if(!appointments.getValue().appointmentID.substring(0, 3).equals(serverName) && same_week_check(appointments.getValue().appointmentID.substring(4), appointmentDate))
            {
                limit++;
            }
        }
        if (limit < 3)
            return true;
        else
            return false;
    }


    private static String getDirectory(String ID, String type) {
        final String dir = System.getProperty("user.dir");
        String fileName = dir;
        if(type == "Servers")
        {
            if (ID.equals("MTL")) {
                fileName = dir + "\\src\\FirstReplica\\Logs\\Montreal_logs.txt";
            } else if (ID.equals("SHE")) {
                fileName = dir + "\\src\\FirstReplica\\Logs\\Sherbrooke_logs.txt";
            } else if (ID.equals("QUE")) {
                fileName = dir + "\\src\\FirstReplica\\Logs\\Quebec_logs.txt";
            }
        }
        else
        {
            fileName = dir + "\\src\\FirstReplica\\Logs\\"+ ID +"_logs.txt";
        }
        return fileName;
    }

    /**
     * This function is invoked to write in a log file
     */

    public void serverLog(String operation, String peram, String requestResult, String response) throws IOException {
        String city = serverName;
        Date date = new Date();
        String strDateFormat = "yyyy-MM-dd hh:mm:ss a";
        DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
        String formattedDate= dateFormat.format(date);

        FileWriter fileWriter = new FileWriter(getDirectory(city.trim().toUpperCase(), "Servers"),true);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println("DATE: "+formattedDate+"| Request type: "+operation+" | Request parameters: "+ peram +" | Request result: "+requestResult+" | com.Servers response: "+ response);

        printWriter.close();

    }

    public void patientLog(String ID, String operation, String response) throws IOException {
        FileWriter fileWriter = new FileWriter(getDirectory(ID, "patients"),true);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println("Request type: "+operation+" | Response: "+ response);

        printWriter.close();

    }

    public String shutDown() throws RemoteException {
        appointmentsMap = new ConcurrentHashMap<>();
        patientMap = new ConcurrentHashMap<>();
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignored
                }
                System.exit(1);
            }
        });
        return "Shutting down";
    }
}
