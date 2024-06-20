package com.task.guessnumber.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class Bet {

    @JsonProperty
    private String name;

    @JsonProperty
    private Integer number;

    @JsonProperty
    private Integer betAmount;
}
