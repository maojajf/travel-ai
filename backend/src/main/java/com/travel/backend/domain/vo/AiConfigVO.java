package com.travel.backend.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Mao
 * @time 2022-05-20
 */
@Data
public class AiConfigVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String baseurl;
    private String apikey;
    private String model;
}
