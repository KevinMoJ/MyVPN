package com.androapplite.shadowsocks.model;

/**
 * Created by Kevin.Mo on 2018/6/20.
 */

public class PriorityConfig {
    private String code;
    private String nation;

    public PriorityConfig(String code, String nation) {
        this.code = code;
        this.nation = nation;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }
}
