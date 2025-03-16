package com.example.teamcity.api.generators;
import com.example.teamcity.api.models.Role;

public class RoleGenerator {
    public static Role generateProjectAdmin(String projectId) {
        return Role.builder()
                .roleId("NEW_ROLE_2")
                .rolesecond("PROJECT_ADMIN")
                .scope("g")
                .build();
    }
}
