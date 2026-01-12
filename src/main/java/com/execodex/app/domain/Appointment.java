package com.execodex.app.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("appointment")
public class Appointment {
    @Id
    private Long id;
    private String title;
}
