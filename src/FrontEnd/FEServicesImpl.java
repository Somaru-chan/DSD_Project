package FrontEnd;

import javax.jws.WebService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@WebService(endpointInterface = "FrontEnd.IFEServices")
public class FEServicesImpl implements IFEServices {
    private static long DYNAMIC_TIMEOUT = 10000;
    private static int Rm1BugCount = 0;
    private static int Rm2BugCount = 0;
    private static int Rm3BugCount = 0;
    private static int Rm1NoResponseCount = 0;
    private static int Rm2NoResponseCount = 0;
    private static int Rm3NoResponseCount = 0;
    private long responseTime = DYNAMIC_TIMEOUT;
    private long startTime;
    private CountDownLatch latch;
    private IFrontEnd inter;
    private final List<RmResponse> responses = new ArrayList<>();

    public FEServicesImpl(IFrontEnd inter) {
        super();
        this.inter = inter;
    }

    @Override
    public synchronized String addAppointment(String adminID, String appointmentID, String appointmentType, Integer capacity) {
        MyRequest myRequest = new MyRequest("addAppointment", adminID);
        myRequest.setAppointmentID(appointmentID);
        myRequest.setAppointmentType(appointmentType);
        myRequest.setBookingCapacity(capacity);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE Implementation:addAppointment>>>" + myRequest.toString());
        return validateResponses(myRequest);
    }

    @Override
    public synchronized String removeAppointment(String adminID, String appointmentID, String appointmentType) {
        MyRequest myRequest = new MyRequest("removeAppointment", adminID);
        myRequest.setAppointmentID(appointmentID);
        myRequest.setAppointmentType(appointmentType);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE Implementation:removeAppointment>>>" + myRequest.toString());
        return validateResponses(myRequest);
    }

    @Override
    public synchronized String listAppointmentAvailability(String adminID, String appointmentType) {
        MyRequest myRequest = new MyRequest("listAppointmentAvailability", adminID);
        myRequest.setAppointmentType(appointmentType);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE Implementation:listAppointmentAvailability>>>" + myRequest.toString());
        return validateResponses(myRequest);
    }

    @Override
    public synchronized String bookAppointment(String patientID, String appointmentID, String appointmentType) {
        MyRequest myRequest = new MyRequest("bookAppointment", patientID);
        myRequest.setAppointmentID(appointmentID);
        myRequest.setAppointmentType(appointmentType);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE Implementation:bookAppointment>>>" + myRequest.toString());
        return validateResponses(myRequest);
    }

    @Override
    public synchronized String getAppointmentSchedule(String patientID) {
        MyRequest myRequest = new MyRequest("getAppointmentSchedule", patientID);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE Implementation:getAppointmentSchedule>>>" + myRequest.toString());
        return validateResponses(myRequest);
    }

    @Override
    public synchronized String cancelAppointment(String patientID, String appointmentID, String appointmentType) {
        MyRequest myRequest = new MyRequest("cancelAppointment", patientID);
        myRequest.setAppointmentID(appointmentID);
        myRequest.setAppointmentType(appointmentType);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE Implementation:cancelAppointment>>>" + myRequest.toString());
        return validateResponses(myRequest);
    }

    @Override
    public synchronized String swapAppointment(String patientID, String oldAppointmentID, String oldAppointmentType, String newAppointmentID, String newAppointmentType) {
        MyRequest myRequest = new MyRequest("swapAppointment", patientID);
        myRequest.setAppointmentID(newAppointmentID);
        myRequest.setAppointmentType(newAppointmentType);
        myRequest.setOldAppointmentID(oldAppointmentID);
        myRequest.setOldAppointmentType(oldAppointmentType);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE Implementation:swapAppointment>>>" + myRequest.toString());
        return validateResponses(myRequest);
    }

    @Override
    public void shutdown(){
        String status;
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
//        status = "Shutting down";
//        return status;
    }

    private int sendUdpUnicastToSequencer(MyRequest myRequest) {
        startTime = System.nanoTime();
        int sequenceNumber = inter.sendRequestToSequencer(myRequest);
        myRequest.setSequenceNumber(sequenceNumber);
        latch = new CountDownLatch(3);
        waitForResponse();
        return sequenceNumber;
    }

