package ReplicaThree;

import ReplicaThree.Database.Message;
import ReplicaThree.Servers.MontrealServer;
import ReplicaThree.Servers.QuebecServer;
import ReplicaThree.Servers.SherbrookeServer;
import ReplicaThree.Service.WebInterface;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ThirdRM {
    static Service montrealSer, quebecSer, sherbrookeSer;
    public static int lastSequenceID = 1;
    public static ConcurrentHashMap<Integer, Message> message_list = new ConcurrentHashMap<>();
    public static Queue<Message> message_q = new ConcurrentLinkedQueue<Message>();
    private static boolean serversFlag = true;

    public static void main(String[] args) throws Exception {
        Run();
    }

    private static void Run() throws Exception {
        Runnable task = () -> {
            try {
                receive();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    private static void receive() throws Exception {
        MulticastSocket socket = null;
        try {

            socket = new MulticastSocket(1234);

            socket.joinGroup(InetAddress.getByName("230.1.1.10"));

            byte[] buffer = new byte[1000];
            System.out.println("RM3 UDP Server Started(port=1234)............");

            //Run thread for executing all messages in queue
            Runnable task = () -> {
                try {
                    executeAllRequests();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            Thread thread = new Thread(task);
            thread.start();

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                String data = new String(request.getData(), 0, request.getLength());
                String[] parts = data.split(";");

                /*
                Message Types:
                    00- Simple message
                    01- Sync request between the RMs
                    02- Initialing RM
                    11-RM1 has bug
                    12-RM2 has bug
                    13-Rm3 has bug
                    21-RM1 is down
                    22-RM2 is down
                    23-Rm3 is down
                */
                System.out.println("RM3 received a message. Detail:" + data);
                if (parts[2].equalsIgnoreCase("00")) {
                    Message message = message_obj_create(data);
                    Message message_To_RMs = message_obj_create(data);
                    message_To_RMs.MessageType = "01";
                    send_multicast_toRM(message_To_RMs);
                    if (message.sequenceId - lastSequenceID > 1) {
                        Message initial_message = new Message(0, "Null", "02", Integer.toString(lastSequenceID), Integer.toString(message.sequenceId), "RM3", "Null", "Null", "Null", 0);
                        System.out.println("RM3 send request to update its message list. from:" + lastSequenceID + "To:" + message.sequenceId);
                        // Request all RMs to send back list of messages
                        send_multicast_toRM(initial_message);
                    }
                    System.out.println("is adding queue:" + message);
                    message_q.add(message);
                    message_list.put(message.sequenceId, message);
                } else if (parts[2].equalsIgnoreCase("01")) {
                    Message message = message_obj_create(data);
                    if (!message_list.contains(message.sequenceId))
                        message_list.put(message.sequenceId, message);
                } else if (parts[2].equalsIgnoreCase("02")) {
                    initial_send_list(Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), parts[5]);
                } else if (parts[2].equalsIgnoreCase("03") && parts[5].equalsIgnoreCase("RM3")) {
                    update_message_list(parts[1]);
                } else if (parts[2].equalsIgnoreCase("11")) {
                    Message message = message_obj_create(data);
                    System.out.println("RM3 has bug:" + message.toString());
                } else if (parts[2].equalsIgnoreCase("12")) {
                    Message message = message_obj_create(data);
                    System.out.println("RM3 has bug:" + message.toString());
                } else if (parts[2].equalsIgnoreCase("13")) {
                    Message message = message_obj_create(data);
                    System.out.println("RM3 has bug:" + message.toString());
                } else if (parts[2].equalsIgnoreCase("23")) {
                    Runnable crash_task = () -> {
                        try {
                            //suspend the execution of messages untill all servers are up. (serversFlag=false)
                            serversFlag = false;
                            //reboot Monteal Server
                            URL montrealURL = new URL("http://localhost:6331/montreal?wsdl");
                            QName montrealQName = new QName("http://ReplicaThree/", "ServerImplementationService");
                            montrealSer = Service.create(montrealURL, montrealQName);
                            WebInterface MTL_Object = montrealSer.getPort(WebInterface.class);
                            MTL_Object.shutdown();
                            System.out.println("RM3 shutdown Montreal Server");

                            //reboot Quebec Server
                            URL quebecURL = new URL("http://localhost:6330/quebec?wsdl");
                            QName quebecQName = new QName("http://ReplicaThree/", "ServerImplementationService");
                            quebecSer = Service.create(quebecURL, quebecQName);
                            WebInterface QUE_Object = quebecSer.getPort(WebInterface.class);
                            QUE_Object.shutdown();
                            System.out.println("RM1 shutdown Quebec Server");

                            //reboot Sherbrooke Server
                            URL sherbrookeURL = new URL("http://localhost:6329/sherbrooke?wsdl");
                            QName sherbrookeQName = new QName("http://ReplicaThree/", "ServerImplementationService");
                            sherbrookeSer = Service.create(sherbrookeURL, sherbrookeQName);
                            WebInterface SHE_Object = sherbrookeSer.getPort(WebInterface.class);
                            SHE_Object.shutdown();
                            System.out.println("RM1 shutdown Sherbrooke Server");

                            //running all servers
                            MontrealServer.main(new String[0]);
                            Thread.sleep(500);
                            QuebecServer.main(new String[0]);
                            Thread.sleep(500);
                            SherbrookeServer.main(new String[0]);

                            //wait untill are servers are up
                            Thread.sleep(5000);

                            System.out.println("RM3 is reloading servers hashmap");
                            reloadServers();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                    Thread handleThread = new Thread(crash_task);
                    handleThread.start();
//                    handleThread.join();
                    System.out.println("RM3 handled the crash!");
                    serversFlag = true;
                }
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

    private static Message message_obj_create(String data) {
        String[] parts = data.split(";");
        int sequenceId = Integer.parseInt(parts[0]);
        String FrontIpAddress = parts[1];
        String MessageType = parts[2];
        String Function = parts[3];
        String userID = parts[4];
        String newAppointmentID = parts[5];
        String newAppointmentType = parts[6];
        String oldAppointmentID = parts[7];
        String oldAppointmentType = parts[8];
        int bookingCapacity = Integer.parseInt(parts[9]);
        Message message = new Message(sequenceId, FrontIpAddress, MessageType, Function, userID, newAppointmentID, newAppointmentType, oldAppointmentID, oldAppointmentType, bookingCapacity);
        return message;
    }

    // Create a list of messsages, seperating them with @ and send it back to RM
    private static void initial_send_list(Integer begin, Integer end, String RmNumber) {
        String list = "";
        for (ConcurrentHashMap.Entry<Integer, Message> entry : message_list.entrySet()) {
            if (entry.getValue().sequenceId > begin && entry.getValue().sequenceId < end) {
                list += entry.getValue().toString() + "@";
            }
        }
        // Remove the last @ character
        if (list.length() > 2)
            list.substring(list.length() - 1);
        Message message = new Message(0, list, "03", begin.toString(), end.toString(), RmNumber, "Null", "Null", "Null", 0);
        System.out.println("RM3 sending its list of messages for initialization. list of messages:" + list);
        send_multicast_toRM(message);
    }

    //update the hashmap and new data to be queued for execution
    private static void update_message_list(String data) {
        String[] parts = data.split("@");
        for (int i = 0; i < parts.length; ++i) {
            Message message = message_obj_create(parts[i]);
            //we get the list from 2 other RMs and will ensure that there will be no duplication
            if (!message_list.containsKey(message.sequenceId)) {
                System.out.println("RM3 update its message list" + message);
                message_q.add(message);
                message_list.put(message.sequenceId, message);
            }
        }
    }

    private static void send_multicast_toRM(Message message) {
        int port = 1234;
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            byte[] data = message.toString().getBytes();
            //TODO: replace 230.1.1.10 with another multicast address that works
            InetAddress aHost = InetAddress.getByName("230.1.1.10");
//            InetAddress aHost = InetAddress.getByName("236.22.9.30");

            DatagramPacket request = new DatagramPacket(data, data.length, aHost, port);
            socket.send(request);
            System.out.println("Message multicasted from RM3 to other RMs. Detail:" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Execute all request from the lastSequenceID, send the response back to Front and update the counter(lastSequenceID)
    private static void executeAllRequests() throws Exception {
        System.out.println("before while true");
        while (true) {
            synchronized (ThirdRM.class) {
                Iterator<Message> itr = message_q.iterator();
                while (itr.hasNext()) {
                    Message data = itr.next();
                    System.out.println("RM3 is executing message request. Detail:" + data);
                    //when the servers are down serversFlag is False therefore, no execution untill all servers are up.
                    if (data.sequenceId == lastSequenceID && serversFlag) {
                        System.out.println("RM3 is executing message request. Detail:" + data);
                        requestToServers(data);
                        Message message = new Message(data.sequenceId, "Null", "RM3",
                                data.Function, data.userID, data.newAppointmentID,
                                data.newAppointmentType, data.oldAppointmentID,
                                data.oldAppointmentType, data.bookingCapacity);
                        lastSequenceID += 1;
                        messsageToFront(message.toString(), data.FrontIpAddress);
                        message_q.poll();
                    }
                }
            }
        }
    }

    //Send Web Services  request to server
    private static String requestToServers(Message input) throws Exception {
        int portNumber = serverPort(input.userID.substring(0, 3));
        String serverBranch = " ";
        Service service;
        WebInterface serviceInterface;

        switch(portNumber) {
            case 6331:
                serverBranch = "montreal";
                break;
            case 6330:
                serverBranch = "quebec";
                break;

            case 6329:
                serverBranch = "sherbrooke";
                break;

            default:
                break;
        }

        URL serverURL = new URL("http://localhost:" + portNumber + "/" + serverBranch + "?wsdl");
        QName serverQName = new QName("http://Service/", "ServerImplementationService");
        service = Service.create(serverURL, serverQName);
        serviceInterface = service.getPort(WebInterface.class);

        if (input.userID.substring(3, 4).equalsIgnoreCase("A")) {
            if (input.Function.equalsIgnoreCase("addAppointment")) {
                String response = serviceInterface.addAppointment(input.newAppointmentID, input.newAppointmentType, input.bookingCapacity);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("removeAppointment")) {
                String response = serviceInterface.removeAppointment(input.newAppointmentID, input.newAppointmentType);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("listAppointmentAvailability")) {
                String response = serviceInterface.listAppointmentAvailability(input.newAppointmentType);
                System.out.println(response);
                return response;
            }
        } else if (input.userID.substring(3, 4).equalsIgnoreCase("P")) {
            if (input.Function.equalsIgnoreCase("bookAppointment")) {
                String response = serviceInterface.bookAppointment(input.userID, input.newAppointmentID, input.newAppointmentType);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("getAppointmentSchedule")) {
                String response = serviceInterface.getAppointmentSchedule(input.userID);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("cancelAppointment")) {
                String response = serviceInterface.cancelAppointment(input.userID, input.newAppointmentID, input.newAppointmentType);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("swapAppointment")) {
                String response = serviceInterface.swapAppointment(input.userID, input.newAppointmentID, input.newAppointmentType, input.oldAppointmentID, input.oldAppointmentType);
                System.out.println(response);
                return response;
            }
        }
        return "Null response from server" + input.userID.substring(0, 3);
    }

    private static int serverPort(String input) {
        String branch = input.substring(0, 3);
        int portNumber = -1;

        if (branch.equalsIgnoreCase("MTL"))
            portNumber = 6331;
        else if (branch.equalsIgnoreCase("QUE"))
            portNumber = 6330;
        else if (branch.equalsIgnoreCase("SHE"))
            portNumber = 6329;

        return portNumber;
    }

    public static void messsageToFront(String message, String FrontIpAddress) {
        System.out.println("Message to front:" + message);
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(4322);
            byte[] bytes = message.getBytes();
            InetAddress aHost = InetAddress.getByName(FrontIpAddress);

            System.out.println(aHost);
            DatagramPacket request = new DatagramPacket(bytes, bytes.length, aHost, 1999);
            socket.send(request);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

    }

    public static void reloadServers() throws Exception {
        for (ConcurrentHashMap.Entry<Integer, Message> entry : message_list.entrySet()) {
//            System.out.println("Recovery Mode-RM3 is executing message request. Detail:" + entry.getValue().toString());
//            requestToServers(entry.getValue());
            if (entry.getValue().sequenceId < lastSequenceID)
                requestToServers(entry.getValue());
        }
//        message_q.clear();
    }
}
