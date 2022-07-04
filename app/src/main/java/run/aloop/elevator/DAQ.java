package run.aloop.elevator;

import java.util.HashMap;

import Moka7.S7;
import Moka7.S7Client;

public class DAQ extends Thread {

    private DAQEvent receiver;
    private DataStruct[] ds;
    private boolean enable;

    private String address = "";
    private int localTSAP = 0x1000, remoteTSAP = 0x1000;
    private boolean useTSAP = true;
    private int rack = 0, slot = 0;
    private boolean reconnect = false;
    private S7Client client = new S7Client();

    public DAQ(DataStruct[] ds, DAQEvent dataReceived) {
        this.receiver = dataReceived;
        this.ds = ds;
        this.enable = true;
    }

    @Override
    public void run() {
        while (enable) {
            if (reconnect) {
                client.Disconnect();
                reconnect = false;
            }
            if (!client.Connected && !this.address.isEmpty()) {
                int code;
                if (useTSAP) {
                    client.SetConnectionParams(this.address, this.localTSAP, this.remoteTSAP);
                    code = client.Connect();
                } else {
                    client.SetConnectionType(S7.OP);
                    code = client.ConnectTo(this.address, this.rack, this.slot);
                }
                if (code != 0) {
                    if (receiver != null)
                        this.receiver.OnConnectFailed(code);
                    try {
                        sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
            }
            if (client.Connected && receiver != null) {
                this.receiver.OnConnected();
                this.receiver.OnDataReceived(readAll(client));
            }
            try {
                sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private HashMap<String, Object> readAll(S7Client client) {
        HashMap<String, Object> values = new HashMap<>();
        for (DataStruct struct: this.ds) {
            byte[] data = new byte[struct.length()];
            int code = client.ReadArea(struct.area, struct.db, struct.start, struct.length(), data);
            if (code == 0)
                values.putAll(struct.unpack(data));
            else if (this.receiver != null)
                this.receiver.OnReadFailed(code);
        }
        return values;
    }

    public boolean isConnected() {
        return client.Connected;
    }

    public void setParams(String address, boolean useTSAP, int localTSAP, int remoteTSAP, int rack, int slot) {
        boolean update = false;
        if (!address.equals(this.address)) {
            this.address = address;
            update = true;
        }
        if (localTSAP != this.localTSAP) {
            this.localTSAP = localTSAP;
            update = true;
        }
        if (remoteTSAP != this.remoteTSAP) {
            this.remoteTSAP = remoteTSAP;
            update = true;
        }
        if (rack != this.rack) {
            this.rack = rack;
            update = true;
        }
        if (slot != this.slot) {
            this.slot = slot;
            update = true;
        }
        if (useTSAP != this.useTSAP) {
            this.useTSAP = useTSAP;
            update = true;
        }
        if (update) {
            reconnect = true;
        }
    }

    public String getAddress() {
        return address;
    }

    public void write(final String name, final Object value) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                S7Client client = new S7Client();
                int code;
                if (useTSAP) {
                    client.SetConnectionParams(address, localTSAP, remoteTSAP);
                    code = client.Connect();
                } else {
                    client.SetConnectionType(S7.OP);
                    code = client.ConnectTo(address, rack, slot);
                }
                if (code == 0) {
                    _write(client, name, value);
                    client.Disconnect();
                }
                else if (receiver != null)
                    receiver.OnConnectFailed(code);
            }
        }).start();
    }

    private void _write(S7Client client, String name, Object value) {
        if (!this.enable)
            return;
        for (DataStruct ds : this.ds) {
            int offset = ds.getOffset(name);
            if (offset < 0)
                continue;
            DataStruct.Variable v = ds.getVariable(name);
            if (v == null)
                continue;
            byte[] data = new byte[v.size];
            switch (v.type) {
                case INT:
                    assert value instanceof Integer;
                    S7.SetDIntAt(data, 0, (Integer) value);
                    break;
                case SHORT:
                    assert value instanceof Integer;
                    S7.SetShortAt(data, 0, (Integer) value);
                    break;
                case FLOAT:
                    assert value instanceof Float;
                    S7.SetFloatAt(data, 0, (Float) value);
                    break;
                case BOOL:
                    assert value instanceof Boolean;
                    S7.SetBitAt(data, 0, 0, (Boolean) value);
            }
            int code = client.WriteArea(ds.area, ds.db, ds.start + offset, v.size, data);
            if (code != 0 && this.receiver != null)
                this.receiver.OnWriteFailed(code);
        }
    }

    public void close() {
        this.enable = false;
        this.client.Disconnect();
    }
}