    public void waitForResponse() {
        try {
            System.out.println("FE Implementation:waitForResponse>>>ResponsesRemain" + latch.getCount());
            boolean timeoutReached = latch.await(DYNAMIC_TIMEOUT, TimeUnit.MILLISECONDS);
            if (timeoutReached) {
                setDynamicTimout();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setDynamicTimout() {
        if (responseTime < 4000) {
            DYNAMIC_TIMEOUT = (DYNAMIC_TIMEOUT + (responseTime * 3)) / 2;
        } else {
            DYNAMIC_TIMEOUT = 10000;
        }
        System.out.println("FE Implementation:setDynamicTimout>>>" + DYNAMIC_TIMEOUT);
    }

    private String validateResponses(MyRequest myRequest) {
        String resp;
        switch ((int) latch.getCount()) {
            case 0:
            case 1:
            case 2:
                resp = findMajorityResponse(myRequest);
                break;
            case 3:
                resp = "Fail: No response from any server";
                System.out.println(resp);
                if (myRequest.haveRetries()) {
                    myRequest.countRetry();
                    resp = retryRequest(myRequest);
                }
                rmDown(1);
                rmDown(2);
                rmDown(3);
                break;
            default:
                resp = "Fail: " + myRequest.noRequestSendError();
                break;
        }
        System.out.println("FE Implementation:validateResponses>>>Responses remain:" + latch.getCount() + " >>>Response to be sent to client " + resp);
        return resp;
    }

    private String retryRequest(MyRequest myRequest) {
        System.out.println("FE Implementation:retryRequest>>>" + myRequest.toString());
        startTime = System.nanoTime();
        inter.retryRequest(myRequest);
        latch = new CountDownLatch(3);
        waitForResponse();
        return validateResponses(myRequest);
    }

    private String findMajorityResponse(MyRequest myRequest) {
        RmResponse res1 = null;
        RmResponse res2 = null;
        RmResponse res3 = null;
        for (RmResponse response : responses) {
            if (response.getSequenceID() == myRequest.getSequenceNumber()) {
                switch (response.getRmNumber()) {
                    case 1:
                        res1 = response;
                        break;
                    case 2:
                        res2 = response;
                        break;
                    case 3:
                        res3 = response;
                        break;
                }
            }
        }
        System.out.println("FE Implementation:findMajorityResponse>>>RM1" + ((res1 != null) ? res1.getResponse() : "null"));
        System.out.println("FE Implementation:findMajorityResponse>>>RM2" + ((res2 != null) ? res2.getResponse() : "null"));
        System.out.println("FE Implementation:findMajorityResponse>>>RM3" + ((res3 != null) ? res3.getResponse() : "null"));
        if (res1 == null) {
            rmDown(1);
        } else {
            Rm1NoResponseCount = 0;
            if (res1.equals(res2)) {
                if (!res1.equals(res3) && res3 != null) {
                    rmBugFound(3);
                }
                return res2.getResponse();
            } else if (res1.equals(res3)) {
                if (!res1.equals(res2) && res2 != null) {
                    rmBugFound(2);
                }
                return res1.getResponse();
            } else {
//                if (res2 != null && res2.equals(res3)) {
                if (res2 == null && res3 == null) {
                    return res1.getResponse();
                } else {
//                    rmBugFound(1);
                }
//                    return res2.getResponse();
//                }
            }
        }
        if (res2 == null) {
            rmDown(2);
        } else {
            Rm2NoResponseCount = 0;
            if (res2.equals(res3)) {
                if (!res2.equals(res1) && res1 != null) {
                    rmBugFound(1);
                }
                return res2.getResponse();
            } else if (res2.equals(res1)) {
                if (!res2.equals(res3) && res3 != null) {
                    rmBugFound(3);
                }
                return res2.getResponse();
            } else {
//                if (!res1.equals("null") && res1.equals(res3)) {
                if (res1 == null && res3 == null) {
                    return res2.getResponse();
                } else {
//                    rmBugFound(2);
                }
//                }
//                return res1;
            }
        }
        if (res3 == null) {
            rmDown(3);
        } else {
            Rm3NoResponseCount = 0;
            if (res3.equals(res2)) {
                if (!res3.equals(res1) && res1 != null) {
                    rmBugFound(1);
                }
                return res2.getResponse();
            } else if (res3.equals(res1) && res2 != null) {
                if (!res3.equals(res2)) {
                    rmBugFound(2);
                }
                return res3.getResponse();
            } else {
//                if (!res2.equals("null") && res2.equals(res1)) {
                if (res1 == null && res2 == null) {
                    return res3.getResponse();
                } else {
//                    rmBugFound(3);
                }
//                }
//                return res1;
            }
        }
        return "Fail: majority response not found";
    }

    private void rmBugFound(int rmNumber) {
        switch (rmNumber) {
            case 1:
                Rm1BugCount++;
                if (Rm1BugCount == 3) {
                    Rm1BugCount = 0;
                    inter.informRmHasBug(rmNumber);
                }
                break;
            case 2:
                Rm2BugCount++;
                if (Rm2BugCount == 3) {
                    Rm2BugCount = 0;
                    inter.informRmHasBug(rmNumber);
                }
                break;

            case 3:
                Rm3BugCount++;
                if (Rm3BugCount == 3) {
                    Rm3BugCount = 0;
                    inter.informRmHasBug(rmNumber);
                }
                break;
        }
        System.out.println("FE Implementation:rmBugFound>>>RM1 - bugs:" + Rm1BugCount);
        System.out.println("FE Implementation:rmBugFound>>>RM2 - bugs:" + Rm2BugCount);
        System.out.println("FE Implementation:rmBugFound>>>RM3 - bugs:" + Rm3BugCount);
    }

    private void rmDown(int rmNumber) {
        DYNAMIC_TIMEOUT = 10000;
        switch (rmNumber) {
            case 1:
                Rm1NoResponseCount++;
                if (Rm1NoResponseCount == 3) {
                    Rm1NoResponseCount = 0;
                    inter.informRmIsDown(rmNumber);
                }
                break;
            case 2:
                Rm2NoResponseCount++;
                if (Rm2NoResponseCount == 3) {
                    Rm2NoResponseCount = 0;
                    inter.informRmIsDown(rmNumber);
                }
                break;

            case 3:
                Rm3NoResponseCount++;
                if (Rm3NoResponseCount == 3) {
                    Rm3NoResponseCount = 0;
                    inter.informRmIsDown(rmNumber);
                }
                break;
        }
        System.out.println("FE Implementation:rmDown>>>RM1 - noResponse:" + Rm1NoResponseCount);
        System.out.println("FE Implementation:rmDown>>>RM2 - noResponse:" + Rm2NoResponseCount);
        System.out.println("FE Implementation:rmDown>>>RM3 - noResponse:" + Rm3NoResponseCount);
    }

    public void addReceivedResponse(RmResponse res) {
        long endTime = System.nanoTime();
        responseTime = (endTime - startTime) / 1000000;
        System.out.println("Current Response time is: " + responseTime);
        responses.add(res);
        notifyOKCommandReceived();
    }

    private void notifyOKCommandReceived() {
        latch.countDown();
        System.out.println("FE Implementation:notifyOKCommandReceived>>>Response Received: Remaining responses" + latch.getCount());
    }
}
