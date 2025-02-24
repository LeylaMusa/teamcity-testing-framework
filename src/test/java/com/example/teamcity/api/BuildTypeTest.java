package com.example.teamcity.api;

import com.example.teamcity.api.enums.Endpoint;
import com.example.teamcity.api.generators.RoleGenerator;
import com.example.teamcity.api.models.BuildType;
import com.example.teamcity.api.models.Project;
import com.example.teamcity.api.models.Role;
import com.example.teamcity.api.models.User;
import com.example.teamcity.api.requests.CheckedRequests;
import com.example.teamcity.api.requests.unchecked.UncheckedBase;
import com.example.teamcity.api.spec.Specifications;
import com.example.teamcity.api.spec.ValidationResponseSpecifications;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.teamcity.api.enums.Endpoint.*;
import static com.example.teamcity.api.generators.TestDataGenerator.generate;
import static io.qameta.allure.Allure.step;

@Test(groups = {"Regression"})
public class BuildTypeTest extends BaseApiTest {
    @Test(description = "User should be able to create build type", groups = {"Positive", "CRUD"})
    public void userCreatesBuildTypeTest() {
        var user = generate(User.class);
        step("Create user", () -> {
            superUserCheckRequests.getRequest(Endpoint.USERS).create(user);
        });

        var project = generate(Project.class);
        AtomicReference<String> projectId = new AtomicReference<>("");

        step("Create project and grant PROJECT_ADMIN role to the user", () -> {
            var userCheckRequests = new CheckedRequests(Specifications.authSpec(user));
            Project createdProject = userCheckRequests.<Project>getRequest(Endpoint.PROJECT).create(project);
            projectId.set(createdProject.getId());

           Role role = RoleGenerator.generateProjectAdmin(projectId.get());
            superUserCheckRequests.getRequest(Endpoint.ROLE_ASSIGNMENTS)
                    .put(String.valueOf(role));
        });

        var buildType = generate(Arrays.asList(project), BuildType.class);
        AtomicReference<String> buildTypeId = new AtomicReference<>("");

        step("Create buildType for project by user (PROJECT_ADMIN)", () -> {
            var userCheckRequests = new CheckedRequests(Specifications.authSpec(user));
            BuildType createdBuildType = (BuildType) userCheckRequests.getRequest(Endpoint.BUILD_TYPES).create(buildType);
            buildTypeId.set(createdBuildType.id);
        });

        step("Check buildType was created successfully", () -> {
            var userCheckRequests = new CheckedRequests(Specifications.authSpec(user));
            var createdBuildType = userCheckRequests.<BuildType>getRequest(Endpoint.BUILD_TYPES).read(buildTypeId.get());
            softy.assertEquals(buildType.getName(), createdBuildType.getName(), "Build type name is not correct");
            softy.assertEquals(createdBuildType.getProject().getId(), projectId.get(), "Build type project ID is not correct");
        });
    }

    @Test(description = "User should not be able to create two build types with the same id", groups = {"Negative", "CRUD"})
    public void userCreatesTwoBuildTypesWithTheSameIdTest() {
        var user = generate(User.class);

        superUserCheckRequests.getRequest(USERS).create(user);
        var userCheckRequests = new CheckedRequests(Specifications.authSpec(user));

        var project = generate(Project.class);

        project = userCheckRequests.<Project>getRequest(PROJECT).create(project);

        var buildType1 = generate(Arrays.asList(project), BuildType.class);
        var buildType2 = generate(Arrays.asList(project), BuildType.class, buildType1.getId());
        userCheckRequests.getRequest(BUILD_TYPES).create(buildType1);
        new UncheckedBase(Specifications.authSpec(user), BUILD_TYPES)
                .create(buildType2)
                .then()
                .spec(ValidationResponseSpecifications.checkBuildTypeWithIdAlreadyExist(buildType1.getId()));  // Используем созданную спецификацию
    }

