package ReplicaOne.Servers;

import FrontEnd.FrontEnd;
import ReplicaOne.Service.ServerImplementation;

import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.*;

public class QuebecServer {

    public static final String serverIP = FrontEnd.serverIP;

    /*static {
        try {
            serverIP = InetAddress.getLocalHost().getHostAddress();
            System.out.println(serverIP);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }*/

    public static void main(String args[]) throws Exception {

        try {

            ServerImplementation serverImpl = new ServerImplementation(6130, 6131, 6129, "QUE");
            Endpoint ep = Endpoint.publish("http://"+ serverIP + ":6130/quebec", serverImpl);
            System.out.println("Service is successfully published: " + ep.isPublished());

            System.out.println("Quebec Server Ready");

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
            socket = new DatagramSocket(6130);
            byte[] buffer = new byte[5000];
            System.out.println("Quebec UDP Server Started at 6130");
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
                else if(function.equals("listAppointments")) {
                    String result = ServerImpl.listAppointments(appointmentType);
                    response= result;
                }
                else if(function.equals("cancelPatientAppointment")) {
                    String result = ServerImpl.cancelPatientAppointment(appointmentID, appointmentType);
                    response= result;
                }
                else if(function.equals("bookNextAppointment")) {
                    String result = ServerImpl.bookNextAppointment(appointmentID, appointmentType, patientID);
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
}