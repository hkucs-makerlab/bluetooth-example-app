package com.makerlab.example.protocol;

import java.io.UnsupportedEncodingException;

/*
https://www.thingiverse.com/thing:3096135
https://marlinfw.org/meta/gcode/

G1 X10 Y10   移動到Ｘ10，Ｙ10坐標
G28 回home點，每次開機后需要先回home點以確定機械臂位置
G90 切換到絕對坐標
G91 切換到相對坐標
G94 切換到直角坐標
G95 切換到到角度坐標

M3 舵機落筆  轉到默認落筆角度
M3 S15 舵機轉到15度
M4 L10 T40  設置默認落筆角度為10 默認抬筆角度為40
M5 舵機抬筆  轉到默認抬筆角度

// not use
M92 X48.8 Y48.8 設置電機分辨率，如果電機步進角是1.8°，16細分直接用出廠設置就行
M203 X2000 Y2000  設置電機最大運動速度
M201 X1000 Y1000 設置電機運動加速度
M205 X0.8 電機運動平滑度 （下面介紹）
M503 查看機械臂設置參數
M501 載入用戶參數（保存到EEPROM的參數）
M502 恢復出廠設置
M500 保存用戶參數（保存到EEPROM）
M370 將當面位置定義為坐標原點
M700 自動標定
 */

public class DrawBotGcode {
    static final String HOME="G28";
    static final String MOVE="G1 ";
    static final String RELATIVE_POS="G91";
    static final String LINE_BREAK="\r\n";

    private int mPosX=0, mPosY=0;
    public DrawBotGcode() {

    }

    public byte[] home() {
        return getPayload(HOME+LINE_BREAK+RELATIVE_POS);
    }

    public byte[] move(int x, int y) {
        return getPayload(MOVE+"X"+x+" Y"+y);
    }

    private byte[] getPayload(String gcode) {
        try {
            gcode+=LINE_BREAK;
            return gcode.getBytes("iso8859-1");
        } catch (UnsupportedEncodingException e) {
           return null;
        }
    }
}
