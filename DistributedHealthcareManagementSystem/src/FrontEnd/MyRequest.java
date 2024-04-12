package FrontEnd;


public class MyRequest {
    private String function = "null";
    private String patientID = "null";
    private String appointmentType = "null";
    private String OldAppointmentType = "null";
    private String appointmentID = "null";
    private String OldAppointmentID = "null";
    private String FeIpAddress = FrontEnd.FE_IP_Address;
    private int bookingCapacity = 0;
    private int sequenceNumber = 0;
    private String MessageType = "00";
    private int retryCount = 1;

    public MyRequest(String function, String patientID) {
        setFunction(function);
        setPatientID(patientID);
    }

    public MyRequest(int rmNumber, String bugType) {
        setMessageType(bugType + rmNumber);
    }

    public String noRequestSendError() {
        return "request: " + getFunction() + " from " + getPatientID() + " not sent";
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public String getAppointmentType() {
        return appointmentType;
    }

    public void setAppointmentType(String appointmentType) {
        this.appointmentType = appointmentType;
    }

    public String getOldAppointmentType() {
        return OldAppointmentType;
    }

    public void setOldAppointmentType(String OldAppointmentType) {
        this.OldAppointmentType = OldAppointmentType;
    }

    public String getAppointmentID() {
        return appointmentID;
    }

    public void setAppointmentID(String appointmentID) {
        this.appointmentID = appointmentID;
    }

    public String getOldAppointmentID() {
        return OldAppointmentID;
    }

    public void setOldAppointmentID(String OldAppointmentID) {
        this.OldAppointmentID = OldAppointmentID;
    }

    public int getBookingCapacity() {
        return bookingCapacity;
    }

    public void setBookingCapacity(int bookingCapacity) {
        this.bookingCapacity = bookingCapacity;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getFeIpAddress() {
        return FeIpAddress;
    }

    public void setFeIpAddress(String feIpAddress) {
        FeIpAddress = feIpAddress;
    }

    public String getMessageType() {
        return MessageType;
    }

    public void setMessageType(String messageType) {
        MessageType = messageType;
    }

    public boolean haveRetries() {
        return retryCount > 0;
    }

    public void countRetry() {
        retryCount--;
    }

    @Override
    public String toString() {
        return getSequenceNumber() + ";" +
                getFeIpAddress().toUpperCase() + ";" +
                getMessageType().toUpperCase() + ";" +
                getFunction().toUpperCase() + ";" +
                getPatientID().toUpperCase() + ";" +
                getAppointmentID().toUpperCase() + ";" +
                getAppointmentType().toUpperCase() + ";" +
                getOldAppointmentID().toUpperCase() + ";" +
                getOldAppointmentType().toUpperCase() + ";" +
                getBookingCapacity();
    }
}
