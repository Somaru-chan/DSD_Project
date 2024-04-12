package FrontEnd;

public interface IFrontEnd {
    void informRmHasBug(int RmNumber);

    void informRmIsDown(int RmNumber);

    int sendRequestToSequencer(MyRequest myRequest);

    void retryRequest(MyRequest myRequest);
}
