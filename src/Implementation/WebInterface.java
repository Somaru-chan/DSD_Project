package Implementation;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)

public interface WebInterface {

    //admins add available appointments
    @WebMethod
    public String addAppointment (String appointmentID, String appointmentType, int capacity);

    //admins remove appointments from list of available appointments
    @WebMethod
    public String removeAppointment (String appointmentID, String appointmentType);

    //admins list available appointments
    @WebMethod
    public String listAppointmentAvailability (String appointmentType);

    //patients/admins may add appointments to their schedule
    @WebMethod
    public String bookAppointment (String patientID, String appointmentID, String appointmentType);

    //patients/admins may retrieve the patient's schedule of booked appointments
    @WebMethod
    public String getAppointmentSchedule (String patientID);

    //patients/admins may cancel booked appointments
    @WebMethod
    public String cancelAppointment (String patientID, String appointmentID, String appointmentType);

    //patients may swap old appointment with a new AND available appointment
    @WebMethod
    public String swapAppointment (String patientID, String oldAppointmentID, String oldAppointmentType, String newAppointmentID, String newAppointmentType);

    @WebMethod
    public String shutdown();
}
