package com.itcompany.recruitment.model;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.elasticsearch.annotations.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkExperience {
    @Field(type = FieldType.Text)
    private String company;

    @Field(type = FieldType.Text)
    private String position;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> technologies;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate;

    @Field(type = FieldType.Boolean)
    private Boolean isCurrent;
}
