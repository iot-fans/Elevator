package run.aloop.elevator;

import java.util.HashMap;

public interface DAQEvent {
    void OnConnectFailed(int code);
    void OnReadFailed(int code);
    void OnConnected();
    void OnWriteFailed(int code);
    void OnDataReceived(HashMap<String, Object> data);
}
