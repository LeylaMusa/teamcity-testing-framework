package com.example.teamcity.api.spec;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.hc.core5.http.HttpStatus;
import org.hamcrest.Matchers;

public class ValidationResponseSpecifications {
    public static ResponseSpecification checkBuildTypeWithIdAlreadyExist(String buildTypeId) {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(HttpStatus.SC_BAD_REQUEST);
        responseSpecBuilder.expectBody(Matchers.containsString(
                "The build configuration / template ID \"%s\" is already used by another configuration or template"
                        .formatted(buildTypeId)));
        return responseSpecBuilder.build();
    }
}

