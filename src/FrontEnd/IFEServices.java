package FrontEnd;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface IFEServices {
    @WebMethod
    String addAppointment(String userID, String appointmentID, String appointmentType, Integer capacity);

    @WebMethod
    String removeAppointment(String userID, String appointmentID, String appointmentType);

    @WebMethod
    String listAppointmentAvailability(String userID, String appointmentType);

    @WebMethod
    String bookAppointment(String patientID, String appointmentID, String appointmentType);

    @WebMethod
    String getAppointmentSchedule(String patientID);

    @WebMethod
    String cancelAppointment(String patientID, String appointmentID, String appointmentType);

    @WebMethod
    String swapAppointment(String patientID, String oldAppointmentID, String oldAppointmentType, String newAppointmentID, String newAppointmentType);

    @WebMethod
    void shutdown();
}
