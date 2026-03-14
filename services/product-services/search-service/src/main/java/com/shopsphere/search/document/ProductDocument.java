package com.shopsphere.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.math.BigDecimal;

@Document(indexName="products")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductDocument {

 @Id
 private String id;

 @Field(type=FieldType.Text, analyzer="standard")
 private String name;

 @Field(type=FieldType.Text, analyzer="standard")
 private String description;

 @Field(type=FieldType.Scaled_Float, scalingFactor=100)
 private BigDecimal price;

 @Field(type=FieldType.Keyword)
 private String categoryId;

 @Field(type=FieldType.Keyword)
 private String categoryName;

 @Field(type=FieldType.Keyword)
 private String vendorId;

 @Field(type=FieldType.Keyword)
 private String status;

 @Field(type=FieldType.Keyword)
 private String primaryImageUrl;
}
