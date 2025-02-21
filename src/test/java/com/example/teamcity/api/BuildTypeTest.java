package com.example.teamcity.api;

import com.example.teamcity.api.enums.Endpoint;
import com.example.teamcity.api.models.BuildType;
import com.example.teamcity.api.models.Project;
import com.example.teamcity.api.models.Role;
import com.example.teamcity.api.models.User;
import com.example.teamcity.api.requests.CheckedRequests;
import com.example.teamcity.api.requests.unchecked.UncheckedBase;
import com.example.teamcity.api.spec.Specifications;
import org.apache.hc.core5.http.HttpStatus;
import org.hamcrest.Matchers;
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

        superUserCheckRequests.getRequest(USERS).create(user);
        var userCheckRequests = new CheckedRequests(Specifications.authSpec(user));

        var project = generate(Project.class);

        project = userCheckRequests.<Project>getRequest(PROJECT).create(project);

        var buildType = generate(Arrays.asList(project), BuildType.class);

        userCheckRequests.getRequest(BUILD_TYPES).create(buildType);

        var createdBuildType = userCheckRequests.<BuildType>getRequest(BUILD_TYPES).read(buildType.getId());

        softy.assertEquals(buildType.getName(), createdBuildType.getName(), "Build type name is not correct");
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

        var response = new UncheckedBase(Specifications.authSpec(user), BUILD_TYPES)
                .create(buildType2);
        response.then().assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.containsString("The build configuration / template ID \"%s\" is already used by another configuration or template".formatted(buildType1.getId())));
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
        step("Create user1", () -> {
            superUserCheckRequests.getRequest(Endpoint.USERS).create(user1);
        });
        var project1 = generate(Project.class);
        AtomicReference<String> project1Id = new AtomicReference<>("");
        step("Create project1", () -> {
            var userCheckRequests1 = new CheckedRequests(Specifications.authSpec(user1));
            Project createdProject1 = userCheckRequests1.<Project>getRequest(Endpoint.PROJECT).create(project1);
            project1Id.set(createdProject1.getId()); // Set project1's ID
        });
        step("Grant user1 PROJECT_ADMIN role in project1", () -> {
            String roleId = "NEW_ROLE_2";
            String permissionPath = "permissions/comment_build";

            Role role = Role.builder()
                    .roleId(roleId)
                    .rolesecond("PROJECT_ADMIN")
                    .scope("g")
                    .build();

            var userCheckRequests1 = new CheckedRequests(Specifications.authSpec(user1));
            String requestUrl = String.format("/app/rest/roles/id:%s/%s", roleId, permissionPath);
            userCheckRequests1.getRequest(ROLE_ASSIGNMENTS).put(String.valueOf(role)); // Assuming you have such a method to make the PUT request
        });
        var user2 = generate(User.class);
        step("Create user2", () -> {
            superUserCheckRequests.getRequest(Endpoint.USERS).create(user2);
        });

        var project2 = generate(Project.class);
        AtomicReference<String> project2Id = new AtomicReference<>("");
        step("Create project2", () -> {
            var userCheckRequests2 = new CheckedRequests(Specifications.authSpec(user2));
            Project createdProject2 = userCheckRequests2.<Project>getRequest(Endpoint.PROJECT).create(project2);
            project2Id.set(createdProject2.getId()); // Set project2's ID
        });
        step("Grant user2 PROJECT_ADMIN role in project2", () -> {
            String roleId = "NEW_ROLE_2";
            String permissionPath = "permissions/comment_build";

            Role role = Role.builder()
                    .roleId(roleId)
                    .rolesecond("PROJECT_ADMIN")
                    .scope("g")
                    .build();

            var userCheckRequests2 = new CheckedRequests(Specifications.authSpec(user2));
            String requestUrl = String.format("/app/rest/roles/id:%s/%s", roleId, permissionPath);
            userCheckRequests2.getRequest(ROLE_ASSIGNMENTS).put(String.valueOf(role)); // Send PUT request to grant the role
        });

        var buildType = generate(Arrays.asList(project1), BuildType.class);
        AtomicReference<String> buildTypeId = new AtomicReference<>("");
        step("Create buildType for project1 by user2", () -> {
            var userCheckRequests2 = new CheckedRequests(Specifications.authSpec(user2));
            try {
                BuildType createdBuildType = (BuildType) userCheckRequests2.getRequest(Endpoint.BUILD_TYPES).create(buildType);
                buildTypeId.set(createdBuildType.id);
            } catch (Exception e) {
                softy.assertTrue(e.getMessage().contains("Forbidden"), "Build type creation should be forbidden");
            }
        });

        step("Check buildType was not created with forbidden code", () -> {
            var userCheckRequests2 = new CheckedRequests(Specifications.authSpec(user2));
            try {
                var createdBuildType = userCheckRequests2.<BuildType>getRequest(Endpoint.BUILD_TYPES).read(buildTypeId.get());
                softy.assertNull(createdBuildType, "Build type should not have been created by user2");
            } catch (Exception e) {
                softy.assertTrue(e.getMessage().contains("Not Found") || e.getMessage().contains("Forbidden"), "Build type should not exist");
            }
        });
    }
}