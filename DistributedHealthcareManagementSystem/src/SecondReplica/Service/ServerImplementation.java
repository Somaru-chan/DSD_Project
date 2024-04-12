package SecondReplica.Service;

import FirstReplica.Database.AppointData;
import FirstReplica.Database.PatientData;

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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements the web interface
 */

@WebService(endpointInterface = "Service.WebInterface")
//@SOAPBinding(style = SOAPBinding.Style.RPC)

public class ServerImplementation implements WebInterface {

    private ConcurrentHashMap<String, ConcurrentHashMap<String, AppointData>> appointmentsMap;

    private ConcurrentHashMap<String, ConcurrentHashMap<String, PatientData>> patientMap;

    private String serverName = "";

    private  int qPort;
    private int mPort;
    private int shPort;

    public ServerImplementation() {
        super();
    }

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
            return "appointment added capacity";
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
            return "appointment added to " + serverName.toUpperCase().trim();
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
            return "appointment added to " + serverName.toUpperCase().trim();
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
                sendRequestToServer(mPort, "removePatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-");
                sendRequestToServer(shPort, "removePatientAppointment",appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-");
            }
            else if(branch.trim().equals("MTL"))
            {
                sendRequestToServer(qPort, "removePatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-");
                sendRequestToServer(shPort, "removePatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-");

            }
            else if(branch.trim().equals("SHE"))
            {
                sendRequestToServer(mPort, "removePatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-");
                sendRequestToServer(qPort, "removePatientAppointment", appointmentID.toUpperCase().trim(), appointmentType.toUpperCase().trim(),"-");
            }

            return response;
        }
        else
        {
            return "Error: There is no such appointment to remove";
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
                //patient.getValue().remove(appointmentType.toUpperCase().trim() +";"+ appointmentID.toUpperCase().trim());
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

    public String listAppointmentAvailability(String appointmentType){
        String response = "List of availability for "+ appointmentType +":\n";
        if ((!appointmentsMap.isEmpty())) {
            if (appointmentsMap.containsKey(appointmentType.trim())) {
                for (Map.Entry<String, AppointData> entry : appointmentsMap.get(appointmentType.toUpperCase().trim()).entrySet()) {
                    response += entry.getKey() + " " + entry.getValue().bookingCapacity + ", \n ";
                }
            }
        }
        if(serverName.trim().equals("QUE"))
        {
            response += sendRequestToServer(mPort, "listAppointmentAvailability", "-", appointmentType.toUpperCase().trim(),"-");
            response += sendRequestToServer(shPort, "listAppointmentAvailability", "-", appointmentType.toUpperCase().trim(),"-");

        }
        else if(serverName.trim().equals("MTL"))
        {
            response += sendRequestToServer(qPort, "listAppointmentAvailability", "-", appointmentType.toUpperCase().trim(),"-");
            response += sendRequestToServer(shPort, "listAppointmentAvailability", "-", appointmentType.toUpperCase().trim(),"-");
        }
        else if(serverName.trim().equals("SHE"))
        {
            response += sendRequestToServer(mPort, "listAppointmentAvailability", "-", appointmentType.toUpperCase().trim(),"-");
            response += sendRequestToServer(qPort, "listAppointmentAvailability", "-", appointmentType.toUpperCase().trim(),"-");
        }

        return response;
    }

    /**
     * Function to Book appointments, to be called by patient or admin
     */

    public synchronized String bookAppointment(String patientID, String appointmentID, String appointmentType){
        String response="";
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
        String response = "";

        if(patientMap.containsKey(patientID.toUpperCase().trim()))
        {
            for (ConcurrentHashMap.Entry<String, PatientData> entry : patientMap.get(patientID.toUpperCase().trim()).entrySet())
            {
                String [] data = entry.getKey().split(";");
                response += "appointmentType:" + data[0] + " appointmentID:" + data[1]+"\n";
            }
            return response;
        }
        else
            return "No appointments found for this patient";
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

    public String bookNextAppointment(String temp, String appointmentType) {

        String response="";
        String [] data = temp.split(":");
        String appointmentID="";
        int capacity =0;

        if(appointmentsMap.containsKey(appointmentType.toUpperCase().trim()) && appointmentsMap.get(appointmentType.toUpperCase().trim()).values().size() != 0)
        {
            if(data.length!= 0)
            {
                for (ConcurrentHashMap.Entry<String,AppointData> entry : appointmentsMap.get(appointmentType).entrySet())
                {
                    boolean check = false;
                    for (int i =0; i< data.length; ++i)
                    {
                        capacity = entry.getValue().bookingCapacity;

                        if(!(data[i].indexOf(entry.getKey().toUpperCase().trim())!=-1) && capacity!=0)
                        {
                            check = true;
                        }
                        else
                        {
                            check = false;
                            break;
                        }
                    }
                    if(check == true)
                    {
                        appointmentID = entry.getKey().toUpperCase().trim();
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
                ConcurrentHashMap.Entry<String,AppointData> entry = appointmentsMap.get(appointmentType).entrySet().iterator().next();
                capacity = entry.getValue().bookingCapacity;
                if(capacity!=0)
                {
                    appointmentID = entry.getValue().appointmentID.toUpperCase().trim();
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
                fileName = dir + "/src/Logs/Montreal_logs.txt";
            } else if (ID.equals("SHE")) {
                fileName = dir + "/src/Logs/Sherbrooke_logs.txt";
            } else if (ID.equals("QUE")) {
                fileName = dir + "/src/Logs/Quebec_logs.txt";
            }
        }
        else
        {
            fileName = dir + "/src/Logs/"+ ID +"_logs.txt";
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
}
