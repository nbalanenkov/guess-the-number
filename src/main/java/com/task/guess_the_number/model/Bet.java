package com.task.guess_the_number.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Bet {

    public Bet() {
    }

    @JsonProperty()
    private String name;

    @JsonProperty
    private Integer number;

    @JsonProperty
    private Integer betAmount;

    public String getName() {
        return name;
    }

    public Integer getNumber() {
        return number;
    }

    public Integer getBetAmount() {
        return betAmount;
    }
}
