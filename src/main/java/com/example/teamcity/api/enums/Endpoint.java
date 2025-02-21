package com.example.teamcity.api.enums;

import com.example.teamcity.api.models.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Endpoint {
    BUILD_TYPES("/app/rest/buildTypes", BuildType.class),
    PROJECT("/app/rest/projects", Project.class),
    USERS("/app/rest/users", User.class),
    ROLE_ASSIGNMENTS("/app/rest/roles/id:NEW_ROLE_2/permissions/comment_build",Role.class );



    private final String url;
    private final Class<? extends BaseModel> modelClass;
}