package com.itcompany.recruitment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "candidates")
@Setting(settingPath = "elasticsearch-settings.json")
public class Candidate {
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String firstName;

    @Field(type = FieldType.Text)
    private String lastName;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Text)
    private String phone;

    @Field(type = FieldType.Keyword)
    private String location;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String cvContent; // Sadržaj CV-ja kao tekst

    @Field(type = FieldType.Dense_Vector, dims = 768)
    private float[] cvVector; // Vektorizovan CV

    @Field(type = FieldType.Keyword)
    private List<String> skills;

    @Field(type = FieldType.Dense_Vector, dims = 768)
    private float[] skillsVector; // Vektorizovane veštine

    @Field(type = FieldType.Integer)
    private Integer yearsOfExperience;

    @Field(type = FieldType.Nested)
    private List<WorkExperience> workExperiences;

    @Field(type = FieldType.Nested)
    private List<Education> educationHistory;

    @Field(type = FieldType.Keyword)
    private List<String> certifications;

    @Field(type = FieldType.Keyword)
    private String currentPosition;

    @Field(type = FieldType.Double)
    private Double expectedSalary;

    @Field(type = FieldType.Keyword)
    private String preferredEmploymentType;

    @Field(type = FieldType.Boolean)
    private Boolean willingToRelocate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate dateOfBirth;

    @Field(type = FieldType.Keyword)
    private String linkedinProfile;

    @Field(type = FieldType.Keyword)
    private String githubProfile;

    @Field(type = FieldType.Double)
    private Double matchScore; // Skor podudaranja sa oglassom (runtime calculated)

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime registrationDate;
}
