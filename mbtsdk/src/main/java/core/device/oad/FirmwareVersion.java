package core.device.oad;


public enum FirmwareVersion {

    V1_7_0,
    V1_7_1,
    V1_7_4,
    V1_7_6;

    public String getFirmwareVersionAsString(){
        return this.name().replace("V","");
    }
}
