package ThirdReplica.Database;

public class PatientData {
    public String patientID, appointmentID, appointmentType;

    public PatientData(String patientID, String appointmentType, String appointmentID)
    {
        this.patientID = patientID;
        this.appointmentType = appointmentType;
        this.appointmentID = appointmentID;
    }
}
