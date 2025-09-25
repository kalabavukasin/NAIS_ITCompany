package com.itcompany.recruitment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "job_postings")
@Setting(settingPath = "elasticsearch-settings.json")
public class JobPosting {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Dense_Vector, dims = 768)
    private float[] descriptionVector; // Vektorizovan opis posla

    @Field(type = FieldType.Keyword)
    private String department;

    @Field(type = FieldType.Keyword)
    private String location;

    @Field(type = FieldType.Keyword)
    private String experienceLevel; // Medior, Senior ...

    @Field(type = FieldType.Integer)
    private Integer minYearsExperience;

    @Field(type = FieldType.Integer)
    private Integer maxYearsExperience;

    @Field(type = FieldType.Keyword)
    private List<String> requiredSkills;

    @Field(type = FieldType.Keyword)
    private List<String> preferredSkills;

    @Field(type = FieldType.Double)
    private Double minSalary;

    @Field(type = FieldType.Double)
    private Double maxSalary;

    @Field(type = FieldType.Keyword)
    private String employmentType; //full-time, part-time i druge opcije...

    @Field(type = FieldType.Boolean)
    private Boolean isActive;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime postedDate;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime applicationDeadline;

    @Field(type = FieldType.Keyword)
    private String hrManagerId;

    @Field(type = FieldType.Integer)
    private Integer numberOfPositions;
}
