package com.itcompany.recruitment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.elasticsearch.annotations.*;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Education {
    @Field(type = FieldType.Text)
    private String institution;

    @Field(type = FieldType.Text)
    private String degree;

    @Field(type = FieldType.Text)
    private String fieldOfStudy;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate;

    @Field(type = FieldType.Double)
    private Double gpa;
}
