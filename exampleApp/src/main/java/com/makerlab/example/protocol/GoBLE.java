package com.makerlab.example.protocol;

import android.util.Log;

public class GoBLE {
    private boolean mRepeat = true;
    private int mPrevChecksum = -1;

    public GoBLE() {
    }

    public void setRepeat(boolean flag) {
        mRepeat = flag;
    }


    public byte[] getPayload(int xPos, int yPos, byte[] mButtonPressed) {
        byte[] payload = {
                0x55, // header 1
                (byte) 0xAA, // header 2
                0x11, // address, no effect!
                0x00, // no.of button pressed?
                0x03, // rocker position, any value, doesn't matter
                0x00, // joystick y
                0x00, // joystick x
                0x00, // yPos2 not use
                0x00, // xPos2 not use
                0x00  // checksum
        };
        int payloadIndex = 0;
        int size = 0;
        if (mButtonPressed != null) {
            size = (byte) mButtonPressed.length;
        }
        if (size > 0) { // if button pressed, data len will be dynamic
            Log.e("buttons",String.valueOf(mButtonPressed.length));
            payload = new byte[size + payload.length];
            //
            payload[payloadIndex++] = 0x55;
            payload[payloadIndex++] = (byte) 0xAA;
            payload[payloadIndex++] = 0x11;
            payload[payloadIndex++] = (byte) size; // no. of button pressed?
            payload[payloadIndex++] = 0x03;      // rocker position, any value
            for (int i = 0; i < mButtonPressed.length; i++) {
                payload[payloadIndex++] = mButtonPressed[i];
            }
            payload[payloadIndex++] = (byte) yPos; // joystick y
            payload[payloadIndex++] = (byte) xPos; // joystick x
            payload[payloadIndex++] = 0; // yPos2 not use
            payload[payloadIndex++] = 0; // xPos2 not use
        } else {
            payload[payloadIndex++] = 0x55;
            payload[payloadIndex++] = (byte) 0xAA;
            payload[payloadIndex++] = 0x11;
            payload[payloadIndex++] = 0; // is a button pressed? no
            payload[payloadIndex++] = 0x03; // rocker position
            payload[payloadIndex++] = (byte) yPos; // joystick y
            payload[payloadIndex++] = (byte) xPos; // joystick x
            payload[payloadIndex++] = 0; // yPos2 not use
            payload[payloadIndex++] = 0; // xPos2 not use
        }
        //
        int checksum = 0;
        for (int i = 0; i < payload.length; i++) {
            checksum += payload[i];
        }
        checksum = (checksum % 256);
        payload[payloadIndex] = (byte) checksum; // checksum
        return payload;
    }

}
