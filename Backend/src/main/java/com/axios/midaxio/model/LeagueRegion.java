package com.axios.midaxio.model;

import lombok.Getter;

@Getter
public enum LeagueRegion {
    BR1("br1", "americas"),
    EUN1("eun1", "europe"),
    EUW1("euw1", "europe"),
    JP1("jp1", "asia"),
    KR("kr", "asia"),
    LA1("la1", "americas"),
    LA2("la2", "americas"),
    NA1("na1", "americas"),
    OC1("oc1", "sea"),
    PH2("ph2", "sea"),
    RU("ru", "europe"),
    SG2("sg2", "sea"),
    TH2("th2", "sea"),
    TR1("tr1", "europe"),
    TW2("tw2", "sea"),
    VN2("vn2", "sea");

    private final String id;
    private final String routingValue;

    LeagueRegion(String id, String routingValue) {
        this.id = id;
        this.routingValue = routingValue;
    }

    public String getRegionalHost() {
        return this.routingValue + ".api.riotgames.com";
    }
}