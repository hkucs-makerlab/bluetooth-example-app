package com.makerlab.example.protocol;

import java.io.UnsupportedEncodingException;

public class PlainTextProtocol {
    static final String messages[]={
            "button released",
            "button forward pressed",
            "button right pressed",
            "button backward pressed",
            "button left pressed",
            "button center pressed"
    };

    public byte[] getPayload(int i) {
        byte[] payload=null;
        if (i < messages.length && i>=0) {
            try {
                payload = messages[i].getBytes("iso8859-1");
            } catch (UnsupportedEncodingException e) {}
        }
        return payload;
    };
}
