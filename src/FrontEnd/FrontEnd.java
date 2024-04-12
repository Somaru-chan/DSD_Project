package FrontEnd;

import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class FrontEnd {
    private static final int sequencerPort = 1333;
    //    private static final String sequencerIP = "192.168.2.17";
    private static final String sequencerIP = "localhost";
    private static final String RM_Multicast_group_address = "233.252.30.10";
    private static final int FE_SQ_PORT = 1414;
    private static final int FE_PORT = 1999;
    private static final int RM_Multicast_Port = 1234;
    //    public static String FE_IP_Address = "192.168.2.11";
    public static String FE_IP_Address = "localhost";

    public static void main(String[] args) {
        try {
            IFrontEnd inter = new IFrontEnd() {
                @Override
                public void informRmHasBug(int RmNumber) {
                    MyRequest errorMessage = new MyRequest(RmNumber, "1");
                    System.out.println("Rm:" + RmNumber + "has bug");
                    sendUnicastToSequencer(errorMessage);
                }

                @Override
                public void informRmIsDown(int RmNumber) {
                    MyRequest errorMessage = new MyRequest(RmNumber, "2");
                    System.out.println("Rm:" + RmNumber + "is down");
                    sendUnicastToSequencer(errorMessage);
                }

                @Override
                public int sendRequestToSequencer(MyRequest myRequest) {
                    return sendUnicastToSequencer(myRequest);
                }

                @Override
                public void retryRequest(MyRequest myRequest) {
                    System.out.println("No response from all Rms, Retrying request...");
                    sendUnicastToSequencer(myRequest);
                }
            };
            FEServicesImpl servant = new FEServicesImpl(inter);
            Runnable task = () -> {
                listenForUDPResponses(servant);
            };
            Thread thread = new Thread(task);
            thread.start();

            Endpoint endpoint = Endpoint.publish("http://localhost:9004/fe", new FEServicesImpl(inter));
            System.out.println("FrontEnd Server is Up & Running" + endpoint.isPublished());

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private static int sendUnicastToSequencer(MyRequest requestFromClient) {
        DatagramSocket aSocket = null;
        String dataFromClient = requestFromClient.toString();
        System.out.println("FE:sendUnicastToSequencer>>>" + dataFromClient);
        int sequenceID = 0;
        try {
            aSocket = new DatagramSocket(FE_SQ_PORT);
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName(sequencerIP);
            DatagramPacket requestToSequencer = new DatagramPacket(message, dataFromClient.length(), aHost, sequencerPort);

            aSocket.send(requestToSequencer);

            aSocket.setSoTimeout(1000);

            // Set up a UPD packet for receiving
            byte[] buffer = new byte[1000];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);

            // Try to receive the response from the ping
            aSocket.receive(response);
            String sentence = new String(response.getData(), 0,
                    response.getLength());
            System.out.println("FE:sendUnicastToSequencer/ResponseFromSequencer>>>" + sentence);
            sequenceID = Integer.parseInt(sentence.trim());
            System.out.println("FE:sendUnicastToSequencer/ResponseFromSequencer>>>SequenceID:" + sequenceID);
        } catch (SocketException e) {
            System.out.println("Failed: " + requestFromClient.noRequestSendError());
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Failed: " + requestFromClient.noRequestSendError());
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
        return sequenceID;
    }

    private static void listenForUDPResponses(FEServicesImpl servant) {
        DatagramSocket aSocket = null;
        try {
            InetAddress desiredAddress = InetAddress.getByName(FE_IP_Address);
            aSocket = new DatagramSocket(FE_PORT, desiredAddress);
            byte[] buffer = new byte[1000];
            System.out.println("FE Server Started on " + desiredAddress + ":" + FE_PORT + "............");

            while (true) {
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(response);
                String sentence = new String(response.getData(), 0,
                        response.getLength()).trim();
                System.out.println("FE:Response received from Rm>>>" + sentence);
                RmResponse rmResponse = new RmResponse(sentence);

                System.out.println("Adding response to FrontEndImplementation:");
                servant.addReceivedResponse(rmResponse);
            }

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
//            if (aSocket != null)
//                aSocket.close();
        }
    }
}
