package ReplicaTwo.Database;

public class AppointData {
    public String appointmentType, appointmentID;
    public int bookingCapacity;

    public AppointData(String appointmentType, String appointmentID, int bookingCapacity)
    {
        this.appointmentType = appointmentType;
        this.appointmentID = appointmentID;
        this.bookingCapacity = bookingCapacity;
    }
}
