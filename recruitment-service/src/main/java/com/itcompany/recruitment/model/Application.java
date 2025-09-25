package com.itcompany.recruitment.model;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "applications")
public class Application {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String candidateId;

    @Field(type = FieldType.Keyword)
    private String jobPostingId;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime applicationDate;

    @Field(type = FieldType.Keyword)
    private String status; // PENDING, REVIEWED, SHORTLISTED, REJECTED, ACCEPTED

    @Field(type = FieldType.Text)
    private String coverLetter;

    @Field(type = FieldType.Dense_Vector, dims = 768)
    private float[] coverLetterVector;

    @Field(type = FieldType.Double)
    private Double overallMatchScore; // Ukupni skor podudaranja

    @Field(type = FieldType.Double)
    private Double skillMatchScore; // Skor podudaranja vestina

    @Field(type = FieldType.Double)
    private Double experienceMatchScore; // Skor podudaranja iskustva

    @Field(type = FieldType.Double)
    private Double cvMatchScore; // Skor podudaranja CV-ja sa opisom posla

    @Field(type = FieldType.Integer)
    private Integer ranking; // Pozicija u rangiranju

    @Field(type = FieldType.Text)
    private String hrNotes;

    @Field(type = FieldType.Keyword)
    private String reviewedBy; // HR Manager(njegov Id)

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime reviewDate;

    @Field(type = FieldType.Boolean)
    private Boolean isShortlisted;
}