    @Test(description = "Project admin should be able to create build type for their project", groups = {"Positive", "Roles"})
    public void projectAdminCreatesBuildTypeTest() {
        var user = generate(User.class);
        step("Create user", () -> {
            superUserCheckRequests.getRequest(Endpoint.USERS).create(user);
        });
        var project = generate(Project.class);
        AtomicReference<String> projectId = new AtomicReference<>("");

        step("Create project and grant PROJECT_ADMIN role to the user", () -> {
            var userCheckRequests = new CheckedRequests(Specifications.authSpec(user));
            Project createdProject = userCheckRequests.<Project>getRequest(Endpoint.PROJECT).create(project);
            projectId.set(createdProject.getId());
            String roleId = "NEW_ROLE_2";
            String permissionPath = "permissions/comment_build";
            Role role = Role.builder()
                    .roleId(roleId)
                    .rolesecond("PROJECT_ADMIN")
                    .scope("g")
                    .build();
            superUserCheckRequests.getRequest(Endpoint.ROLE_ASSIGNMENTS)
                    .put(String.valueOf(role));
        });
        var buildType = generate(Arrays.asList(project), BuildType.class);
        AtomicReference<String> buildTypeId = new AtomicReference<>("");

        step("Create buildType for project by user (PROJECT_ADMIN)", () -> {
            var userCheckRequests = new CheckedRequests(Specifications.authSpec(user));
            BuildType createdBuildType = (BuildType) userCheckRequests.getRequest(Endpoint.BUILD_TYPES).create(buildType);
            buildTypeId.set(createdBuildType.id);
        });
        step("Check buildType was created successfully", () -> {
            var userCheckRequests = new CheckedRequests(Specifications.authSpec(user));
            var createdBuildType = userCheckRequests.<BuildType>getRequest(Endpoint.BUILD_TYPES).read(buildTypeId.get());
            softy.assertEquals(buildType.getName(), createdBuildType.getName(), "Build type name is not correct");
            softy.assertEquals(createdBuildType.getProject().getId(), projectId.get(), "Build type project ID is not correct");
        });
    }

    @Test(description = "Verify that users with the PROJECT_ADMIN role can and cannot create build types for projects")
    public void projectAdminRoleTest() {

        var user1 = generate(User.class);
        var project1 = generate(Project.class);
        AtomicReference<String> project1Id = new AtomicReference<>("");

        var user2 = generate(User.class);
        var project2 = generate(Project.class);
        AtomicReference<String> project2Id = new AtomicReference<>("");

        step("Create user1 and user2", () -> {
            superUserCheckRequests.getRequest(Endpoint.USERS).create(user1);
            superUserCheckRequests.getRequest(Endpoint.USERS).create(user2);
        });

        step("Create project1 and project2", () -> {
            var userCheckRequests1 = new CheckedRequests(Specifications.authSpec(user1));
            Project createdProject1 = userCheckRequests1.<Project>getRequest(Endpoint.PROJECT).create(project1);
            project1Id.set(createdProject1.getId());

            var userCheckRequests2 = new CheckedRequests(Specifications.authSpec(user2));
            Project createdProject2 = userCheckRequests2.<Project>getRequest(Endpoint.PROJECT).create(project2);
            project2Id.set(createdProject2.getId());
        });

        step("Grant user1 PROJECT_ADMIN role in project1", () -> {
            Role role1 = RoleGenerator.generateProjectAdmin(project1Id.get());
            superUserCheckRequests.getRequest(Endpoint.ROLE_ASSIGNMENTS).put(String.valueOf(role1));
        });

        step("Grant user2 PROJECT_ADMIN role in project2", () -> {
            Role role2 = RoleGenerator.generateProjectAdmin(project2Id.get());
            superUserCheckRequests.getRequest(Endpoint.ROLE_ASSIGNMENTS).put(String.valueOf(role2));
        });

        var buildType1 = generate(Arrays.asList(project1), BuildType.class);
        var buildType2 = generate(Arrays.asList(project2), BuildType.class);
        AtomicReference<String> buildType1Id = new AtomicReference<>("");
        AtomicReference<String> buildType2Id = new AtomicReference<>("");

        step("Create buildType1 for project1 by user1", () -> {
            var userCheckRequests1 = new CheckedRequests(Specifications.authSpec(user1));
            BuildType createdBuildType1 = (BuildType) userCheckRequests1.getRequest(Endpoint.BUILD_TYPES).create(buildType1);
            buildType1Id.set(createdBuildType1.id);
        });

        step("Create buildType2 for project2 by user2", () -> {
            var userCheckRequests2 = new CheckedRequests(Specifications.authSpec(user2));
            BuildType createdBuildType2 = (BuildType) userCheckRequests2.getRequest(Endpoint.BUILD_TYPES).create(buildType2);
            buildType2Id.set(createdBuildType2.id);
        });

        step("Check buildType1 was created successfully", () -> {
            var userCheckRequests1 = new CheckedRequests(Specifications.authSpec(user1));
            var createdBuildType1 = userCheckRequests1.<BuildType>getRequest(Endpoint.BUILD_TYPES).read(buildType1Id.get());
            softy.assertEquals(buildType1.getName(), createdBuildType1.getName(), "Build type name is not correct");
            softy.assertEquals(createdBuildType1.getProject().getId(), project1Id.get(), "Build type project ID is not correct");
        });

        step("Check buildType2 was created successfully", () -> {
            var userCheckRequests2 = new CheckedRequests(Specifications.authSpec(user2));
            var createdBuildType2 = userCheckRequests2.<BuildType>getRequest(Endpoint.BUILD_TYPES).read(buildType2Id.get());
            softy.assertEquals(buildType2.getName(), createdBuildType2.getName(), "Build type name is not correct");
            softy.assertEquals(createdBuildType2.getProject().getId(), project2Id.get(), "Build type project ID is not correct");
        });
    }
}
