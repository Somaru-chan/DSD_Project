package SecondReplica.Servers;

import Service.ServerImplementation;

import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class SherbrookeServer {

    public static void main(String args[]) throws Exception {

        try {

            ServerImplementation serverImpl = new ServerImplementation(6230, 6231, 6229, "SHE");
            Endpoint ep = Endpoint.publish("http://localhost:6229/sherbrooke", serverImpl);
            System.out.println("Service is successfully published: " + ep.isPublished());

            System.out.println("Sherbrooke Server Ready");

            Runnable task = () -> {
                runServer(serverImpl);
            };
            Thread thread = new Thread(task);
            thread.start();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void runServer(ServerImplementation ServerImpl) {
        DatagramSocket socket = null;
        String response = "";
        try {
            socket = new DatagramSocket(6229);
            byte[] buffer = new byte[5000];
            System.out.println("Sherbrooke UDP Server Started at 6229");
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                String data = new String( request.getData(), 0, request.getLength() );
                String[] parts = data.split(";");
                String function = parts[0];
                String appointmentID = parts[1];
                String appointmentType = parts[2];
                String patientID= parts[3];
                if(function.equals("removePatientAppointment")) {
                    String result = ServerImpl.removePatientAppointment(appointmentID, appointmentType);
                    response= result;
                }
                else if(function.equals("bookAppointment")) {
                    String result = ServerImpl.bookAppointment(patientID, appointmentID, appointmentType);
                    response= result;
                }
                else if(function.equals("listAppointmentAvailability")) {
                    String result = ServerImpl.listAppointmentAvailability(appointmentType);
                    response= result;
                }
                else if(function.equals("cancelPatientAppointment")) {
                    String result = ServerImpl.cancelPatientAppointment(appointmentID, appointmentType);
                    response= result;
                }
                else if(function.equals("bookNextAppointment")) {
                    String result = ServerImpl.bookNextAppointment(appointmentID, appointmentType);
                    response= result;
                }
                byte[] sendData = response.getBytes();
                DatagramPacket reply = new DatagramPacket(sendData, response.length(), request.getAddress(),request.getPort());
                socket.send(reply);
            }
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (socket != null)
                socket.close();
        }

    }

//    public static void main(String[] args) throws Exception{
//        Registry registry = LocateRegistry.createRegistry(6229);
//        DatagramSocket socket = new DatagramSocket(6229);
//        ServerImplementation sheServerImplementation = new ServerImplementation("SHE");
//        sheServerImplementation.serverName = "Sherbrooke";
//        Naming.rebind("rmi://localhost:6229/she", sheServerImplementation);
//        System.out.println("Sherbrooke com.Servers Ready");
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            public void run() {
//                socket.close();
//            }
//        });
//        while(true) {
//            requestHandler(sheServerImplementation, socket);
//        }
//
////		socket.close();
//    }
//    public static void requestHandler(ServerImplementation sheServerImplementation, DatagramSocket socket) throws IOException{
//        //Send request to get data.
//        try {
//            String responseReturn = "";
//            byte[] b = new byte[1024];
//            DatagramPacket dp1 = new DatagramPacket(b, b.length);
//            socket.receive(dp1);
//
//            //Now to send response
//            String request = new String(dp1.getData()).trim();
//            String [] requestMessageArray = request.split(",");
//            String appointmentType = requestMessageArray[0];
//            int port = Integer.parseInt(requestMessageArray[1]);
//            String operation = requestMessageArray[2];
//            String userID = requestMessageArray[3];
//            String appointmentID = requestMessageArray[4];
//            String capacity = requestMessageArray[5];
//
//            switch(operation) {
//                case "List Appointment Availability":{
//                    String result = sheServerImplementation.listAppointmentAvailability(appointmentType);
//                    responseReturn = result;
//                    break;
//                }
//
//
//                case "Book Appointment":{
//                    //patient ID rather than user ID
//                    String result = sheServerImplementation.bookAppointment(userID, appointmentID, appointmentType);
//                    responseReturn = result;
//                    break;
//                }
//
//                case "Get Appointment Schedule":{
//                    //patient ID rather than user ID
//                    String result= sheServerImplementation.getAppointmentSchedule(userID);
//                    responseReturn=result;
//                    break;
//                }
//
//                case "Cancel Appointment`":{
//                    //patient ID rather than user ID
//                    String result= sheServerImplementation.cancelAppointment(userID, appointmentID);
//                    responseReturn=result;
//                    break;
//                }
//
//                default:{
//                    break;
//                }
//            }
//
//            String message = responseReturn.trim();
//            byte[] b2 = message.getBytes();
//            DatagramPacket dp2 = new DatagramPacket(b2, b2.length,dp1.getAddress(),dp1.getPort());
//            socket.send(dp2);
//        } catch (SocketException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }
//        return;
//    }

}